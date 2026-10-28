package id.shisen.app.data.cache;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import id.shisen.app.data.model.Anime;

import static org.junit.Assert.*;

public class FeedCacheTest {

    @Test
    public void testCalculateNextRevalidateEpochWithUpcomingAiring() {
        long nowSec = System.currentTimeMillis() / 1000L;
        List<Anime> list = new ArrayList<>();

        Anime a1 = new Anime();
        a1.title = "Anime Hari Ini 1 Jam Lagi";
        a1.airingAt = nowSec + 3600; // 1 jam mendatang

        Anime a2 = new Anime();
        a2.title = "Anime Hari Ini 3 Jam Lagi";
        a2.airingAt = nowSec + 10800; // 3 jam mendatang

        Anime a3 = new Anime();
        a3.title = "Anime Sudah Tayang Tadi Pagi";
        a3.airingAt = nowSec - 7200; // 2 jam yang lalu

        list.add(a1);
        list.add(a2);
        list.add(a3);

        long targetEpoch = FeedCache.calculateNextRevalidateEpoch(list);
        // Harus memilih jadwal tayang terdekat di masa depan (1 jam mendatang = a1.airingAt)
        assertEquals(a1.airingAt, targetEpoch);
    }

    @Test
    public void testCalculateNextRevalidateEpochFallback() {
        long nowSec = System.currentTimeMillis() / 1000L;
        List<Anime> list = new ArrayList<>();

        // Anime tamat / tidak punya airingAt di masa depan
        Anime a1 = new Anime();
        a1.title = "Anime Tamat";
        a1.status = "FINISHED";
        a1.airingAt = 0;
        list.add(a1);

        long targetEpoch = FeedCache.calculateNextRevalidateEpoch(list);
        // Harus fallback ke ~6 jam dari sekarang
        assertTrue(targetEpoch >= nowSec + (6L * 3600L) - 5);
        assertTrue(targetEpoch <= nowSec + (6L * 3600L) + 5);
    }

    @Test
    public void testIsRevalidationNeeded() {
        long nowSec = System.currentTimeMillis() / 1000L;
        long nowMs = System.currentTimeMillis();

        // 1. Payload null -> butuh revalidasi
        assertTrue(FeedCache.isRevalidationNeeded(null));

        // 2. Belum jam tayang dan umur cache baru 10 menit -> TIDAK butuh revalidasi
        List<Anime> items = new ArrayList<>();
        Anime a = new Anime();
        a.title = "Test";
        items.add(a);

        FeedCache.CachePayload freshPayload = new FeedCache.CachePayload(
                items, true, 50, nowMs - (10 * 60 * 1000L), nowSec + 7200 // jadwal rilis 2 jam lagi
        );
        assertFalse(FeedCache.isRevalidationNeeded(freshPayload));

        // 3. Sudah lewat jadwal tayang anime (nowSec >= nextRevalidateEpochSec) -> BUTUH revalidasi
        FeedCache.CachePayload airedPayload = new FeedCache.CachePayload(
                items, true, 50, nowMs - (10 * 60 * 1000L), nowSec - 60 // baru rilis 1 menit lalu
        );
        assertTrue(FeedCache.isRevalidationNeeded(airedPayload));

        // 4. Melebihi umur maksimum 6 jam -> BUTUH revalidasi
        FeedCache.CachePayload oldPayload = new FeedCache.CachePayload(
                items, true, 50, nowMs - (7L * 3600L * 1000L), nowSec + 3600
        );
        assertTrue(FeedCache.isRevalidationNeeded(oldPayload));
    }

    @Test
    public void testAnimeVisualEquivalence() {
        Anime a = new Anime();
        a.id = 101;
        a.slug = "frieren";
        a.title = "Frieren";
        a.score = 9.2;
        a.airedEpisodesCount = 28;
        a.epsCounter = "Ep 28";
        a.status = "FINISHED";

        Anime b = new Anime();
        b.id = 101;
        b.slug = "frieren";
        b.title = "Frieren";
        b.score = 9.2;
        b.airedEpisodesCount = 28;
        b.epsCounter = "Ep 28";
        b.status = "FINISHED";

        // Objek berbeda namun representasi visual identik
        assertTrue(a.isVisuallyEquivalent(b));

        // Jika episode baru rilis (airedEpisodesCount bertambah) -> tidak identik
        b.airedEpisodesCount = 29;
        b.epsCounter = "Ep 29";
        assertFalse(a.isVisuallyEquivalent(b));
    }

    @Test
    public void testSerializationDeserializationFidelity() {
        long nowSec = System.currentTimeMillis() / 1000L;
        List<Anime> items = new ArrayList<>();

        Anime a = new Anime();
        a.id = 555;
        a.slug = "dandadan";
        a.title = "Dandadan";
        a.score = 8.6;
        a.airedEpisodesCount = 4;
        a.nextAiringEpisode = 5;
        a.airingAt = nowSec + 86400;
        a.epsCounter = "Ep 4";
        a.status = "RELEASING";
        items.add(a);

        FeedCache.CachePayload payload = new FeedCache.CachePayload(
                items, true, 100, System.currentTimeMillis(), a.airingAt
        );

        String json = FeedCache.serializePayload(payload);
        assertNotNull(json);
        assertTrue(json.contains("dandadan"));

        FeedCache.CachePayload restored = FeedCache.deserializePayload(json);
        assertNotNull(restored);
        assertEquals(1, restored.items.size());
        Anime restoredA = restored.items.get(0);

        assertEquals(a.id, restoredA.id);
        assertEquals(a.slug, restoredA.slug);
        assertEquals(a.title, restoredA.title);
        assertEquals(a.airedEpisodesCount, restoredA.airedEpisodesCount);
        assertEquals(a.nextAiringEpisode, restoredA.nextAiringEpisode);
        assertEquals(a.airingAt, restoredA.airingAt);
        assertTrue(a.isVisuallyEquivalent(restoredA));
    }
}
