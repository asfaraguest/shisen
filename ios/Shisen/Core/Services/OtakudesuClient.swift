import Foundation

public final class OtakudesuClient {
    public static let shared = OtakudesuClient()
    private let baseUrl = "https://otakudesu.cloud"
    
    private init() {}
    
    public func resolveEpisodeStream(animeTitle: String, episodeNumber: Int) async throws -> StreamSource? {
        // Fallback tier 3: Otakudesu Indonesian hardsub scraper
        guard let encoded = animeTitle.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let searchUrl = URL(string: "\(baseUrl)/?s=\(encoded)&post_type=anime") else {
            return nil
        }
        
        do {
            let (data, response) = try await NetworkClient.shared.get(url: searchUrl)
            guard response.statusCode == 200, let html = String(data: data, encoding: .utf8) else { return nil }
            
            // Extract anime link
            if let animeLink = extractFirstMatch(in: html, pattern: "href=[\"'](https://otakudesu\\.[^\"']+/anime/[^\"']+)[\"']") {
                guard let detailUrl = URL(string: animeLink) else { return nil }
                let (detailData, _) = try await NetworkClient.shared.get(url: detailUrl)
                if let detailHtml = String(data: detailData, encoding: .utf8) {
                    // Match episode link
                    let epPattern = "href=[\"'](https://otakudesu\\.[^\"']+/episode/[^\"']+)[\"'][^>]*>Episode \(episodeNumber)<"
                    if let epLink = extractFirstMatch(in: detailHtml, pattern: epPattern) ?? extractFirstMatch(in: detailHtml, pattern: "href=[\"'](https://otakudesu\\.[^\"']+/episode/[^\"']+)[\"']") {
                        guard let epUrl = URL(string: epLink) else { return nil }
                        let (epData, _) = try await NetworkClient.shared.get(url: epUrl)
                        if let epHtml = String(data: epData, encoding: .utf8),
                           let streamSrc = extractFirstMatch(in: epHtml, pattern: "<iframe[^>]+src=[\"']([^\"']+)[\"']") {
                            return StreamSource(
                                url: streamSrc,
                                isHls: false,
                                quality: "720p (Hardsub ID)",
                                serverName: "Otakudesu (Tier 3)"
                            )
                        }
                    }
                }
            }
        } catch {
            // Ignore
        }
        return nil
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
}
