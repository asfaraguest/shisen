import Foundation

public final class GtxTranslator {
    public static let shared = GtxTranslator()
    private let baseUrl = "https://translate.googleapis.com/translate_a/single"
    
    private init() {}
    
    public func translateBatch(texts: [String], targetLang: String = "id") async -> [String] {
        guard !texts.isEmpty else { return [] }
        
        var results: [String] = []
        // Chunk into batches of 15 lines to avoid URI length issues
        let chunkSize = 15
        for i in stride(from: 0, to: texts.count, by: chunkSize) {
            let chunk = Array(texts[i..<min(i + chunkSize, texts.count)])
            let joined = chunk.joined(separator: "\n---SHISEN_SPLIT---\n")
            
            if let translatedJoined = await translateSingle(text: joined, targetLang: targetLang) {
                let parts = translatedJoined.components(separatedBy: "\n---SHISEN_SPLIT---\n")
                if parts.count == chunk.count {
                    results.append(contentsOf: parts.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) })
                } else {
                    // Fallback to one-by-one in this chunk
                    for t in chunk {
                        let single = await translateSingle(text: t, targetLang: targetLang) ?? t
                        results.append(single)
                    }
                }
            } else {
                results.append(contentsOf: chunk)
            }
        }
        return results
    }
    
    public func translateSingle(text: String, targetLang: String = "id") async -> String? {
        guard let encoded = text.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "\(baseUrl)?client=gtx&sl=auto&tl=\(targetLang)&dt=t&q=\(encoded)") else {
            return nil
        }
        
        do {
            let (data, response) = try await NetworkClient.shared.get(url: url)
            guard response.statusCode == 200 else { return nil }
            
            if let json = try JSONSerialization.jsonObject(with: data) as? [Any],
               let sentences = json.first as? [[Any]] {
                var fullTranslated = ""
                for s in sentences {
                    if let transPart = s.first as? String {
                        fullTranslated += transPart
                    }
                }
                return fullTranslated
            }
        } catch {
            // Translation failed
        }
        return nil
    }
}
