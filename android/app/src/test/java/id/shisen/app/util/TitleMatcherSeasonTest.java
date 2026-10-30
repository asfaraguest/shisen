package id.shisen.app.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import id.shisen.app.data.model.Anime;

import static org.junit.Assert.*;

public class TitleMatcherSeasonTest {

    private static Anime anime(String title, String romaji, String slug) {
        Anime a = new Anime();
        a.title = title;
        a.romajiTitle = romaji;
        a.slug = slug;
        return a;
    }

    @Test
    public void testChitoseInRamuneBottleSeason2PicksPart2NotSeason1() {
        Anime targetS2 = anime(
                "Chitose Is in the Ramune Bottle 2nd Cour",
                "Chitose-kun wa Ramune Bin no Naka Part 2",
                "chitose-is-in-the-ramune-bottle-2nd-cour"
        );
        Anime candS1 = anime(
                "Chitose Is in the Ramune Bottle",
                "Chitose-kun wa Ramune Bin no Naka",
                "chitose-is-in-the-ramune-bottle-40"
        );
        Anime candS2 = anime(
                "Chitose Is in the Ramune Bottle Part 2",
                "Chitose-kun wa Ramune Bin no Naka Part 2",
                "chitose-is-in-the-ramune-bottle-part-2-10545"
        );

        assertEquals(2, TitleMatcher.extractSeasonNumber(targetS2.title));
        assertEquals(2, TitleMatcher.extractSeasonNumber(targetS2.romajiTitle));
        assertFalse(TitleMatcher.isMatchAny(targetS2, candS1));
        assertTrue(TitleMatcher.isMatchAny(targetS2, candS2));

        List<Anime> hiAnimeResults = Arrays.asList(candS1, candS2);
        String picked = TitleMatcher.pickBestSlug(hiAnimeResults, targetS2, targetS2.title, targetS2.romajiTitle);
        assertEquals("chitose-is-in-the-ramune-bottle-part-2-10545", picked);
    }

    @Test
    public void testMultiSeasonAndSplitCourSequelsNeverMatchPreviousSeason() {
        String[][][] sequelVsPrequel = {
                // { {s2En, s2Ro}, {s1En, s1Ro} }
                {{"The Angel Next Door Spoils Me Rotten2", "Otonari no Tenshi-sama ni Itsunomanika Dame Ningen ni Sareteita Ken 2nd Season"},
                 {"The Angel Next Door Spoils Me Rotten", "Otonari no Tenshi-sama ni Itsunomanika Dame Ningen ni Sareteita Ken"}},
                {{"Mushoku Tensei: Jobless Reincarnation Season 2 Part 2", "Mushoku Tensei II: Isekai Ittara Honki Dasu Part 2"},
                 {"Mushoku Tensei: Jobless Reincarnation Season 2", "Mushoku Tensei II: Isekai Ittara Honki Dasu"}},
                {{"Fire Force Season 3 Part 2", "Enen no Shouboutai: San no Shou Part 2"},
                 {"Fire Force Season 3", "Enen no Shouboutai: San no Shou"}},
                {{"Dr. STONE SCIENCE FUTURE Cour 2", "Dr. STONE: SCIENCE FUTURE Part 2"},
                 {"Dr. STONE SCIENCE FUTURE", "Dr. STONE: SCIENCE FUTURE"}},
                {{"Dr. STONE SCIENCE FUTURE Cour 3", "Dr. STONE: SCIENCE FUTURE Part 3"},
                 {"Dr. STONE SCIENCE FUTURE Cour 2", "Dr. STONE: SCIENCE FUTURE Part 2"}},
                {{"MASHLE: MAGIC AND MUSCLES Season 2", "MASHLE: Kami Shinkakusha Kouho Senbatsu Shiken-hen"},
                 {"MASHLE: MAGIC AND MUSCLES", "MASHLE"}},
                {{"BLUE LOCK Season 2", "Blue Lock VS. U-20 JAPAN"},
                 {"BLUE LOCK", "Blue Lock"}},
                {{"Farming Life in Another World 2", "Isekai Nonbiri Nouka 2"},
                 {"Farming Life in Another World", "Isekai Nonbiri Nouka"}},
                {{"Chained Soldier Season 2", "Mato Seihei no Slave 2"},
                 {"Chained Soldier", "Mato Seihei no Slave"}},
                {{"Bottom-Tier Character Tomozaki 2nd Stage", "Jaku-Chara Tomozaki-kun 2nd STAGE"},
                 {"Bottom-Tier Character Tomozaki", "Jaku-Chara Tomozaki-kun"}},
                {{"Demon Slayer: Kimetsu no Yaiba Hashira Training Arc", "Kimetsu no Yaiba: Hashira Geiko-hen"},
                 {"Demon Slayer: Kimetsu no Yaiba Swordsmith Village Arc", "Kimetsu no Yaiba: Katanakaji no Sato-hen"}},
                {{"BLEACH: Thousand-Year Blood War - The Calamity", "BLEACH: Sennen Kessen-hen - Kashin-tan"},
                 {"BLEACH: Thousand-Year Blood War - The Conflict", "BLEACH: Sennen Kessen-hen - Soukoku-tan"}},
                {{"The Misfit of Demon King Academy II (Cour 2)", "Maou Gakuin no Futekigousha II Part 2"},
                 {"The Misfit of Demon King Academy \u2161", "Maou Gakuin no Futekigousha II"}},
                {{"Haikyuu!! Second Season", "Haikyuu!! Second Season"},
                 {"Haikyuu!!", "Haikyuu!!"}},
                {{"Kaguya-sama: Love is War?", "Kaguya-sama wa Kokurasetai?"},
                 {"Kaguya-sama: Love is War", "Kaguya-sama wa Kokurasetai"}}
        };

        for (String[][] pair : sequelVsPrequel) {
            Anime s2 = anime(pair[0][0], pair[0][1], "s2-slug");
            Anime s1 = anime(pair[1][0], pair[1][1], "s1-slug");
            assertFalse("Sequel must NOT match prequel: " + s2.title + " vs " + s1.title,
                    TitleMatcher.isMatchAny(s2, s1));
        }
    }

    @Test
    public void testNonSeasonNumbersDoNotTriggerFalseSeason() {
        assertEquals(1, TitleMatcher.extractSeasonNumber("Kaiju No. 8"));
        assertEquals(1, TitleMatcher.extractSeasonNumber("Mob Psycho 100"));
        assertEquals(1, TitleMatcher.extractSeasonNumber("[SubsPlease] Sousou no Frieren - 02 (1080p)"));
    }
}
