package id.shisen.app.util;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.*;

public class LocaleHelperTest {

    @Test
    public void testLanguageConstants() {
        assertEquals("in", LocaleHelper.LANG_ID);
        assertEquals("en", LocaleHelper.LANG_EN);
        assertEquals("ja", LocaleHelper.LANG_JA);
    }

    @Test
    public void testDefaultLanguageWithoutContext() {
        assertEquals(LocaleHelper.LANG_ID, LocaleHelper.getLanguage(null));
    }

    @Test
    public void testSetLocaleNullContextSafe() {
        assertNull(LocaleHelper.setLocale(null, LocaleHelper.LANG_JA));
    }
}
