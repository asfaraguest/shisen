package id.shisen.app.data.subtitle;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class DeepLxTranslatorTest {

    @Test
    public void testCleanAdTextFiltering() {
        assertEquals("", SmartLexicon.cleanAdText("Daftar slot gacor maxwin deposit bonus di www.judislot.xyz"));
        assertEquals("", SmartLexicon.cleanAdText("Diterjemahkan oleh Fansub Indonesia"));
        assertEquals("", SmartLexicon.cleanAdText("Subtitles downloaded from opensubtitles.org"));
        assertEquals("", SmartLexicon.cleanAdText("https://t.me/animesubindo"));
        assertEquals("Ayo lari!", SmartLexicon.cleanAdText("Ayo lari! (subscene.com)"));
        assertEquals("Kita harus cepat!", SmartLexicon.cleanAdText("Kita harus cepat!"));
    }

    @Test
    public void testSanitizeAdsListIntegration() {
        List<SubtitleCue> cues = new ArrayList<>();
        cues.add(new SubtitleCue(1000, 2000, "Promo slot gacor zeus"));
        cues.add(new SubtitleCue(2500, 4000, "Halo teman-teman!"));
        cues.add(new SubtitleCue(4500, 6000, "Translated by Funimation"));
        cues.add(new SubtitleCue(6500, 8000, "Sampai jumpa besok!"));

        List<SubtitleCue> clean = SmartLexicon.sanitizeAdsAndWatermarks(cues);
        assertEquals(2, clean.size());
        assertEquals("Halo teman-teman!", clean.get(0).text);
        assertEquals("Sampai jumpa besok!", clean.get(1).text);
    }
}
