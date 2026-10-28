package id.shisen.app.util;

import org.junit.Test;

import id.shisen.app.ui.detail.DetailActivity;
import id.shisen.app.ui.player.PlayerActivity;

import static org.junit.Assert.*;

public class DeviceCompatibilityTest {

    @Test
    public void testDialogWidthCalculationBounds() {
        // Phone 1080x2400, density 2.75 (~392dp width)
        int phoneWidth = 1080;
        float phoneDensity = 2.75f;
        int calculatedPhone = SystemUiHelper.calculateDialogWidth(phoneWidth, phoneDensity);
        assertEquals((int) (phoneWidth * 0.92f), calculatedPhone);

        // Tablet 2560x1600, density 2.0 (1280dp width)
        int tabletWidth = 2560;
        float tabletDensity = 2.0f;
        int calculatedTablet = SystemUiHelper.calculateDialogWidth(tabletWidth, tabletDensity);
        // Max should be capped at 520dp * 2.0 = 1040px
        assertEquals(1040, calculatedTablet);

        // Foldable unfolded: 1812px, density 2.625 (~690dp width)
        int foldWidth = 1812;
        float foldDensity = 2.625f;
        int calculatedFold = SystemUiHelper.calculateDialogWidth(foldWidth, foldDensity);
        // Desired: 1812 * 0.92 = 1667; Max: 520 * 2.625 = 1365 -> capped at 1365
        assertEquals((int) (520 * foldDensity), calculatedFold);
    }

    @Test
    public void testDialogWidthZeroOrNegativeDensityGraceful() {
        int width = 720;
        // density 0.0f or negative should default to safeDensity = 1.0f without crash
        int resZero = SystemUiHelper.calculateDialogWidth(width, 0.0f);
        assertTrue(resZero > 0);
        int resNeg = SystemUiHelper.calculateDialogWidth(width, -2.0f);
        assertTrue(resNeg > 0);
    }

    @Test
    public void testDetailEpisodeSpanCalculation() {
        // Small phone (320dp @ 2.0 density = 640px)
        assertEquals(3, DetailActivity.calculateEpisodeSpan(640, 2.0f));

        // Normal phone (392dp @ 2.75 density = 1080px)
        assertEquals(3, DetailActivity.calculateEpisodeSpan(1080, 2.75f));

        // Foldable (700dp @ 2.0 density = 1400px)
        assertEquals(4, DetailActivity.calculateEpisodeSpan(1400, 2.0f));

        // Large tablet (1000dp @ 2.0 density = 2000px)
        assertEquals(6, DetailActivity.calculateEpisodeSpan(2000, 2.0f));

        // Zero / negative density protection
        int spanZero = DetailActivity.calculateEpisodeSpan(1080, 0.0f);
        assertTrue(spanZero >= 3 && spanZero <= 6);
    }

    @Test
    public void testPlayerPortraitEpisodeSpanCalculation() {
        // Small phone (320dp @ 2.0 density = 640px)
        assertEquals(4, PlayerActivity.calculatePortraitEpisodeSpan(640, 2.0f));

        // Normal phone (392dp @ 2.75 density = 1080px)
        assertEquals(4, PlayerActivity.calculatePortraitEpisodeSpan(1080, 2.75f));

        // Foldable (700dp @ 2.0 density = 1400px)
        assertEquals(5, PlayerActivity.calculatePortraitEpisodeSpan(1400, 2.0f));

        // Large tablet (1000dp @ 2.0 density = 2000px)
        assertEquals(6, PlayerActivity.calculatePortraitEpisodeSpan(2000, 2.0f));

        // Zero / negative density protection
        int spanZero = PlayerActivity.calculatePortraitEpisodeSpan(1080, 0.0f);
        assertTrue(spanZero >= 4 && spanZero <= 6);
    }
}
