package id.menatap.app.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class TitleMatcherTest {

    @Test
    public void testReZeroMultiSeasonGoldCases() {
        // Season 2 English vs Romaji match
        assertTrue("Re:Zero Season 2 should match Romaji 2nd Season",
                TitleMatcher.isTitleMatch(
                        "Re:ZERO -Starting Life in Another World- Season 2",
                        "Re:Zero kara Hajimeru Isekai Seikatsu 2nd Season"));

        // Season 3 English vs Romaji match
        assertTrue("Re:Zero Season 3 should match Romaji 3rd Season",
                TitleMatcher.isTitleMatch(
                        "Re:ZERO -Starting Life in Another World- Season 3",
                        "Re:Zero kara Hajimeru Isekai Seikatsu 3rd Season"));

        // S3 must NEVER match S2
        assertFalse("Re:Zero Season 3 must NOT match Season 2",
                TitleMatcher.isTitleMatch(
                        "Re:ZERO -Starting Life in Another World- Season 3",
                        "Re:Zero kara Hajimeru Isekai Seikatsu 2nd Season"));

        // S1 must NEVER match S2
        assertFalse("Re:Zero Season 1 must NOT match Season 2",
                TitleMatcher.isTitleMatch(
                        "Re:ZERO -Starting Life in Another World-",
                        "Re:Zero kara Hajimeru Isekai Seikatsu 2nd Season"));
    }

    @Test
    public void testJujutsuKaisenMultiSeasonGoldCases() {
        // S2 match
        assertTrue("Jujutsu Kaisen 2nd Season should match Season 2",
                TitleMatcher.isTitleMatch(
                        "Jujutsu Kaisen 2nd Season",
                        "Jujutsu Kaisen Season 2"));

        // S2 must NEVER match S1
        assertFalse("Jujutsu Kaisen 2nd Season must NOT match Season 1",
                TitleMatcher.isTitleMatch(
                        "Jujutsu Kaisen 2nd Season",
                        "Jujutsu Kaisen"));

        assertFalse("Jujutsu Kaisen Season 1 must NOT match 2nd Season",
                TitleMatcher.isTitleMatch(
                        "Jujutsu Kaisen Season 1",
                        "Jujutsu Kaisen 2nd Season"));
    }

    @Test
    public void testOshiNoKoMultiSeasonGoldCases() {
        // Oshi no Ko S2 with brackets
        assertTrue("Oshi no Ko Season 2 should match stylized title",
                TitleMatcher.isTitleMatch(
                        "Oshi no Ko Season 2",
                        "【OSHI NO KO】 Season 2"));

        // Oshi no Ko S1 with brackets
        assertTrue("Oshi no Ko Season 1 should match stylized title",
                TitleMatcher.isTitleMatch(
                        "Oshi no Ko",
                        "【OSHI NO KO】"));

        // Oshi no Ko S2 must NOT match S1
        assertFalse("Oshi no Ko Season 2 must NOT match Season 1",
                TitleMatcher.isTitleMatch(
                        "Oshi no Ko Season 2",
                        "【OSHI NO KO】"));
    }

    @Test
    public void testFrierenApostropheAndHyphenGoldCases() {
        // "Journey's" apostrophe vs "Journeys" without apostrophe
        assertTrue("Frieren with apostrophe must match without apostrophe",
                TitleMatcher.isTitleMatch(
                        "Frieren: Beyond Journey's End",
                        "Sousou no Frieren: Beyond Journeys End"));

        // Sluggified hyphen format "journey-s"
        assertTrue("Frieren with apostrophe must match sluggified journey-s",
                TitleMatcher.isTitleMatch(
                        "Frieren: Beyond Journey's End",
                        "frieren-beyond-journey-s-end"));
    }

    @Test
    public void testTheWorldsStrongestWitchGoldCases() {
        // "The World's Strongest Witch" vs "the-world-s-strongest-witch"
        assertTrue("The World's Strongest Witch must match slug",
                TitleMatcher.isTitleMatch(
                        "The World's Strongest Witch",
                        "the-world-s-strongest-witch"));

        assertTrue("The World's Strongest Witch must match normalized space",
                TitleMatcher.isTitleMatch(
                        "The World's Strongest Witch",
                        "The Worlds Strongest Witch"));
    }

    @Test
    public void testSeasonRomanNumeralsAndPartTokens() {
        assertEquals(2, TitleMatcher.extractSeasonNumber("Overlord Season II"));
        assertEquals(3, TitleMatcher.extractSeasonNumber("Overlord Season III"));
        assertEquals(4, TitleMatcher.extractSeasonNumber("Overlord Season IV"));

        assertEquals(2, TitleMatcher.extractSeasonNumber("Bleach: Thousand-Year Blood War - Part 2"));
        assertEquals(3, TitleMatcher.extractSeasonNumber("Bleach: Thousand-Year Blood War - Part 3"));
    }

    @Test
    public void testMovieAndOvaIsolation() {
        // Movie vs TV series must NOT match
        assertFalse("Movie must not match TV series",
                TitleMatcher.isTitleMatch(
                        "KonoSuba: God's Blessing on this Wonderful World! Legend of Crimson Movie",
                        "KonoSuba: God's Blessing on this Wonderful World! Season 2"));

        // OVA vs TV series must NOT match
        assertFalse("OVA must not match TV series",
                TitleMatcher.isTitleMatch(
                        "Attack on Titan OVA",
                        "Attack on Titan Season 1"));
    }
}
