import Foundation

public final class AniChanClient {
    public static let shared = AniChanClient()
    private let baseUrl = "https://anichan.my.id"
    
    private init() {}
    
    public func resolveEpisodeStream(animeTitle: String, episodeNumber: Int) async throws -> StreamSource? {
        // Fallback tier 2: AniChan API
        guard let encoded = animeTitle.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "\(baseUrl)/api/search?q=\(encoded)") else {
            return nil
        }
        
        do {
            let (data, response) = try await NetworkClient.shared.get(url: url)
            guard response.statusCode == 200 else { return nil }
            
            if let list = try JSONSerialization.jsonObject(with: data) as? [[String: Any]],
               let first = list.first,
               let slug = first["slug"] as? String,
               let streamApi = URL(string: "\(baseUrl)/api/stream/\(slug)/\(episodeNumber)") {
                
                let (streamData, sRes) = try await NetworkClient.shared.get(url: streamApi)
                guard sRes.statusCode == 200 else { return nil }
                
                if let streamJson = try JSONSerialization.jsonObject(with: streamData) as? [String: Any],
                   let streamUrl = streamJson["url"] as? String {
                    return StreamSource(
                        url: streamUrl,
                        isHls: true,
                        quality: "720p",
                        serverName: "AniChan (Tier 2)"
                    )
                }
            }
        } catch {
            // Ignore and let fallback handle
        }
        return nil
    }
}
