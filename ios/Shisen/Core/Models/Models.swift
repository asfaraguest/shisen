import Foundation

// MARK: - Core Domain Models

public struct Anime: Identifiable, Codable, Hashable {
    public let id: Int
    public let idMal: Int?
    public let titleRomaji: String
    public let titleEnglish: String?
    public let titleNative: String?
    public let coverImage: String?
    public let bannerImage: String?
    public let description: String?
    public let format: String?
    public let status: String?
    public let episodes: Int?
    public let duration: Int?
    public let averageScore: Int?
    public let genres: [String]
    public let season: String?
    public let seasonYear: Int?
    public let nextAiringEpisode: Int?
    
    public var displayTitle: String {
        return titleEnglish?.isEmpty == false ? titleEnglish! : titleRomaji
    }
    
    public init(
        id: Int,
        idMal: Int? = nil,
        titleRomaji: String,
        titleEnglish: String? = nil,
        titleNative: String? = nil,
        coverImage: String? = nil,
        bannerImage: String? = nil,
        description: String? = nil,
        format: String? = nil,
        status: String? = nil,
        episodes: Int? = nil,
        duration: Int? = nil,
        averageScore: Int? = nil,
        genres: [String] = [],
        season: String? = nil,
        seasonYear: Int? = nil,
        nextAiringEpisode: Int? = nil
    ) {
        self.id = id
        self.idMal = idMal
        self.titleRomaji = titleRomaji
        self.titleEnglish = titleEnglish
        self.titleNative = titleNative
        self.coverImage = coverImage
        self.bannerImage = bannerImage
        self.description = description
        self.format = format
        self.status = status
        self.episodes = episodes
        self.duration = duration
        self.averageScore = averageScore
        self.genres = genres
        self.season = season
        self.seasonYear = seasonYear
        self.nextAiringEpisode = nextAiringEpisode
    }
}

public struct Episode: Identifiable, Codable, Hashable {
    public var id: String { "\(source)_\(number)" }
    public let number: Int
    public let title: String?
    public let source: String
    public let sourceId: String
    
    public init(number: Int, title: String? = nil, source: String, sourceId: String) {
        self.number = number
        self.title = title
        self.source = source
        self.sourceId = sourceId
    }
}

public struct StreamSource: Identifiable, Hashable {
    public let id = UUID()
    public let url: String
    public let isHls: Bool
    public let quality: String
    public let serverName: String
    public let headers: [String: String]
    public let subtitles: [SubtitleTrack]
    
    public init(
        url: String,
        isHls: Bool = true,
        quality: String = "auto",
        serverName: String = "Default",
        headers: [String: String] = [:],
        subtitles: [SubtitleTrack] = []
    ) {
        self.url = url
        self.isHls = isHls
        self.quality = quality
        self.serverName = serverName
        self.headers = headers
        self.subtitles = subtitles
    }
}

public struct AniSkipInterval: Codable, Hashable {
    public let start: Double
    public let end: Double
    public let type: String // "op", "ed", "mixed-op", "recap"
    
    public init(start: Double, end: Double, type: String) {
        self.start = start
        self.end = end
        self.type = type
    }
}
