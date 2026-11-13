import Foundation

public final class SmartLexicon {
    public static let shared = SmartLexicon()
    
    private struct Rule {
        let regex: NSRegularExpression
        let replacement: String
    }
    
    private var rules: [Rule] = []
    
    private init() {
        setupRules()
    }
    
    private func setupRules() {
        let rawRules: [(String, String)] = [
            // Anime & Fansub honorifics & common terms
            ("\\b(senpai|sempai)\\b", "kakak kelas"),
            ("\\b(kouhai|kohai)\\b", "adik kelas"),
            ("\\b(sensei)\\b", "guru"),
            ("\\b(onee-san|oneesan|onee-chan)\\b", "kakak"),
            ("\\b(onii-san|oniisan|onii-chan)\\b", "abang"),
            ("\\b(otousan|otou-san)\\b", "ayah"),
            ("\\b(okaasan|okaa-san)\\b", "ibu"),
            
            // Common Indonesian casual fansub polishing
            ("\\bAnda\\b", "kamu"),
            ("\\btidak akan pernah\\b", "takkan"),
            ("\\bapakah kamu\\b", "kamu"),
            ("\\bapakah anda\\b", "kamu"),
            ("\\bbagaimana mungkin\\b", "mana mungkin"),
            ("\\bterima kasih banyak\\b", "makasih banyak"),
            ("\\bselamat tinggal\\b", "sampai jumpa"),
            ("\\bdengan cepat\\b", "cepat-cepat"),
            ("\\bhanya saja\\b", "cuma"),
            ("\\btentu saja\\b", "tentu"),
            
            // Overgeared / Fantasy specific terms
            ("\\bSatisfy\\b", "Satisfy"),
            ("\\bGrid\\b", "Grid"),
            ("\\bGreed\\b", "Greed")
        ]
        
        for (pat, rep) in rawRules {
            if let regex = try? NSRegularExpression(pattern: pat, options: [.caseInsensitive]) {
                rules.append(Rule(regex: regex, replacement: rep))
            }
        }
    }
    
    public func applyUniversalLexicon(_ text: String) -> String {
        var result = text
        for rule in rules {
            let range = NSRange(location: 0, length: (result as NSString).length)
            result = rule.regex.stringByReplacingMatches(in: result, options: [], range: range, withTemplate: rule.replacement)
        }
        return result
    }
    
    public func sanitizeCue(_ cueText: String) -> String {
        var clean = cueText
        // Strip ASS/VTT formatting tags
        clean = clean.replacingOccurrences(of: "\\{[^}]*\\}", with: "", options: .regularExpression)
        clean = clean.replacingOccurrences(of: "<[^>]+>", with: "", options: .regularExpression)
        return applyUniversalLexicon(clean).trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
