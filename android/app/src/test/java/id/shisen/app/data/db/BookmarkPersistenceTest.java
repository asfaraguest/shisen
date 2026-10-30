package id.shisen.app.data.db;

import org.junit.Test;

import id.shisen.app.data.model.Anime;

import static org.junit.Assert.*;

public class BookmarkPersistenceTest {

    @Test
    public void testAnimeModelIds() {
        Anime anime = new Anime();
        anime.id = 52991;
        anime.anilistId = 0; // Kasus dari AniList di mana anilistId belum diset tapi id terisi
        anime.idMal = 52991;
        anime.title = "Frieren";
        anime.slug = "frieren-beyond-journeys-end";

        int aid = anime.anilistId > 0 ? anime.anilistId : anime.id;
        assertEquals(52991, aid);
        assertEquals(52991, anime.idMal);
    }

    @Test
    public void testByteArrayStreamFullRead() throws Exception {
        byte[] original = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Subtitle content test.".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(original);
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();

        byte[] buf = new byte[16]; // Small chunk buffer to test loop
        int read;
        while ((read = bais.read(buf)) != -1) {
            bos.write(buf, 0, read);
        }

        String result = bos.toString("UTF-8");
        assertEquals(new String(original, java.nio.charset.StandardCharsets.UTF_8), result);
    }
}
