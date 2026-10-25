package id.menatap.app.util;

import org.junit.Test;

import id.menatap.app.R;

import static org.junit.Assert.*;

public class AnimeThemeManagerTest {

    @Test
    public void testThemeConstants() {
        assertEquals("shinkai", AnimeThemeManager.THEME_SHINKAI);
        assertEquals("sakura", AnimeThemeManager.THEME_SAKURA);
        assertEquals("cyber", AnimeThemeManager.THEME_CYBER);
        assertEquals("ghibli", AnimeThemeManager.THEME_GHIBLI);
    }

    @Test
    public void testCanvasBackgroundMapping() {
        assertEquals(R.drawable.bg_anime_canvas_shinkai,
                AnimeThemeManager.getCanvasBackgroundResIdForTheme(AnimeThemeManager.THEME_SHINKAI));
        assertEquals(R.drawable.bg_anime_canvas_sakura,
                AnimeThemeManager.getCanvasBackgroundResIdForTheme(AnimeThemeManager.THEME_SAKURA));
        assertEquals(R.drawable.bg_anime_canvas_cyber,
                AnimeThemeManager.getCanvasBackgroundResIdForTheme(AnimeThemeManager.THEME_CYBER));
        assertEquals(R.drawable.bg_anime_canvas_ghibli,
                AnimeThemeManager.getCanvasBackgroundResIdForTheme(AnimeThemeManager.THEME_GHIBLI));
        // Fallback for null or unknown theme key
        assertEquals(R.drawable.bg_anime_canvas_shinkai,
                AnimeThemeManager.getCanvasBackgroundResIdForTheme(null));
        assertEquals(R.drawable.bg_anime_canvas_shinkai,
                AnimeThemeManager.getCanvasBackgroundResIdForTheme("unknown_custom"));
    }

    @Test
    public void testDefaultThemeWithoutContext() {
        assertEquals(AnimeThemeManager.THEME_SHINKAI, AnimeThemeManager.getTheme(null));
    }
}
