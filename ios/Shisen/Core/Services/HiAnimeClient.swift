import Foundation

public final class HiAnimeClient {
    public static let shared = HiAnimeClient()
    private let baseUrl = "https://hianime.to"
    
    private init() {}
    
    public func fetchEpisodes(animeSlug: String) async throws -> [Episode] {
        guard let url = URL(string: "\(baseUrl)/ajax/v2/episode/list/\(animeSlug)") else {
            return []
        }
        
        var headers = [
            "X-Requested-With": "XMLHttpRequest",
            "Referer": "\(baseUrl)/watch/\(animeSlug)"
        ]
        
        let (data, response) = try await NetworkClient.shared.get(url: url, headers: headers)
        guard response.statusCode == 200 else { return [] }
        
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let html = json["html"] as? String else {
            return []
        }
        
        // Parse episodes from AJAX response html
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
        
        let (data, _) = try await NetworkClient.shared.get(url: url, headers: headers)
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let html = json["html"] as? String else {
            return nil
        }
        
        // Extract server ID (e.g. data-id="...")
        guard let serverId = extractFirstMatch(in: html, pattern: "data-id=[\"'](\\d+)[\"']") else {
            return nil
        }
        
        // Fetch server source
        guard let sourceUrl = URL(string: "\(baseUrl)/ajax/v2/episode/sources?id=\(serverId)") else {
            return nil
        }
        
        let (sourceData, _) = try await NetworkClient.shared.get(url: sourceUrl, headers: headers)
        guard let sourceJson = try JSONSerialization.jsonObject(with: sourceData) as? [String: Any],
              let link = sourceJson["link"] as? String,
              let embedUrl = URL(string: link) else {
            return nil
        }
        
        // Extract MegaCloud / RabbitStream embed
        return try await extractMegaCloudStream(embedUrl: embedUrl)
    }
    
    private func extractMegaCloudStream(embedUrl: URL) async throws -> StreamSource? {
        let host = embedUrl.host ?? "megacloud.tv"
        let pathComponents = embedUrl.pathComponents
        guard let embedId = pathComponents.last(where: { !$0.isEmpty && $0 != "/" }) else {
            return nil
        }
        
        // MegaCloud sources endpoint: /embed-2/ajax/e-1/getSources?id=...
        guard let getSourcesUrl = URL(string: "https://\(host)/embed-2/ajax/e-1/getSources?id=\(embedId)") else {
            return nil
        }
        
        let headers = [
            "X-Requested-With": "XMLHttpRequest",
            "Referer": embedUrl.absoluteString
        ]
        
        let (data, _) = try await NetworkClient.shared.get(url: getSourcesUrl, headers: headers)
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }
        
        var streamUrl: String? = nil
        if let sources = json["sources"] as? [[String: Any]], let first = sources.first {
            streamUrl = first["file"] as? String
        } else if let encryptedSource = json["sources"] as? String {
            streamUrl = encryptedSource
        }
        
        guard let finalUrl = streamUrl else { return nil }
        
        // Subtitle tracks
        var subTracks: [SubtitleTrack] = []
        if let tracks = json["tracks"] as? [[String: Any]] {
            for track in tracks {
                if let kind = track["kind"] as? String, kind == "captions" || kind == "subtitles",
                   let file = track["file"] as? String {
                    let label = track["label"] as? String ?? "Subtitle"
                    let isDefault = track["default"] as? Bool ?? false
                    subTracks.append(SubtitleTrack(url: file, label: label, isDefault: isDefault))
                }
            }
        }
        
        return StreamSource(
            url: finalUrl,
            isHls: true,
            quality: "1080p Auto",
            serverName: "MegaCloud (HiAnime)",
            headers: [
                "Referer": "https://\(host)/",
                "Origin": "https://\(host)"
            ],
            subtitles: subTracks
        )
    }
    
    private func parseEpisodesFromHtml(html: String) -> [Episode] {
        var episodes: [Episode] = []
        let pattern = "data-number=[\"'](\\d+)[\"'][^>]*data-id=[\"'](\\d+)[\"']"
        guard let regex = try? NSRegularExpression(pattern: pattern, options: []) else { return [] }
        
        let nsString = html as NSString
        let results = regex.matches(in: html, range: NSRange(location: 0, length: nsString.length))
        
        for match in results {
            if match.numberOfRanges >= 3 {
                let numStr = nsString.substring(with: match.range(at: 1))
                let idStr = nsString.substring(with: match.range(at: 2))
                if let num = Int(numStr) {
                    episodes.append(Episode(number: num, title: "Episode \(num)", source: "hianime", sourceId: idStr))
                }
            }
        }
        return episodes
    }
    
    private func extractFirstMatch(in text: String, pattern: String) -> String? {
        guard let regex = try? NSRegularExpression(pattern: pattern, options: []) else { return nil }
        let nsString = text as NSString
        if let match = regex.firstMatch(in: text, range: NSRange(location: 0, length: nsString.length)),
           match.numberOfRanges > 1 {
            return nsString.substring(with: match.range(at: 1))
        }
        return nil
    }
}
