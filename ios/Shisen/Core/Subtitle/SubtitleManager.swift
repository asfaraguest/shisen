import Foundation

public final class SubtitleManager: ObservableObject {
    public static let shared = SubtitleManager()
    
    @Published public private(set) var activeCues: [Cue] = []
    @Published public private(set) var currentMode: SubtitleMode = .smartMerge
    @Published public private(set) var isLoading: Bool = false
    @Published public private(set) var currentCueText: String? = nil
    
    private init() {}
    
    public func setMode(_ mode: SubtitleMode) {
        self.currentMode = mode
        if mode == .off {
            activeCues = []
            currentCueText = nil
        }
    }
    
    public func updatePlaybackPosition(ms: Int64) {
        guard currentMode != .off else {
            if currentCueText != nil { currentCueText = nil }
            return
        }
        
        let match = activeCues.first { cue in
            ms >= cue.startMs && ms <= cue.endMs
        }
        
        if currentCueText != match?.text {
            currentCueText = match?.text
        }
    }
    
    /// INVARIAN: loadFromTracks SELALU jalur Smart Merge ID
    public func loadFromTracks(tracks: [SubtitleTrack], contextTitle: String = "Anime") async {
        await MainActor.run {
            self.isLoading = true
            self.currentMode = .smartMerge
            self.activeCues = []
        }
        
        // 1. Cari track English bawaan dan track ID
        let enTrack = tracks.first(where: { $0.label.lowercased().contains("english") || $0.lang == "en" }) ?? tracks.first
        let idTrack = tracks.first(where: { $0.label.lowercased().contains("indonesia") || $0.lang == "id" })
        
        guard let enUrlStr = enTrack?.url, let enUrl = URL(string: enUrlStr) else {
            await MainActor.run { self.isLoading = false }
            return
        }
        
        let enCues = await fetchAndParseCues(from: enUrl)
        
        if let idUrlStr = idTrack?.url, let idUrl = URL(string: idUrlStr) {
            let idCues = await fetchAndParseCues(from: idUrl)
            let offset = SubtitleMergeEngine.shared.estimateOptimalOffset(builtin: enCues, idCues: idCues)
            let gapIndices = SubtitleMergeEngine.shared.findUnmatchedBuiltinIndices(builtin: enCues, idCues: idCues, offsetMs: offset)
            
            // Translate gaps asynchronously
            var translatedGaps: [Int: String] = [:]
            if !gapIndices.isEmpty {
                let gapTexts = gapIndices.map { enCues[$0].text }
                let translated = await GtxTranslator.shared.translateBatch(texts: gapTexts, targetLang: "id")
                for (i, gapIdx) in gapIndices.enumerated() {
                    if i < translated.count {
                        translatedGaps[gapIdx] = translated[i]
                    }
                }
            }
            
            let merged = SubtitleMergeEngine.shared.merge(
                builtinCues: enCues,
                externalIdCues: idCues,
                optimalOffsetMs: offset,
                translatedGaps: translatedGaps
            )
            
            await MainActor.run {
                self.activeCues = merged
                self.isLoading = false
            }
        } else {
            // Jika belum ada track ID eksternal, terjemahkan English via AI Subtitle / GTX
            let textsToTranslate = enCues.map { $0.text }
            let translated = await GtxTranslator.shared.translateBatch(texts: textsToTranslate, targetLang: "id")
            
            let translatedCues: [Cue] = enCues.enumerated().map { idx, cue in
                let transText = idx < translated.count ? translated[idx] : cue.text
                let sanitized = SmartLexicon.shared.sanitizeCue(transText)
                return Cue(startMs: cue.startMs, endMs: cue.endMs, text: sanitized, originalText: cue.text)
            }
            
            await MainActor.run {
                self.activeCues = translatedCues
                self.isLoading = false
            }
        }
    }
    
    /// Jalur raw tanpa Smart Merge
    public func loadRawTrack(url: URL, mode: SubtitleMode) async {
        await MainActor.run {
            self.isLoading = true
            self.currentMode = mode
            self.activeCues = []
        }
        
        let cues = await fetchAndParseCues(from: url)
        await MainActor.run {
            self.activeCues = cues
            self.isLoading = false
        }
    }
    
    private func fetchAndParseCues(from url: URL) async -> [Cue] {
        do {
            let (data, _) = try await NetworkClient.shared.get(url: url)
            guard let text = String(data: data, encoding: .utf8) else { return [] }
            return parseVttOrSrt(content: text)
        } catch {
            return []
        }
    }
    
    public func parseVttOrSrt(content: String) -> [Cue] {
        var cues: [Cue] = []
        let lines = content.components(separatedBy: .newlines)
        
        var i = 0
        while i < lines.count {
            let line = lines[i].trimmingCharacters(in: .whitespacesAndNewlines)
            if line.contains("-->") {
                let timeParts = line.components(separatedBy: "-->")
                if timeParts.count == 2 {
                    let start = parseTimestampMs(timeParts[0].trimmingCharacters(in: .whitespacesAndNewlines))
                    // Remove cue settings like line:80% position:50%
                    let rawEnd = timeParts[1].trimmingCharacters(in: .whitespacesAndNewlines).components(separatedBy: .whitespaces).first ?? ""
                    let end = parseTimestampMs(rawEnd)
                    
                    var textLines: [String] = []
                    i += 1
                    while i < lines.count && !lines[i].trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        textLines.append(lines[i].trimmingCharacters(in: .whitespacesAndNewlines))
                        i += 1
                    }
                    
                    let text = textLines.joined(separator: "\n")
                    if start >= 0 && end > start && !text.isEmpty {
                        cues.append(Cue(startMs: start, endMs: end, text: text))
                    }
                }
            }
            i += 1
        }
        return cues
    }
    
    private func parseTimestampMs(_ ts: String) -> Int64 {
        // Formats: 00:01:23.456 or 01:23.456 or 00:01:23,456
        let clean = ts.replacingOccurrences(of: ",", with: ".")
        let parts = clean.components(separatedBy: ":")
        
        var hours: Double = 0
        var mins: Double = 0
        var secs: Double = 0
        
        if parts.count == 3 {
            hours = Double(parts[0]) ?? 0
            mins = Double(parts[1]) ?? 0
            secs = Double(parts[2]) ?? 0
        } else if parts.count == 2 {
            mins = Double(parts[0]) ?? 0
            secs = Double(parts[1]) ?? 0
        } else {
            return -1
        }
        
        let totalMs = (hours * 3600.0 + mins * 60.0 + secs) * 1000.0
        return Int64(totalMs)
    }
}
