import Foundation

public final class StreamResolver {
    public static let shared = StreamResolver()
    
    private init() {}
    
    public func resolve(anime: Anime, episode: Episode) async throws -> StreamSource {
        // Tier 1: HiAnime
        if episode.source == "hianime" {
            do {
                if let src = try await HiAnimeClient.shared.resolveEpisodeStream(episodeId: episode.sourceId) {
                    return src
                }
            } catch {
                // HiAnime failed, try Tier 2
            }
        }
        
        // Tier 2: AniChan
        do {
            if let src = try await AniChanClient.shared.resolveEpisodeStream(animeTitle: anime.displayTitle, episodeNumber: episode.number) {
                return src
            }
        } catch {
            // AniChan failed, try Tier 3
        }
        
        // Tier 3: Otakudesu
        do {
            if let src = try await OtakudesuClient.shared.resolveEpisodeStream(animeTitle: anime.displayTitle, episodeNumber: episode.number) {
                return src
            }
        } catch {
            // All tiers failed
        }
        
        throw NSError(domain: "StreamResolver", code: 404, userInfo: [NSLocalizedDescriptionKey: "Tidak dapat menemukan sumber video yang valid untuk episode ini."])
    }
}
