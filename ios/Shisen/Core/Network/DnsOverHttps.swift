import Foundation

public final class DnsOverHttps {
    public static let shared = DnsOverHttps()
    
    // Cloudflare & Google DoH Endpoints
    private let dohEndpoints = [
        "https://1.1.1.1/dns-query",
        "https://dns.google/resolve"
    ]
    
    private init() {}
    
    public func resolveIP(host: String) async -> String? {
        guard let url = URL(string: "https://1.1.1.1/dns-query?name=\(host)&type=A") else {
            return nil
        }
        var request = URLRequest(url: url)
        request.setValue("application/dns-json", forHTTPHeaderField: "Accept")
        
        do {
            let (data, _) = try await URLSession.shared.data(for: request)
            if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
               let answers = json["Answer"] as? [[String: Any]] {
                for answer in answers {
                    if let type = answer["type"] as? Int, type == 1,
                       let ip = answer["data"] as? String {
                        return ip
                    }
                }
            }
        } catch {
            // Fallback to direct resolution
        }
        return nil
    }
}
