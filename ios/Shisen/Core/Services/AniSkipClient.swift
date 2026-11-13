import Foundation

public final class AniSkipClient {
    public static let shared = AniSkipClient()
    private let baseUrl = "https://api.aniskip.com/v2/skip-times"
    
    private init() {}
    
    public func fetchSkipTimes(malId: Int, episodeNumber: Int, episodeLength: Double = 1440) async -> [AniSkipInterval] {
        guard let url = URL(string: "\(baseUrl)/\(malId)/\(episodeNumber)?types=op&types=ed&types=mixed-op&types=recap&episodeLength=\(Int(episodeLength))") else {
            return []
        }
        
        do {
            let (data, response) = try await NetworkClient.shared.get(url: url)
            guard response.statusCode == 200 else { return [] }
            
            if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
               let found = json["found"] as? Bool, found,
               let results = json["results"] as? [[String: Any]] {
                return results.compactMap { item in
                    guard let type = item["skipType"] as? String,
                          let interval = item["interval"] as? [String: Any],
                          let start = interval["startTime"] as? Double,
                          let end = interval["endTime"] as? Double else { return nil }
                    return AniSkipInterval(start: start, end: end, type: type)
                }
            }
        } catch {
            // Non-critical, fallback to manual skipping
        }
        return []
    }
}
