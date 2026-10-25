package id.menatap.app.data.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import id.menatap.app.data.model.Anime;
import id.menatap.app.data.model.Episode;

public class AniListSeasonIsolationTest {

    @Test
    public void testIsLongRunningClassification() {
        // Seasonal anime with defined episode count (<= 30) should NEVER be classified as long running
        Anime seasonal1 = new Anime();
        seasonal1.title = "Bleach: Thousand-Year Blood War - The Conflict";
        seasonal1.slug = "bleach-thousand-year-blood-war";
        seasonal1.episodesCount = 13;
        seasonal1.airedEpisodesCount = 4;
        assertFalse("Bleach TYBW (13 eps) must NOT be classified as long-running", AniListClient.isLongRunning(seasonal1));

        Anime seasonal2 = new Anime();
        seasonal2.title = "Re:Zero kara Hajimeru Isekai Seikatsu 3rd Season";
        seasonal2.episodesCount = 16;
        seasonal2.airedEpisodesCount = 4;
        assertFalse("Re:Zero S3 (16 eps) must NOT be classified as long-running", AniListClient.isLongRunning(seasonal2));

        // True long-running franchises should be classified as long-running
        Anime longRunning1 = new Anime();
        longRunning1.title = "One Piece";
        longRunning1.slug = "one-piece";
        longRunning1.episodesCount = 0;
        longRunning1.airedEpisodesCount = 1120;
        assertTrue("One Piece must be classified as long-running", AniListClient.isLongRunning(longRunning1));

        Anime longRunning2 = new Anime();
        longRunning2.title = "Detective Conan";
        longRunning2.slug = "detective-conan";
        longRunning2.episodesCount = 0;
        assertTrue("Detective Conan must be classified as long-running", AniListClient.isLongRunning(longRunning2));

        Anime longRunning3 = new Anime();
        longRunning3.title = "Doraemon (2005)";
        longRunning3.slug = "doraemon-2005";
        longRunning3.episodesCount = 0;
        assertTrue("Doraemon must be classified as long-running", AniListClient.isLongRunning(longRunning3));
    }

    @Test
    public void testParseMediaNodeEpisodeAccuracy() throws Exception {
        // Case 1: Releasing anime with 4 episodes aired out of 12
        JSONObject node1 = new JSONObject();
        node1.put("id", 1001);
        JSONObject t1 = new JSONObject();
        t1.put("english", "Dandadan");
        t1.put("romaji", "Dandadan");
        node1.put("title", t1);
        node1.put("format", "TV");
        node1.put("status", "RELEASING");
        node1.put("episodes", 12);
        JSONObject nextAiring1 = new JSONObject();
        nextAiring1.put("episode", 5);
        nextAiring1.put("airingAt", 1730000000);
        node1.put("nextAiringEpisode", nextAiring1);

        Anime anime1 = AniListClient.parseMediaNode(node1);
        assertEquals("Aired episodes count should be nextAiring - 1 (4)", 4, anime1.airedEpisodesCount);
        assertEquals("Total episodes count should be 12", 12, anime1.episodesCount);
        assertEquals("epsCounter should show Ep 4/12", "Ep 4/12", anime1.epsCounter);

        // Case 2: Releasing or not yet released anime where episode 1 has NOT aired
        JSONObject node2 = new JSONObject();
        node2.put("id", 1002);
        JSONObject t2 = new JSONObject();
        t2.put("english", "Upcoming Anime");
        node2.put("title", t2);
        node2.put("format", "TV");
        node2.put("status", "NOT_YET_RELEASED");
        node2.put("episodes", 12);

        Anime anime2 = AniListClient.parseMediaNode(node2);
        assertEquals("Aired episodes count for unreleased anime must be 0", 0, anime2.airedEpisodesCount);
        assertEquals("epsCounter for unreleased anime must be 'Belum Tayang'", "Belum Tayang", anime2.epsCounter);

        // Case 3: Releasing anime with nextAiringEpisode = 1 (airing today/tomorrow, ep 1 not out yet)
        JSONObject node3 = new JSONObject();
        node3.put("id", 1003);
        JSONObject t3 = new JSONObject();
        t3.put("english", "Airing Today Anime");
        node3.put("title", t3);
        node3.put("format", "TV");
        node3.put("status", "RELEASING");
        node3.put("episodes", 24);
        JSONObject nextAiring3 = new JSONObject();
        nextAiring3.put("episode", 1);
        node3.put("nextAiringEpisode", nextAiring3);

        Anime anime3 = AniListClient.parseMediaNode(node3);
        assertEquals("Aired episodes count must be 0 when next episode is 1", 0, anime3.airedEpisodesCount);
        assertEquals("epsCounter must show 'Belum Tayang' before ep 1 airs", "Belum Tayang", anime3.epsCounter);

        // Case 4: Finished anime
        JSONObject node4 = new JSONObject();
        node4.put("id", 1004);
        JSONObject t4 = new JSONObject();
        t4.put("english", "Finished Classic");
        node4.put("title", t4);
        node4.put("format", "TV");
        node4.put("status", "FINISHED");
        node4.put("episodes", 24);

        Anime anime4 = AniListClient.parseMediaNode(node4);
        assertEquals("Aired episodes count should match total for finished anime", 24, anime4.airedEpisodesCount);
        assertEquals("epsCounter should show '24 Ep (Tamat)'", "24 Ep (Tamat)", anime4.epsCounter);
    }

    @Test
    public void testAccurateEpisodeFilteringLogic() {
        // Simulate an ongoing anime with next episode = 5 (meaning episodes 1-4 have aired)
        Anime ongoing = new Anime();
        ongoing.status = "RELEASING";
        ongoing.nextAiringEpisode = 5;
        ongoing.airedEpisodesCount = 4;
        ongoing.episodesCount = 12;

        // Scraper accidentally returned 12 episodes (e.g. placeholder or previous season)
        List<Episode> rawList = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            Episode ep = new Episode(i, "Episode " + i, "slug-" + i);
            rawList.add(ep);
        }

        // Apply filtering logic
        int maxAired = ongoing.nextAiringEpisode > 1 ? ongoing.nextAiringEpisode - 1 : ongoing.airedEpisodesCount;
        List<Episode> filtered = new ArrayList<>();
        for (Episode ep : rawList) {
            if ("RELEASING".equalsIgnoreCase(ongoing.status) && maxAired < Integer.MAX_VALUE && ep.number > maxAired) {
                continue;
            }
            filtered.add(ep);
        }

        assertEquals("Filtered list should only contain 4 episodes", 4, filtered.size());
        assertEquals("Last episode number should be 4", 4, filtered.get(filtered.size() - 1).number);
    }
}
