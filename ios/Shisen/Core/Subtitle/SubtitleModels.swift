import Foundation

public struct Cue: Identifiable, Hashable {
    public let id = UUID()
    public var startMs: Int64
    public var endMs: Int64
    public var text: String
    public var originalText: String?
    
    public init(startMs: Int64, endMs: Int64, text: String, originalText: String? = nil) {
        self.startMs = startMs
        self.endMs = endMs
        self.text = text
        self.originalText = originalText
    }
}

public struct SubtitleTrack: Identifiable, Hashable, Codable {
    public var id = UUID()
    public let url: String
    public let label: String
    public let lang: String
    public let isDefault: Bool
    
    enum CodingKeys: String, CodingKey {
        case url, label, lang, isDefault
    }
    
    public init(url: String, label: String, lang: String = "en", isDefault: Bool = false) {
        self.url = url
        self.label = label
        self.lang = lang
        self.isDefault = isDefault
    }
}

public enum SubtitleMode: String, CaseIterable, Identifiable {
    case smartMerge = "Smart Merge ID"
    case rawIndonesian = "Indonesian (Raw)"
    case rawEnglish = "English (Raw)"
    case aiTranslated = "AI Subtitle"
    case off = "Off"
    
    public var id: String { rawValue }
}
