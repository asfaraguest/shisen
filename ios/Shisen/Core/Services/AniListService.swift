import Foundation

public final class AniListService {
    public static let shared = AniListService()
    private let endpoint = URL(string: "https://graphql.anilist.co")!
    
    private init() {}
    
    public func fetchTrending(page: Int = 1, perPage: Int = 20) async throws -> [Anime] {
        let query = """
        query ($page: Int, $perPage: Int) {
          Page(page: $page, perPage: $perPage) {
            media(type: ANIME, sort: TRENDING_DESC) {
              id
              title { romaji english native }
              coverImage { extraLarge large medium color }
              bannerImage
              description(asHtml: false)
              format
              status
              episodes
              duration
              averageScore
              genres
              season
              seasonYear
              nextAiringEpisode { episode }
            }
          }
        }
        """
        return try await executeQuery(query: query, variables: ["page": page, "perPage": perPage])
    }
    
    public func fetchPopular(page: Int = 1, perPage: Int = 20) async throws -> [Anime] {
        let query = """
        query ($page: Int, $perPage: Int) {
          Page(page: $page, perPage: $perPage) {
            media(type: ANIME, sort: POPULARITY_DESC) {
              id
              title { romaji english native }
              coverImage { extraLarge large medium color }
              bannerImage
              description(asHtml: false)
              format
              status
              episodes
              duration
              averageScore
              genres
              season
              seasonYear
              nextAiringEpisode { episode }
            }
          }
        }
        """
        return try await executeQuery(query: query, variables: ["page": page, "perPage": perPage])
    }
    
    public func searchAnime(query: String, page: Int = 1, perPage: Int = 20) async throws -> [Anime] {
        let gql = """
        query ($search: String, $page: Int, $perPage: Int) {
          Page(page: $page, perPage: $perPage) {
            media(type: ANIME, search: $search, sort: POPULARITY_DESC) {
              id
              title { romaji english native }
              coverImage { extraLarge large medium color }
              bannerImage
              description(asHtml: false)
              format
              status
              episodes
              duration
              averageScore
              genres
              season
              seasonYear
              nextAiringEpisode { episode }
            }
          }
        }
        """
        return try await executeQuery(query: gql, variables: ["search": query, "page": page, "perPage": perPage])
    }
    
    private func executeQuery(query: String, variables: [String: Any]) async throws -> [Anime] {
        let payload: [String: Any] = [
            "query": query,
            "variables": variables
        ]
        let data = try JSONSerialization.data(withJSONObject: payload)
        var headers = ["Content-Type": "application/json", "Accept": "application/json"]
        
        let (resData, _) = try await NetworkClient.shared.post(url: endpoint, body: data, headers: headers)
        
        guard let json = try JSONSerialization.jsonObject(with: resData) as? [String: Any],
              let dataObj = json["data"] as? [String: Any],
              let pageObj = dataObj["Page"] as? [String: Any],
              let mediaList = pageObj["media"] as? [[String: Any]] else {
            return []
        }
        
        return mediaList.compactMap { dict in
            guard let id = dict["id"] as? Int,
                  let titleDict = dict["title"] as? [String: Any],
                  let romaji = titleDict["romaji"] as? String else { return nil }
            
            let english = titleDict["english"] as? String
            let native = titleDict["native"] as? String
            
            var cover: String? = nil
            if let coverDict = dict["coverImage"] as? [String: Any] {
                cover = coverDict["extraLarge"] as? String ?? coverDict["large"] as? String
            }
            
            let banner = dict["bannerImage"] as? String
            let desc = dict["description"] as? String
            let format = dict["format"] as? String
            let status = dict["status"] as? String
            let episodes = dict["episodes"] as? Int
            let duration = dict["duration"] as? Int
            let score = dict["averageScore"] as? Int
            let genres = dict["genres"] as? [String] ?? []
            let season = dict["season"] as? String
            let seasonYear = dict["seasonYear"] as? Int
            
            var nextEp: Int? = nil
            if let nextAiring = dict["nextAiringEpisode"] as? [String: Any] {
                nextEp = nextAiring["episode"] as? Int
            }
            
            return Anime(
                id: id,
                titleRomaji: romaji,
                titleEnglish: english,
                titleNative: native,
                coverImage: cover,
                bannerImage: banner,
                description: desc,
                format: format,
                status: status,
                episodes: episodes,
                duration: duration,
                averageScore: score,
                genres: genres,
                season: season,
                seasonYear: seasonYear,
                nextAiringEpisode: nextEp
            )
        }
    }
}
