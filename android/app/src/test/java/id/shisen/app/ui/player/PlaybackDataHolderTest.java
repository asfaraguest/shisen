package id.shisen.app.ui.player;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import id.shisen.app.data.model.Anime;
import id.shisen.app.data.model.Episode;

import static org.junit.Assert.*;

public class PlaybackDataHolderTest {

    @Before
    @After
    public void tearDown() {
        PlaybackDataHolder.clear();
    }

    @Test
    public void testLargeEpisodeListRetentionWithoutCrash() {
        // Simulasikan anime episode panjang (misal One Piece: 1100+ episode)
        Anime onePiece = new Anime();
        onePiece.slug = "one-piece";
        onePiece.title = "One Piece";
        onePiece.anilistId = 21;

        List<Episode> episodes = new ArrayList<>();
        for (int i = 1; i <= 1500; i++) {
            Episode ep = new Episode(i, "Episode " + i, "one-piece-episode-" + i);
            episodes.add(ep);
        }

        PlaybackDataHolder.set(onePiece, episodes, 1050, 450000L);

        assertTrue(PlaybackDataHolder.hasData());
        assertEquals("one-piece", PlaybackDataHolder.getAnime().slug);
        assertEquals(1500, PlaybackDataHolder.getEpisodes().size());
        assertEquals(1050, PlaybackDataHolder.getIndex());
        assertEquals(450000L, PlaybackDataHolder.getPositionMs());
    }

    @Test
    public void testClearDataHolderContract() {
        Anime naruto = new Anime();
        naruto.slug = "naruto-shippuden";
        List<Episode> eps = new ArrayList<>();
        eps.add(new Episode(1, "Episode 1", "naruto-shippuden-1"));

        PlaybackDataHolder.set(naruto, eps, 0);
        assertTrue(PlaybackDataHolder.hasData());

        PlaybackDataHolder.clear();
        assertFalse(PlaybackDataHolder.hasData());
        assertNull(PlaybackDataHolder.getAnime());
        assertTrue(PlaybackDataHolder.getEpisodes().isEmpty());
    }
}
