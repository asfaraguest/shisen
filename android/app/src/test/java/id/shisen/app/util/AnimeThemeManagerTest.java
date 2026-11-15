package id.shisen.app.util;

import org.junit.Test;

import id.shisen.app.R;

import static org.junit.Assert.*;

public class AnimeThemeManagerTest {

    @Test
    public void testThemeConstants() {
        assertEquals("shinkai", AnimeThemeManager.THEME_SHINKAI);
        assertEquals("aincrad", AnimeThemeManager.THEME_AINCRAD);
        assertEquals("sakura", AnimeThemeManager.THEME_SAKURA);
        assertEquals("cyber", AnimeThemeManager.THEME_CYBER);
        assertEquals("ghibli", AnimeThemeManager.THEME_GHIBLI);
    }

    @Test
    public void testCanvasBackgroundMapping() {
        assertEquals(R.drawable.bg_anime_canvas_shinkai,
                AnimeThemeManager.getCanvasBackgroundResIdForTheme(AnimeThemeManager.THEME_SHINKAI));
        assertEquals(R.drawable.bg_anime_canvas_shinkai,
                AnimeThemeManager.getCanvasBackgroundResIdForTheme(AnimeThemeManager.THEME_AINCRAD));
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

    @Test
    public void testPaletteGettersPerTheme() {
        assertEquals(0xFF38BDF8, AnimeThemeManager.getPrimaryAccentColorForTheme(AnimeThemeManager.THEME_SHINKAI));
        assertEquals(0xFFF59E0B, AnimeThemeManager.getPrimaryAccentColorForTheme(AnimeThemeManager.THEME_AINCRAD));
        assertEquals(0xFFFB7185, AnimeThemeManager.getPrimaryAccentColorForTheme(AnimeThemeManager.THEME_SAKURA));
        assertEquals(0xFF00F0FF, AnimeThemeManager.getPrimaryAccentColorForTheme(AnimeThemeManager.THEME_CYBER));
        assertEquals(0xFF10B981, AnimeThemeManager.getPrimaryAccentColorForTheme(AnimeThemeManager.THEME_GHIBLI));
        assertEquals(0xFF38BDF8, AnimeThemeManager.getPrimaryAccentColorForTheme(null));

        assertEquals(0xFF818CF8, AnimeThemeManager.getSecondaryAccentColorForTheme(AnimeThemeManager.THEME_SHINKAI));
        assertEquals(0xFFFBBF24, AnimeThemeManager.getSecondaryAccentColorForTheme(AnimeThemeManager.THEME_AINCRAD));
        assertEquals(0xFFFDA4AF, AnimeThemeManager.getSecondaryAccentColorForTheme(AnimeThemeManager.THEME_SAKURA));
        assertEquals(0xFFA855F7, AnimeThemeManager.getSecondaryAccentColorForTheme(AnimeThemeManager.THEME_CYBER));
        assertEquals(0xFF34D399, AnimeThemeManager.getSecondaryAccentColorForTheme(AnimeThemeManager.THEME_GHIBLI));

        if (AnimeThemeManager.isMobileSaoEnabled()) {
            assertEquals(0xFF191C22, AnimeThemeManager.getSurfaceColorForTheme(AnimeThemeManager.THEME_SHINKAI));
            assertEquals(0xFF191C22, AnimeThemeManager.getSurfaceColorForTheme(AnimeThemeManager.THEME_AINCRAD));
            assertEquals(0xFF2E3440, AnimeThemeManager.getBorderColorForTheme(AnimeThemeManager.THEME_SHINKAI));
            assertEquals(0xFF111317, AnimeThemeManager.getVoidColorForTheme(AnimeThemeManager.THEME_SHINKAI));
        } else {
            assertEquals(0xFF10162A, AnimeThemeManager.getSurfaceColorForTheme(AnimeThemeManager.THEME_SHINKAI));
            assertEquals(0xFF1C1226, AnimeThemeManager.getSurfaceColorForTheme(AnimeThemeManager.THEME_SAKURA));
            assertEquals(0xFF0E1424, AnimeThemeManager.getSurfaceColorForTheme(AnimeThemeManager.THEME_CYBER));
            assertEquals(0xFF0E211E, AnimeThemeManager.getSurfaceColorForTheme(AnimeThemeManager.THEME_GHIBLI));

            assertEquals(0xFF233252, AnimeThemeManager.getBorderColorForTheme(AnimeThemeManager.THEME_SHINKAI));
            assertEquals(0xFF381D47, AnimeThemeManager.getBorderColorForTheme(AnimeThemeManager.THEME_SAKURA));
            assertEquals(0xFF1E2C4A, AnimeThemeManager.getBorderColorForTheme(AnimeThemeManager.THEME_CYBER));
            assertEquals(0xFF1B3D37, AnimeThemeManager.getBorderColorForTheme(AnimeThemeManager.THEME_GHIBLI));

            assertEquals(0xFF0A0D18, AnimeThemeManager.getVoidColorForTheme(AnimeThemeManager.THEME_SHINKAI));
            assertEquals(0xFF0F0815, AnimeThemeManager.getVoidColorForTheme(AnimeThemeManager.THEME_SAKURA));
            assertEquals(0xFF060810, AnimeThemeManager.getVoidColorForTheme(AnimeThemeManager.THEME_CYBER));
            assertEquals(0xFF06100E, AnimeThemeManager.getVoidColorForTheme(AnimeThemeManager.THEME_GHIBLI));
        }
    }

    @Test
    public void testSaoPaletteAndBlendArgb() {
        assertEquals(0xFF111317, AnimeThemeManager.getSaoVoidColor());
        assertEquals(0xFF191C22, AnimeThemeManager.getSaoSurfaceColor());
        assertEquals(0xFF21252D, AnimeThemeManager.getSaoElevatedColor());
        assertEquals(0xFF2E3440, AnimeThemeManager.getSaoHairlineColor());
        assertEquals(0xFF000000, AnimeThemeManager.blendArgb(0xFF000000, 0xFFFFFFFF, 0f));
        assertEquals(0xFFFFFFFF, AnimeThemeManager.blendArgb(0xFF000000, 0xFFFFFFFF, 1f));
        assertEquals(0xFF808080, AnimeThemeManager.blendArgb(0xFF000000, 0xFFFFFFFF, 0.5f));
    }

    @Test
    public void testWithAlphaClampingAndComposition() {
        assertEquals(0x80FB7185, AnimeThemeManager.withAlpha(0xFFFB7185, 0x80));
        assertEquals(0x00FB7185, AnimeThemeManager.withAlpha(0xFFFB7185, -20));
        assertEquals(0xFFFB7185, AnimeThemeManager.withAlpha(0xFFFB7185, 400));
    }
}
