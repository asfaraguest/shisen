package id.menatap.app.data.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import id.menatap.app.data.model.Anime;

public class AniListRelationsTest {

    @Test
    public void testJujutsuKaisenRelationsClassification() throws Exception {
        // Target: Jujutsu Kaisen 2nd Season (ID: 145064)
        JSONObject root = new JSONObject();
        JSONObject data = new JSONObject();
        JSONObject media = new JSONObject();
        media.put("id", 145064);

        JSONObject relObj = new JSONObject();
        JSONArray edges = new JSONArray();

        // 1. PREQUEL: TV 2020 Jujutsu Kaisen S1
        JSONObject edge1 = new JSONObject();
        edge1.put("relationType", "PREQUEL");
        JSONObject node1 = new JSONObject();
        node1.put("id", 113415);
        JSONObject t1 = new JSONObject();
        t1.put("english", "JUJUTSU KAISEN");
        t1.put("romaji", "Jujutsu Kaisen");
        node1.put("title", t1);
        node1.put("format", "TV");
        node1.put("seasonYear", 2020);
        node1.put("status", "FINISHED");

        // Sub-relations of S1: Movie 0
        JSONObject subRels1 = new JSONObject();
        JSONArray subEdges1 = new JSONArray();
        JSONObject subEdge1_1 = new JSONObject();
        subEdge1_1.put("relationType", "SEQUEL");
        JSONObject subNode1_1 = new JSONObject();
        subNode1_1.put("id", 131573);
        JSONObject tSub = new JSONObject();
        tSub.put("english", "JUJUTSU KAISEN 0");
        tSub.put("romaji", "Jujutsu Kaisen 0");
        subNode1_1.put("title", tSub);
        subNode1_1.put("format", "MOVIE");
        subNode1_1.put("seasonYear", 2021);
        subNode1_1.put("status", "FINISHED");
        subEdge1_1.put("node", subNode1_1);
        subEdges1.put(subEdge1_1);
        subRels1.put("edges", subEdges1);
        node1.put("relations", subRels1);
        edge1.put("node", node1);
        edges.put(edge1);

        // 2. ADAPTATION: MANGA (Should be ignored!)
        JSONObject edge2 = new JSONObject();
        edge2.put("relationType", "ADAPTATION");
        JSONObject node2 = new JSONObject();
        node2.put("id", 101554);
        JSONObject t2 = new JSONObject();
        t2.put("romaji", "Jujutsu Kaisen");
        node2.put("title", t2);
        node2.put("format", "MANGA");
        edge2.put("node", node2);
        edges.put(edge2);

        // 3. SEQUEL: TV 2026 Season 3 Culling Game
        JSONObject edge3 = new JSONObject();
        edge3.put("relationType", "SEQUEL");
        JSONObject node3 = new JSONObject();
        node3.put("id", 172463);
        JSONObject t3 = new JSONObject();
        t3.put("english", "JUJUTSU KAISEN Season 3: The Culling Game");
        t3.put("romaji", "Jujutsu Kaisen: Shimetsu Kaiyuu");
        node3.put("title", t3);
        node3.put("format", "TV");
        node3.put("seasonYear", 2026);
        node3.put("status", "NOT_YET_RELEASED");
        edge3.put("node", node3);
        edges.put(edge3);

        // 4. ALTERNATIVE: MOVIE 2025 Execution
        JSONObject edge4 = new JSONObject();
        edge4.put("relationType", "ALTERNATIVE");
        JSONObject node4 = new JSONObject();
        node4.put("id", 188888);
        JSONObject t4 = new JSONObject();
        t4.put("english", "JUJUTSU KAISEN: Execution");
        node4.put("title", t4);
        node4.put("format", "MOVIE");
        node4.put("seasonYear", 2025);
        edge4.put("node", node4);
        edges.put(edge4);

        relObj.put("edges", edges);
        media.put("relations", relObj);
        data.put("Media", media);
        root.put("data", data);

        AniListClient.RelationsResult res = AniListClient.parseRelationsResponse(root, 145064);

        assertNotNull(res);
        assertEquals(2, res.seasons.size());
        assertEquals(2, res.movies.size());

        // Check Seasons
        Anime s1 = res.seasons.get(0);
        assertEquals(113415, s1.id);
        assertEquals("Season 1", s1.relationLabel); // Earliest prequel refined to Season 1
        assertEquals(2020, s1.year);

        Anime s3 = res.seasons.get(1);
        assertEquals(172463, s3.id);
        assertEquals("Season 3", s3.relationLabel);
        assertEquals(2026, s3.year);

        // Check Movies
        Anime m1 = res.movies.get(0);
        assertEquals(131573, m1.id);
        assertEquals(2021, m1.year);

        Anime m2 = res.movies.get(1);
        assertEquals(188888, m2.id);
        assertEquals(2025, m2.year);
    }

    @Test
    public void testHentaiGenreBlacklist() throws Exception {
        JSONObject root = new JSONObject();
        JSONObject data = new JSONObject();
        JSONObject media = new JSONObject();
        media.put("id", 100);

        JSONObject relObj = new JSONObject();
        JSONArray edges = new JSONArray();

        JSONObject edgeH = new JSONObject();
        edgeH.put("relationType", "SIDE_STORY");
        JSONObject nodeH = new JSONObject();
        nodeH.put("id", 200);
        JSONObject tH = new JSONObject();
        tH.put("romaji", "Blacklisted H Show");
        nodeH.put("title", tH);
        nodeH.put("format", "OVA");
        JSONArray genres = new JSONArray();
        genres.put("Comedy");
        genres.put("Hentai");
        nodeH.put("genres", genres);
        edgeH.put("node", nodeH);
        edges.put(edgeH);

        relObj.put("edges", edges);
        media.put("relations", relObj);
        data.put("Media", media);
        root.put("data", data);

        AniListClient.RelationsResult res = AniListClient.parseRelationsResponse(root, 100);

        assertTrue("Hentai relation must be stripped", res.seasons.isEmpty());
        assertTrue("No movies from hentai relation", res.movies.isEmpty());
    }

    @Test
    public void testMoviePartNumberLabeling() throws Exception {
        JSONObject root = new JSONObject();
        JSONObject data = new JSONObject();
        JSONObject media = new JSONObject();
        media.put("id", 10);

        JSONObject relObj = new JSONObject();
        JSONArray edges = new JSONArray();

        JSONObject edge = new JSONObject();
        edge.put("relationType", "SEQUEL");
        JSONObject node = new JSONObject();
        node.put("id", 20);
        JSONObject t = new JSONObject();
        t.put("english", "Girls und Panzer das Finale: Part 2");
        node.put("title", t);
        node.put("format", "MOVIE");
        node.put("seasonYear", 2019);
        edge.put("node", node);
        edges.put(edge);

        relObj.put("edges", edges);
        media.put("relations", relObj);
        data.put("Media", media);
        root.put("data", data);

        AniListClient.RelationsResult res = AniListClient.parseRelationsResponse(root, 10);

        assertEquals(1, res.movies.size());
        assertEquals("Part 2", res.movies.get(0).relationLabel);
    }
}
