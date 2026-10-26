package id.shisen.app.data.subtitle;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SmartLexiconTest {

    @Test
    public void testOvergearedSatisfyGameNameCorrections() {
        assertEquals("Selamat datang di Satisfy.",
                SmartLexicon.applyUniversalLexicon("Selamat datang di memuaskan."));

        assertEquals("Aku sedang bermain Satisfy.",
                SmartLexicon.applyUniversalLexicon("Aku sedang bermain memuaskan."));

        assertEquals("Dia adalah pemain Satisfy peringkat satu.",
                SmartLexicon.applyUniversalLexicon("Dia adalah pemain memuaskan peringkat satu."));

        assertEquals("Kapsul Satisfy milikku rusak.",
                SmartLexicon.applyUniversalLexicon("Kapsul memuaskan milikku rusak."));

        assertEquals("Masuk ke Satisfy!",
                SmartLexicon.applyUniversalLexicon("Masuk ke memuaskan!"));

        assertEquals("Di dalam Satisfy, dia sangat kuat.",
                SmartLexicon.applyUniversalLexicon("Di dalam memuaskan, dia sangat kuat."));

        assertEquals("Satisfy adalah game realitas virtual terbaik di dunia.",
                SmartLexicon.applyUniversalLexicon("Memuaskan adalah game realitas virtual terbaik di dunia."));

        assertEquals("Berita Satisfy hari ini sangat heboh.",
                SmartLexicon.applyUniversalLexicon("Berita memuaskan hari ini sangat heboh."));

        assertEquals("Satisfy!",
                SmartLexicon.applyUniversalLexicon("Memuaskan!"));

        assertEquals("Bukan Satisfy!",
                SmartLexicon.applyUniversalLexicon("Bukan memuaskan!"));

        assertEquals("Kompetisi Nasional Satisfy akan segera digelar.",
                SmartLexicon.applyUniversalLexicon("Kompetisi nasional memuaskan akan segera digelar."));

        assertEquals("Terkoneksi ke Satisfy...",
                SmartLexicon.applyUniversalLexicon("Terkoneksi ke memuaskan..."));

        assertEquals("Server Satisfy mengalami pembaruan.",
                SmartLexicon.applyUniversalLexicon("Server memuaskan mengalami pembaruan."));

        assertEquals("Akun Satisfy miliknya telah diblokir.",
                SmartLexicon.applyUniversalLexicon("Akun memuaskan miliknya telah diblokir."));
    }

    @Test
    public void testOvergearedWorldAndCharacterTerms() {
        assertEquals("Keturunan Pagma",
                SmartLexicon.applyUniversalLexicon("penerus pagma"));

        assertEquals("Kitab Langka Pagma",
                SmartLexicon.applyUniversalLexicon("buku langka pagma"));

        assertEquals("Pandai Besi Legendaris",
                SmartLexicon.applyUniversalLexicon("legendary blacksmith"));

        assertEquals("Guild Tzedakah",
                SmartLexicon.applyUniversalLexicon("tzedakah guild"));

        assertEquals("Superkomputer Morpheus",
                SmartLexicon.applyUniversalLexicon("supercomputer morpheus"));

        assertEquals("Cincin Doran",
                SmartLexicon.applyUniversalLexicon("doran's ring"));

        assertEquals("Pavranium",
                SmartLexicon.applyUniversalLexicon("pavranium"));
    }

    @Test
    public void testUniversalRpgAndAnimeGlossary() {
        assertEquals("masuk ke dungeon",
                SmartLexicon.applyUniversalLexicon("masuk ke penjara bawah tanah"));

        assertEquals("guild petualang",
                SmartLexicon.applyUniversalLexicon("serikat petualang"));

        assertEquals("party petualang",
                SmartLexicon.applyUniversalLexicon("pesta petualang"));

        assertEquals("menarik aggro",
                SmartLexicon.applyUniversalLexicon("menarik kebencian"));

        assertEquals("titik spawn",
                SmartLexicon.applyUniversalLexicon("titik bertelur"));

        assertEquals("waktu cooldown",
                SmartLexicon.applyUniversalLexicon("waktu pendinginan"));

        assertEquals("bar HP",
                SmartLexicon.applyUniversalLexicon("bilah kesehatan"));

        assertEquals("Sword Art Online",
                SmartLexicon.applyUniversalLexicon("seni pedang daring"));
    }

    @Test
    public void testSceneContextSanitization() {
        // Percakapan santai
        String casual = SmartLexicon.sanitizeGtxPlusId("saya tidak tahu anda ada di sana", false);
        assertEquals("aku nggak tahu kamu ada di sana", casual);

        // Percakapan formal di istana kerajaan
        String formal = SmartLexicon.sanitizeGtxPlusId("Yang Mulia, saya akan menghadap Paduka besok.", false);
        assertTrue(formal.contains("Yang Mulia"));
        assertTrue(formal.contains("saya") || formal.contains("Saya"));
    }

    @Test
    public void testPronounAutomationOpenSubtitles() {
        List<SubtitleCue> cues = new ArrayList<>();
        cues.add(new SubtitleCue(1000, 3000, "saya tidak mau pergi"));
        cues.add(new SubtitleCue(3500, 5000, "apakah anda serius?"));

        List<SubtitleCue> result = SmartLexicon.automateOpenSubtitlesPronouns(cues, "id", 2, 10.0);
        assertEquals("aku nggak mau pergi", result.get(0).text);
        assertTrue(result.get(1).text.contains("kamu") || result.get(1).text.contains("Apa kamu"));
    }
}
