import Foundation

public final class StreamResolver {
    public static let shared = StreamResolver()

    private init() {}

    /// Ambil daftar episode asli: Tier 1 HiAnime -> Tier 2 AniChan -> Fallback metadata AniList
    public func fetchEpisodes(anime: Anime) async -> [Episode] {
        // Tier 1: HiAnime search + episode list
        if let slug = await HiAnimeClient.shared.searchAndMatchSlug(title: anime.displayTitle, romajiTitle: anime.titleRomaji),
           let eps = try? await HiAnimeClient.shared.fetchEpisodes(animeSlug: slug),
           !eps.isEmpty {
            return eps
        }

        // Tier 2: AniChan via AniList ID
        let aniChanEps = await AniChanClient.shared.fetchEpisodes(anilistId: anime.id)
        if !aniChanEps.isEmpty {
            return aniChanEps
        }

        // Fallback terakhir: jumlah episode tayang AniList (nextAiringEpisode - 1)
        let total = anime.nextAiringEpisode.map { max(1, $0 - 1) } ?? anime.episodes ?? 12
        return (1...max(1, total)).map {
            Episode(number: $0, title: "Episode \($0)", source: "anilist", sourceId: "\($0)")
        }
    }

    public func resolve(anime: Anime, episode: Episode) async throws -> StreamSource {
        // Tier 1: HiAnime (pakai episode.sourceId jika dari HiAnime, atau cari slug jika dari fallback)
        if episode.source == "hianime" {
            if let src = try? await HiAnimeClient.shared.resolveEpisodeStream(episodeId: episode.sourceId) {
                return src
            }
        } else if let slug = await HiAnimeClient.shared.searchAndMatchSlug(title: anime.displayTitle, romajiTitle: anime.titleRomaji),
                  let eps = try? await HiAnimeClient.shared.fetchEpisodes(animeSlug: slug),
                  let matched = eps.first(where: { $0.number == episode.number }),
                  let src = try? await HiAnimeClient.shared.resolveEpisodeStream(episodeId: matched.sourceId) {
            return src
        }

        // Tier 2: AniChan (memakai AniList ID langsung)
        if let src = try? await AniChanClient.shared.resolveEpisodeStream(anilistId: anime.id, episodeNumber: episode.number) {
            return src
        }

        // Tier 3: Otakudesu
        if let src = try? await OtakudesuClient.shared.resolveEpisodeStream(animeTitle: anime.displayTitle, episodeNumber: episode.number) {
            return src
        }

        throw NSError(domain: "StreamResolver", code: 404, userInfo: [NSLocalizedDescriptionKey: "Tidak dapat menemukan sumber video yang valid untuk episode ini."])
    }
}
