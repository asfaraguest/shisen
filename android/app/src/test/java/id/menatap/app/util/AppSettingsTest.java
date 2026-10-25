package id.menatap.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import id.menatap.app.data.model.Anime;

public class AppSettingsTest {

    @Test
    public void testSafeAnimeFiltering() {
        Anime normalAnime = new Anime();
        normalAnime.id = 1;
        normalAnime.title = "Sousou no Frieren";
        normalAnime.slug = "sousou-no-frieren";
        normalAnime.genres = Arrays.asList("Adventure", "Drama", "Fantasy");

        Anime ecchiAnime = new Anime();
        ecchiAnime.id = 2;
        ecchiAnime.title = "Highschool DxD";
        ecchiAnime.slug = "highschool-dxd";
        ecchiAnime.genres = Arrays.asList("Action", "Comedy", "Ecchi");

        Anime eroticaAnime = new Anime();
        eroticaAnime.id = 3;
        eroticaAnime.title = "Erotica Master";
        eroticaAnime.slug = "erotica-master";
        eroticaAnime.genres = Arrays.asList("Romance", "Erotica");

        Anime hentaiAnime = new Anime();
        hentaiAnime.id = 4;
        hentaiAnime.title = "Forbidden Episode";
        hentaiAnime.slug = "forbidden-episode";
        hentaiAnime.genres = Arrays.asList("Romance", "Hentai");

        Anime hentaiTitleAnime = new Anime();
        hentaiTitleAnime.id = 5;
        hentaiTitleAnime.title = "Pure Hentai Series";
        hentaiTitleAnime.slug = "pure-hentai-series";
        hentaiTitleAnime.genres = Arrays.asList("Comedy");

        // 1. With Safe Mode ON (Default)
        boolean safeModeOn = true;
        assertTrue(AppSettings.isSafeAnime(normalAnime, safeModeOn));
        assertFalse("Ecchi anime must be filtered when Safe Mode is ON", AppSettings.isSafeAnime(ecchiAnime, safeModeOn));
        assertFalse("Erotica anime must be filtered when Safe Mode is ON", AppSettings.isSafeAnime(eroticaAnime, safeModeOn));
        assertFalse("Hentai genre must ALWAYS be blocked", AppSettings.isSafeAnime(hentaiAnime, safeModeOn));
        assertFalse("Hentai title must ALWAYS be blocked", AppSettings.isSafeAnime(hentaiTitleAnime, safeModeOn));

        // 2. With Safe Mode OFF
        boolean safeModeOff = false;
        assertTrue(AppSettings.isSafeAnime(normalAnime, safeModeOff));
        assertTrue("Ecchi anime allowed when Safe Mode is OFF", AppSettings.isSafeAnime(ecchiAnime, safeModeOff));
        assertTrue("Erotica anime allowed when Safe Mode is OFF", AppSettings.isSafeAnime(eroticaAnime, safeModeOff));
        assertFalse("Hentai genre must STILL be blocked when Safe Mode is OFF", AppSettings.isSafeAnime(hentaiAnime, safeModeOff));
        assertFalse("Hentai title must STILL be blocked when Safe Mode is OFF", AppSettings.isSafeAnime(hentaiTitleAnime, safeModeOff));
    }

    @Test
    public void testSafeQueryValidation() {
        // Safe Mode ON
        boolean safeModeOn = true;
        assertTrue(AppSettings.isSafeQuery("Naruto Shippuden", safeModeOn));
        assertTrue(AppSettings.isSafeQuery("Sousou no Frieren", safeModeOn));
        assertFalse(AppSettings.isSafeQuery("highschool dxd ecchi", safeModeOn));
        assertFalse(AppSettings.isSafeQuery("erotica romance", safeModeOn));
        assertFalse(AppSettings.isSafeQuery("hentai uncensored", safeModeOn));

        // Safe Mode OFF
        boolean safeModeOff = false;
        assertTrue(AppSettings.isSafeQuery("Naruto Shippuden", safeModeOff));
        assertTrue(AppSettings.isSafeQuery("highschool dxd ecchi", safeModeOff));
        assertTrue(AppSettings.isSafeQuery("erotica romance", safeModeOff));
        // Hentai is unconditionally blocked even if Safe Mode is OFF
        assertFalse(AppSettings.isSafeQuery("hentai uncensored", safeModeOff));
    }

    @Test
    public void testFilterSafeList() {
        Anime normal1 = new Anime();
        normal1.id = 1;
        normal1.title = "Anime Satu";
        normal1.genres = Arrays.asList("Action");

        Anime ecchi1 = new Anime();
        ecchi1.id = 2;
        ecchi1.title = "Ecchi Spesial";
        ecchi1.genres = Arrays.asList("Ecchi");

        Anime hentai1 = new Anime();
        hentai1.id = 3;
        hentai1.title = "Hentai OVA";
        hentai1.genres = Arrays.asList("Hentai");

        List<Anime> list = Arrays.asList(normal1, ecchi1, hentai1);

        // Safe Mode ON: only normal1 passes
        List<Anime> filteredOn = AppSettings.filterSafe(list, true);
        assertEquals(1, filteredOn.size());
        assertEquals("Anime Satu", filteredOn.get(0).title);

        // Safe Mode OFF: normal1 and ecchi1 pass, hentai1 is NEVER included
        List<Anime> filteredOff = AppSettings.filterSafe(list, false);
        assertEquals(2, filteredOff.size());
        assertEquals("Anime Satu", filteredOff.get(0).title);
        assertEquals("Ecchi Spesial", filteredOff.get(1).title);
    }
}
