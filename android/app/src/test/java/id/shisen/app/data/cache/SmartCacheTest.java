package id.shisen.app.data.cache;

import org.junit.Before;
import org.junit.Test;

import id.shisen.app.data.model.Anime;
import id.shisen.app.data.service.AniListClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SmartCacheTest {

    @Before
    public void setUp() {
        AniListClient.resetCooldownForTest();
    }

    @Test
    public void syncAiringProgress_advancesSingleAndMultipleWeeks() {
        Anime a = new Anime();
        a.status = "RELEASING";
        a.episodesCount = 12;
        a.airedEpisodesCount = 3;
        a.nextAiringEpisode = 4;
        a.airingAt = 1000_000L;

        // 10 menit setelah jadwal tayang Ep 4
        FeedCache.syncAiringProgress(a, 1000_600L);
        assertEquals(4, a.airedEpisodesCount);
        assertEquals(5, a.nextAiringEpisode);
        assertEquals(1000_000L + 604800L, a.airingAt);
        assertEquals("Ep 4/12", a.epsCounter);

        // 2 minggu lewat lagi (Ep 5 & Ep 6 tayang)
        FeedCache.syncAiringProgress(a, a.airingAt + 604800L + 100L);
        assertEquals(6, a.airedEpisodesCount);
        assertEquals(7, a.nextAiringEpisode);
        assertEquals("Ep 6/12", a.epsCounter);
    }

    @Test
    public void syncAiringProgress_transitionsToFinishedOnFinalEpisode() {
        Anime a = new Anime();
        a.status = "RELEASING";
        a.episodesCount = 12;
        a.airedEpisodesCount = 11;
        a.nextAiringEpisode = 12;
        a.airingAt = 2000_000L;

        FeedCache.syncAiringProgress(a, 2000_100L);
        assertEquals(12, a.airedEpisodesCount);
        assertEquals("FINISHED", a.status);
        assertEquals(0, a.nextAiringEpisode);
        assertEquals(0L, a.airingAt);
        assertEquals("12 Ep (Tamat)", a.epsCounter);
    }

    @Test
    public void handleRateLimitHeaders_setsCooldownOn429AndLowRemaining() {
        long now = 5000_000L;
        AniListClient.handleRateLimitHeaders(200, null, "2", now);
        assertEquals(now + 15_000L, AniListClient.getCooldownUntilMs());

        AniListClient.handleRateLimitHeaders(429, "45", "0", now);
        assertEquals(now + 45_000L, AniListClient.getCooldownUntilMs());
        assertTrue(AniListClient.getCooldownUntilMs() > now);
    }

    @Test
    public void animeSerializationAndMerge_preservesMalIdNativeTitleAndAiringState() {
        Anime src = new Anime();
        src.id = 154587;
        src.idMal = 52991;
        src.slug = "frieren";
        src.title = "Frieren: Beyond Journey's End";
        src.romajiTitle = "Sousou no Frieren";
        src.nativeTitle = "葬送のフリーレン";
        src.status = "RELEASING";
        src.episodesCount = 28;
        src.airedEpisodesCount = 5;
        src.nextAiringEpisode = 6;
        src.airingAt = 3000_000L;
        src.synopsis = "Elf mage journey.";

        Anime deserialized = Anime.fromJson(src.toJson());
        assertEquals(52991, deserialized.idMal);
        assertEquals(154587, deserialized.anilistId);
        assertEquals("葬送のフリーレン", deserialized.nativeTitle);

        // Simulasikan item Favorit dari SQLite yang hanya punya kolom dasar
        Anime bookmarkItem = new Anime();
        bookmarkItem.slug = "frieren";
        bookmarkItem.title = "Frieren: Beyond Journey's End";
        bookmarkItem.epsCounter = "Ep 5/28";

        bookmarkItem.mergeMetadataFrom(deserialized);
        FeedCache.syncAiringProgress(bookmarkItem, 3000_500L);
        assertEquals(52991, bookmarkItem.idMal);
        assertEquals("葬送のフリーレン", bookmarkItem.nativeTitle);
        assertEquals("Elf mage journey.", bookmarkItem.synopsis);
        assertEquals(6, bookmarkItem.airedEpisodesCount);
        assertEquals("Ep 6/28", bookmarkItem.epsCounter);
    }
}
