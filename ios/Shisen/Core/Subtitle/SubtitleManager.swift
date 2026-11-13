import Foundation

public final class SubtitleManager: ObservableObject {
    public static let shared = SubtitleManager()
    
    @Published public private(set) var activeCues: [Cue] = []
    @Published public private(set) var currentMode: SubtitleMode = .smartMerge
    @Published public private(set) var isLoading: Bool = false
    @Published public private(set) var currentCueText: String? = nil

    private var lastTracks: [SubtitleTrack] = []
    private var lastContextTitle: String = "Anime"
    private var lastHeaders: [String: String] = [:]
    private var translationCache: [String: [Cue]] = [:]

    private init() {}

    public func setMode(_ mode: SubtitleMode) {
        self.currentMode = mode
        if mode == .off {
            activeCues = []
            currentCueText = nil
            return
        }
        guard !lastTracks.isEmpty else { return }
        Task {
            await applyMode(mode)
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
    public func loadFromTracks(tracks: [SubtitleTrack], contextTitle: String = "Anime", headers: [String: String] = [:]) async {
        self.lastTracks = tracks
        self.lastContextTitle = contextTitle
        self.lastHeaders = headers
        await applyMode(.smartMerge)
    }

    private func applyMode(_ mode: SubtitleMode) async {
        await MainActor.run {
            self.isLoading = true
            self.currentMode = mode
            self.activeCues = []
            self.currentCueText = nil
        }

        let enTrack = lastTracks.first(where: { $0.label.lowercased().contains("english") || $0.lang == "en" }) ?? lastTracks.first
        let idTrack = lastTracks.first(where: { $0.label.lowercased().contains("indonesia") || $0.lang == "id" })

        switch mode {
        case .off:
            await MainActor.run { self.isLoading = false }
            return

        case .rawEnglish:
            if let urlStr = enTrack?.url, let url = URL(string: urlStr) {
                let cues = await fetchAndParseCues(from: url)
                await MainActor.run { self.activeCues = cues; self.isLoading = false }
            } else {
                await MainActor.run { self.isLoading = false }
            }
            return

        case .rawIndonesian:
            if let urlStr = idTrack?.url, let url = URL(string: urlStr) {
                let cues = await fetchAndParseCues(from: url)
                await MainActor.run { self.activeCues = cues; self.isLoading = false }
            } else {
                await MainActor.run { self.isLoading = false }
            }
            return

        case .smartMerge, .aiTranslated:
            guard let enUrlStr = enTrack?.url, let enUrl = URL(string: enUrlStr) else {
                await MainActor.run { self.isLoading = false }
                return
            }

            let cacheKey = "\(mode.rawValue)|\(enUrlStr)"
            if let cached = translationCache[cacheKey] {
                await MainActor.run { self.activeCues = cached; self.isLoading = false }
                return
            }

            let enCues = await fetchAndParseCues(from: enUrl)
            guard !enCues.isEmpty else {
                await MainActor.run { self.isLoading = false }
                return
            }

            if mode == .smartMerge, let idUrlStr = idTrack?.url, let idUrl = URL(string: idUrlStr) {
                let idCues = await fetchAndParseCues(from: idUrl)
                let offset = SubtitleMergeEngine.shared.estimateOptimalOffset(builtin: enCues, idCues: idCues)
                let gapIndices = SubtitleMergeEngine.shared.findUnmatchedBuiltinIndices(builtin: enCues, idCues: idCues, offsetMs: offset)

                var translatedGaps: [Int: String] = [:]
                if !gapIndices.isEmpty {
                    let gapTexts = gapIndices.map { enCues[$0].text }
                    let translated = await translateLines(gapTexts)
                    for (i, gapIdx) in gapIndices.enumerated() where i < translated.count {
                        translatedGaps[gapIdx] = translated[i]
                    }
                }

                let merged = SubtitleMergeEngine.shared.merge(
                    builtinCues: enCues,
                    externalIdCues: idCues,
                    optimalOffsetMs: offset,
                    translatedGaps: translatedGaps
                )

                await MainActor.run { self.activeCues = merged; self.isLoading = false }
            } else {
                let textsToTranslate = enCues.map { $0.text }
                let translated = await translateLines(textsToTranslate)

                var changedCount = 0
                let translatedCues: [Cue] = enCues.enumerated().map { idx, cue in
                    let transText = idx < translated.count ? translated[idx] : cue.text
                    let sanitized = SmartLexicon.shared.sanitizeCue(transText)
                    if sanitized.lowercased() != cue.text.lowercased() && !sanitized.isEmpty {
                        changedCount += 1
                    }
                    return Cue(startMs: cue.startMs, endMs: cue.endMs, text: sanitized, originalText: cue.text)
                }

                // Simpan cache hanya jika >80% cue berhasil diterjemahkan
                if Double(changedCount) / Double(max(1, enCues.count)) > 0.8 {
                    translationCache[cacheKey] = translatedCues
                }

                await MainActor.run { self.activeCues = translatedCues; self.isLoading = false }
            }
        }
    }

    private func translateLines(_ lines: [String]) async -> [String] {
        let apiKey = UserDefaults.standard.string(forKey: "ai_api_key")?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !apiKey.isEmpty {
            let providerRaw = UserDefaults.standard.string(forKey: "ai_provider") ?? ""
            let provider = AiSubtitleService.Provider(rawValue: providerRaw) ?? AiSubtitleService.Provider.detect(key: apiKey)
            let model = UserDefaults.standard.string(forKey: "ai_selected_model")
            do {
                return try await AiSubtitleService.shared.translateBatch(
                    cues: lines,
                    provider: provider,
                    apiKey: apiKey,
                    model: model,
                    contextTitle: lastContextTitle
                )
            } catch {
                // Fallback ke GTX jika AI gagal
            }
        }
        return await GtxTranslator.shared.translateBatch(texts: lines, targetLang: "id")
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
            let (data, _) = try await NetworkClient.shared.get(url: url, headers: lastHeaders)
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
