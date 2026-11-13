import Foundation
import CommonCrypto

public final class HiAnimeClient {
    public static let shared = HiAnimeClient()
    private let baseUrl = "https://hianime.at"

    // Static Megaplay AES-256-CBC key (16 bytes + 16 zero bytes) & IV
    private static let megaKey: Data = {
        var key = Data(count: 32)
        let part = Array("i?LMTAx0Q6,:}50U".utf8)
        key.replaceSubrange(0..<min(part.count, 16), with: part)
        return key
    }()
    private static let megaIV = Data("W0;27ToaUpl_P%'c".utf8)

    private init() {}

    /// Cari slug HiAnime yang paling cocok berdasarkan judul AniList
    public func searchAndMatchSlug(title: String, romajiTitle: String? = nil) async -> String? {
        let queries = [title, romajiTitle].compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }
        for q in queries {
            let cleanQ = q.replacingOccurrences(of: "[^\\p{L}\\p{N}\\s-]", with: " ", options: .regularExpression)
                .trimmingCharacters(in: .whitespacesAndNewlines)
            guard let encoded = (cleanQ.isEmpty ? q : cleanQ).addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
                  let url = URL(string: "\(baseUrl)/search?keyword=\(encoded)") else { continue }

            do {
                let (data, response) = try await NetworkClient.shared.get(url: url, headers: ["Referer": "\(baseUrl)/"])
                guard response.statusCode == 200, let html = String(data: data, encoding: .utf8) else { continue }

                let candidates = parseSearchSlugs(html: html)
                if let best = pickBestSlug(candidates: candidates, targets: queries) {
                    return best
                }
            } catch {
                continue
            }
        }
        return nil
    }

    public func fetchEpisodes(animeSlug: String) async throws -> [Episode] {
        let numId = extractNumericId(from: animeSlug)
        guard !numId.isEmpty,
              let url = URL(string: "\(baseUrl)/ajax/v2/episode/list/\(numId)") else {
            return []
        }

        let headers = [
            "X-Requested-With": "XMLHttpRequest",
            "Referer": "\(baseUrl)/watch/\(animeSlug)"
        ]

        let (data, response) = try await NetworkClient.shared.get(url: url, headers: headers)
        guard response.statusCode == 200 else { return [] }

        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let html = json["html"] as? String else {
            return []
        }

        return parseEpisodesFromHtml(html: html)
    }

    public func resolveEpisodeStream(episodeId: String) async throws -> StreamSource? {
        guard let url = URL(string: "\(baseUrl)/ajax/v2/episode/servers?episodeId=\(episodeId)") else {
            return nil
        }

        let headers = [
            "X-Requested-With": "XMLHttpRequest",
            "Referer": "\(baseUrl)/"
        ]

        let (data, response) = try await NetworkClient.shared.get(url: url, headers: headers)
        guard response.statusCode == 200,
              let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let html = json["html"] as? String else {
            return nil
        }

        let serverIds = extractAllMatches(in: html, pattern: "data-id=[\"'](\\d+)[\"']")
        for serverId in serverIds {
            guard let sourceUrl = URL(string: "\(baseUrl)/ajax/v2/episode/sources?id=\(serverId)") else { continue }
            do {
                let (sourceData, sResp) = try await NetworkClient.shared.get(url: sourceUrl, headers: headers)
                guard sResp.statusCode == 200,
                      let sourceJson = try JSONSerialization.jsonObject(with: sourceData) as? [String: Any],
                      let link = sourceJson["link"] as? String,
                      let embedUrl = URL(string: link) else { continue }

                if let resolved = try await resolveEmbedStream(embedUrl: embedUrl, referer: "\(baseUrl)/") {
                    return resolved
                }
            } catch {
                continue
            }
        }
        return nil
    }

    private func resolveEmbedStream(embedUrl: URL, referer: String) async throws -> StreamSource? {
        let scheme = embedUrl.scheme ?? "https"
        let host = embedUrl.host ?? "megaplay.buzz"
        let origin = "\(scheme)://\(host)"

        // Ambil halaman embed untuk mencari data-id jika bukan /embed-2/
        let (pageData, pageResp) = try await NetworkClient.shared.get(url: embedUrl, headers: ["Referer": referer])
        let pageHtml = pageResp.statusCode == 200 ? (String(data: pageData, encoding: .utf8) ?? "") : ""

        let dataId = extractFirstMatch(in: pageHtml, pattern: "(?:data-id|data-video-id|data-source-id)=[\"']([a-zA-Z0-9_-]+)[\"']")
            ?? extractFirstMatch(in: pageHtml, pattern: "(?:videoId|sourceId|episodeId|cid|cidu)\\s*[:=]\\s*[\"']?([a-zA-Z0-9_-]+)[\"']?")
            ?? URLComponents(url: embedUrl, resolvingAgainstBaseURL: false)?.queryItems?.first(where: { $0.name == "id" })?.value
            ?? embedUrl.pathComponents.last(where: { !$0.isEmpty && $0 != "/" })

        guard let id = dataId, !id.isEmpty else { return nil }

        let candidateEndpoints = [
            "\(origin)/stream/getSourcesNew?id=\(id)",
            "\(origin)/stream/getSources?id=\(id)",
            "\(origin)/embed-2/ajax/e-1/getSources?id=\(id)"
        ]

        let apiHeaders = [
            "X-Requested-With": "XMLHttpRequest",
            "Referer": embedUrl.absoluteString
        ]

        for endpoint in candidateEndpoints {
            guard let apiUrl = URL(string: endpoint) else { continue }
            do {
                let (data, resp) = try await NetworkClient.shared.get(url: apiUrl, headers: apiHeaders)
                guard resp.statusCode == 200,
                      let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else { continue }

                var masterUrl: String? = nil
                if let sources = json["sources"] as? [[String: Any]], let first = sources.first {
                    masterUrl = first["file"] as? String
                }
                if (masterUrl?.isEmpty ?? true), let enc = json["enc"] as? String,
                   let decStr = decryptMegaPayload(enc),
                   let decData = decStr.data(using: .utf8),
                   let decJson = try? JSONSerialization.jsonObject(with: decData) as? [String: Any] {
                    masterUrl = decJson["file"] as? String
                }

                guard var finalUrl = masterUrl, !finalUrl.isEmpty else { continue }
                if finalUrl.hasPrefix("/") {
                    finalUrl = "\(origin)\(finalUrl)"
                }

                var subTracks: [SubtitleTrack] = []
                if let tracks = json["tracks"] as? [[String: Any]] {
                    for track in tracks {
                        let kind = (track["kind"] as? String ?? "").lowercased()
                        if (kind == "captions" || kind == "subtitles"),
                           let file = track["file"] as? String, !file.isEmpty {
                            let label = track["label"] as? String ?? "English"
                            let rawLang = track["lang"] as? String ?? ""
                            let lang = inferLanguage(label: label, rawLang: rawLang)
                            let isDefault = track["default"] as? Bool ?? false
                            subTracks.append(SubtitleTrack(url: file, label: label, lang: lang, isDefault: isDefault))
                        }
                    }
                }

                return StreamSource(
                    url: finalUrl,
                    isHls: true,
                    quality: "HD Auto",
                    serverName: "HiAnime (Megaplay)",
                    headers: [
                        "Referer": "\(origin)/",
                        "Origin": origin
                    ],
                    subtitles: subTracks
                )
            } catch {
                continue
            }
        }
        return nil
    }

    private func decryptMegaPayload(_ base64UrlData: String) -> String? {
        var b64 = base64UrlData
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        while b64.count % 4 != 0 {
            b64.append("=")
        }
        guard let cipherData = Data(base64Encoded: b64) else { return nil }

        let bufferSize = cipherData.count + kCCBlockSizeAES128
        var outBytes = Data(count: bufferSize)
        var numBytesDecrypted: size_t = 0

        let status = outBytes.withUnsafeMutableBytes { outPtr in
            cipherData.withUnsafeBytes { inPtr in
                Self.megaKey.withUnsafeBytes { keyPtr in
                    Self.megaIV.withUnsafeBytes { ivPtr in
                        CCCrypt(
                            CCOperation(kCCDecrypt),
                            CCAlgorithm(kCCAlgorithmAES),
                            CCOptions(kCCOptionPKCS7Padding),
                            keyPtr.baseAddress, kCCKeySizeAES256,
                            ivPtr.baseAddress,
                            inPtr.baseAddress, cipherData.count,
                            outPtr.baseAddress, bufferSize,
                            &numBytesDecrypted
                        )
                    }
                }
            }
        }

        guard status == kCCSuccess else { return nil }
        return String(data: outBytes.prefix(numBytesDecrypted), encoding: .utf8)
    }

    private func parseSearchSlugs(html: String) -> [(title: String, slug: String)] {
        let pattern = "<a[^>]+href=[\"'][^\"']*?/watch/([^\"'?#]+)[\"'][^>]*title=[\"']([^\"']+)[\"']"
        guard let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive]) else { return [] }
        let ns = html as NSString
        var out: [(String, String)] = []
        for m in regex.matches(in: html, range: NSRange(location: 0, length: ns.length)) where m.numberOfRanges >= 3 {
            let slug = ns.substring(with: m.range(at: 1)).trimmingCharacters(in: CharacterSet(charactersIn: "/"))
            let title = ns.substring(with: m.range(at: 2))
            if !slug.lowercased().contains("hentai") && !title.lowercased().contains("hentai") {
                out.append((title, slug))
            }
        }
        return out
    }

    private func pickBestSlug(candidates: [(title: String, slug: String)], targets: [String]) -> String? {
        let normalizedTargets = targets.map { normalizeTitle($0) }.filter { !$0.isEmpty }
        for target in normalizedTargets {
            if let exact = candidates.first(where: { normalizeTitle($0.title) == target }) {
                return exact.slug
            }
        }
        for target in normalizedTargets {
            if let contains = candidates.first(where: {
                let norm = normalizeTitle($0.title)
                return norm.contains(target) || target.contains(norm)
            }) {
                return contains.slug
            }
        }
        return candidates.first?.slug
    }

    private func normalizeTitle(_ s: String) -> String {
        return s.lowercased()
            .replacingOccurrences(of: "[^a-z0-9]", with: "", options: .regularExpression)
    }

    private func extractNumericId(from slug: String) -> String {
        if let match = extractFirstMatch(in: slug, pattern: "-(\\d+)(?:[/?#]|$)") {
            return match
        }
        if slug.range(of: "^\\d+$", options: .regularExpression) != nil {
            return slug
        }
        return ""
    }

    private func parseEpisodesFromHtml(html: String) -> [Episode] {
        // Cocokkan tag <a> yang memiliki data-number dan data-id dalam urutan apapun
        let tagPattern = "<a\\b[^>]*>"
        guard let tagRegex = try? NSRegularExpression(pattern: tagPattern, options: [.caseInsensitive]) else { return [] }
        let nsString = html as NSString
        var episodes: [Episode] = []

        for match in tagRegex.matches(in: html, range: NSRange(location: 0, length: nsString.length)) {
            let tag = nsString.substring(with: match.range)
            guard let numStr = extractFirstMatch(in: tag, pattern: "data-number=[\"'](\\d+)[\"']"),
                  let idStr = extractFirstMatch(in: tag, pattern: "data-id=[\"'](\\d+)[\"']"),
                  let num = Int(numStr) else { continue }
            let title = extractFirstMatch(in: tag, pattern: "title=[\"']([^\"']+)[\"']")
            episodes.append(Episode(number: num, title: title ?? "Episode \(num)", source: "hianime", sourceId: idStr))
        }
        return episodes.sorted { $0.number < $1.number }
    }

    private func inferLanguage(label: String, rawLang: String) -> String {
        let combined = "\(label) \(rawLang)".lowercased()
        if combined.contains("indo") || combined == "id" { return "id" }
        if combined.contains("eng") || combined == "en" { return "en" }
        if combined.contains("jap") || combined == "ja" { return "ja" }
        return rawLang.isEmpty ? "en" : rawLang
    }

    private func extractFirstMatch(in text: String, pattern: String) -> String? {
        guard let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive]) else { return nil }
        let nsString = text as NSString
        if let match = regex.firstMatch(in: text, range: NSRange(location: 0, length: nsString.length)),
           match.numberOfRanges > 1 {
            return nsString.substring(with: match.range(at: 1))
        }
        return nil
    }

    private func extractAllMatches(in text: String, pattern: String) -> [String] {
        guard let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive]) else { return [] }
        let nsString = text as NSString
        return regex.matches(in: text, range: NSRange(location: 0, length: nsString.length)).compactMap {
            $0.numberOfRanges > 1 ? nsString.substring(with: $0.range(at: 1)) : nil
        }
    }
}
