package id.shisen.app.data.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import id.shisen.app.data.model.Anime;

import static org.junit.Assert.*;

/**
 * Unit test untuk kontrak Safe Mode dan filter katalog AniListClient.
 */
public class AniListSafeModeTest {

    @After
    public void resetSafeMode() {
        AniListClient.setSafeModeEnabled(false);
    }

    @Test
    public void testSafeModeFlagToggle() {
        AniListClient.setSafeModeEnabled(true);
        assertTrue(AniListClient.isSafeModeEnabled());

        AniListClient.setSafeModeEnabled(false);
        assertFalse(AniListClient.isSafeModeEnabled());
    }

    @Test
    public void testParseMediaNodeKeepsIsAdultNonHentaiAndRejectsHentai() throws Exception {
        JSONObject titleObj = new JSONObject();
        titleObj.put("english", "Ishuzoku Reviewers");
        titleObj.put("romaji", "Ishuzoku Reviewers");

        JSONArray ecchiGenres = new JSONArray();
        ecchiGenres.put("Comedy");
        ecchiGenres.put("Ecchi");

        JSONObject adultEcchi = new JSONObject();
        adultEcchi.put("id", 110270);
        adultEcchi.put("title", titleObj);
        adultEcchi.put("isAdult", true);
        adultEcchi.put("genres", ecchiGenres);

        Anime parsedEcchi = AniListClient.parseMediaNode(adultEcchi);
        assertNotNull("Anime dewasa non-Hentai harus tetap diparse dengan flag isAdult", parsedEcchi);
        assertTrue(parsedEcchi.isAdult);

        JSONArray hentaiGenres = new JSONArray();
        hentaiGenres.put("Hentai");

        JSONObject hentaiNode = new JSONObject();
        hentaiNode.put("id", 999999);
        hentaiNode.put("title", titleObj);
        hentaiNode.put("isAdult", true);
        hentaiNode.put("genres", hentaiGenres);

        assertNull("Genre Hentai harus selalu ditolak oleh parseMediaNode", AniListClient.parseMediaNode(hentaiNode));
    }

    @Test
    public void testBuildCatalogFiltersYearOnlyUsesStartDateRange() {
        String yearOnly = AniListClient.buildCatalogFilters("2026", "", "", "", "");
        assertTrue(yearOnly.contains("startDate_greater: 20260101"));
        assertTrue(yearOnly.contains("startDate_lesser: 20261231"));
        assertFalse("Filter tahun tanpa musim tidak boleh pakai seasonYear agar Movie/OVA/ONA tetap muncul",
                yearOnly.contains("seasonYear:"));

        String withSeason = AniListClient.buildCatalogFilters("2026", "SPRING", "", "", "");
        assertTrue(withSeason.contains("season: SPRING"));
        assertTrue(withSeason.contains("seasonYear: 2026"));

        String withMonth = AniListClient.buildCatalogFilters("2026", "", "4", "", "");
        assertTrue(withMonth.contains("startDate_greater: 20260401"));
        assertTrue(withMonth.contains("startDate_lesser: 20260431"));
    }
}
