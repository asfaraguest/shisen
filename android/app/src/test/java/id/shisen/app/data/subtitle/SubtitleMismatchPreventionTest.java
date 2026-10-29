package id.shisen.app.data.subtitle;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import id.shisen.app.data.model.SubtitleTrack;
import id.shisen.app.util.TitleMatcher;

import static org.junit.Assert.*;

public class SubtitleMismatchPreventionTest {

    @Test
    public void testFindEnglishTrackPrefersFullDialogueOverSigns() {
        List<SubtitleTrack> tracks = new ArrayList<>();
        // Skenario: Track tanda/lagu berada di posisi awal sebelum dialog penuh
        tracks.add(new SubtitleTrack("https://cdn.example.com/en-signs.vtt", "en", "English - Signs & Songs", false));
        tracks.add(new SubtitleTrack("https://cdn.example.com/en-full.vtt", "en", "English [CC]", true));

        SubtitleTrack selected = SubtitleTrack.findEnglishTrack(tracks);
        assertNotNull(selected);
        assertEquals("English [CC]", selected.label);
        assertEquals("https://cdn.example.com/en-full.vtt", selected.url);
        assertFalse(SubtitleTrack.isSignsOrSongsOnly(selected));
    }

    @Test
    public void testIsSignsOrSongsOnlyDetector() {
        assertTrue(SubtitleTrack.isSignsOrSongsOnly(new SubtitleTrack("url", "en", "English (Signs & Songs)", false)));
        assertTrue(SubtitleTrack.isSignsOrSongsOnly(new SubtitleTrack("url", "en", "English - Songs & Signs", false)));
        assertTrue(SubtitleTrack.isSignsOrSongsOnly(new SubtitleTrack("url", "en", "Signs", false)));
        assertTrue(SubtitleTrack.isSignsOrSongsOnly(new SubtitleTrack("url", "en", "Forced", false)));
        assertTrue(SubtitleTrack.isSignsOrSongsOnly(new SubtitleTrack("url", "en", "Lyrics Only", false)));

        assertFalse(SubtitleTrack.isSignsOrSongsOnly(new SubtitleTrack("url", "en", "English", true)));
        assertFalse(SubtitleTrack.isSignsOrSongsOnly(new SubtitleTrack("url", "en", "English [CC]", true)));
        assertFalse(SubtitleTrack.isSignsOrSongsOnly(new SubtitleTrack("url", "id", "Indonesian", true)));
    }

    @Test
    public void testSubtitleMergeEngineIsMatchValid() {
        // Skenario 1: Episode yang sama, snapping ratio tinggi (60% cue selaras)
        SubtitleMergeEngine.MergeResult goodResult = new SubtitleMergeEngine.MergeResult(
                new ArrayList<>(), 0L, 60, 5
        );
        assertTrue(SubtitleMergeEngine.isMatchValid(goodResult, 100, 100));

        // Skenario 2: Anime lain atau episode lain (hanya 5 cue kebetulan dekat dari 100 cue)
        SubtitleMergeEngine.MergeResult badResult = new SubtitleMergeEngine.MergeResult(
                new ArrayList<>(), 0L, 5, 20
        );
        assertFalse("Kecocokan snapping di bawah 35% wajib ditolak (mismatch terdeteksi)",
                SubtitleMergeEngine.isMatchValid(badResult, 100, 100));

        // Skenario 3: Video pendek (< 30 cue)
        SubtitleMergeEngine.MergeResult shortValid = new SubtitleMergeEngine.MergeResult(
                new ArrayList<>(), 0L, 5, 0
        );
        assertTrue(SubtitleMergeEngine.isMatchValid(shortValid, 15, 15));
    }

    @Test
    public void testSeasonExtractionConsistencyForAnimeTitles() {
        assertEquals(2, TitleMatcher.extractSeasonNumber("Spy x Family Season 2"));
        assertEquals(2, TitleMatcher.extractSeasonNumber("Jujutsu Kaisen 2nd Season"));
        assertEquals(3, TitleMatcher.extractSeasonNumber("Overlord Season III"));
        assertEquals(4, TitleMatcher.extractSeasonNumber("Overlord Season IV"));
        assertEquals(2, TitleMatcher.extractSeasonNumber("Mushoku Tensei II: Isekai Ittara Honki Dasu"));
        assertEquals(1, TitleMatcher.extractSeasonNumber("Sousou no Frieren"));
    }
}
