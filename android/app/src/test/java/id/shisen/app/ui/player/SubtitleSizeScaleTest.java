package id.shisen.app.ui.player;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SubtitleSizeScaleTest {

    @Test
    public void testTvSubtitleSizeScaleMatchesTatapTvUpToMax42Sp() {
        assertEquals("large", PlayerActivity.getDefaultSubtitleSizeKey(true));
        assertEquals(18f, PlayerActivity.resolveSubtitleSizeSp("small", true), 0.01f);
        assertEquals(22f, PlayerActivity.resolveSubtitleSizeSp("medium", true), 0.01f);
        assertEquals(26f, PlayerActivity.resolveSubtitleSizeSp("large", true), 0.01f);
        assertEquals(30f, PlayerActivity.resolveSubtitleSizeSp("xlarge", true), 0.01f);
        assertEquals(34f, PlayerActivity.resolveSubtitleSizeSp("xxlarge", true), 0.01f);
        assertEquals(38f, PlayerActivity.resolveSubtitleSizeSp("jumbo", true), 0.01f);
        assertEquals(42f, PlayerActivity.resolveSubtitleSizeSp("max", true), 0.01f);
        assertEquals(26f, PlayerActivity.resolveSubtitleSizeSp("unknown", true), 0.01f);
    }

    @Test
    public void testMobileSubtitleSizeScaleRemainsUnchanged() {
        assertEquals("medium", PlayerActivity.getDefaultSubtitleSizeKey(false));
        assertEquals(14f, PlayerActivity.resolveSubtitleSizeSp("small", false), 0.01f);
        assertEquals(17f, PlayerActivity.resolveSubtitleSizeSp("medium", false), 0.01f);
        assertEquals(21f, PlayerActivity.resolveSubtitleSizeSp("large", false), 0.01f);
        assertEquals(25f, PlayerActivity.resolveSubtitleSizeSp("xlarge", false), 0.01f);
        assertEquals(17f, PlayerActivity.resolveSubtitleSizeSp("max", false), 0.01f);
    }
}
