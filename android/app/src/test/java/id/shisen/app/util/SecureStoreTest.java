package id.shisen.app.util;

import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class SecureStoreTest {

    private SecureStore secureStore;
    private FakeSharedPreferences fakePrefs;

    @Before
    public void setUp() {
        fakePrefs = new FakeSharedPreferences();
        secureStore = SecureStore.createForTesting(fakePrefs);
    }

    @Test
    public void testEncryptDecryptRoundTrip() throws Exception {
        String originalKey = "sk-proj-gemini-api-key-abc123xyz789";
        String encrypted = secureStore.encrypt(originalKey);

        assertNotNull(encrypted);
        assertNotEquals(originalKey, encrypted);

        String decrypted = secureStore.decrypt(encrypted);
        assertEquals(originalKey, decrypted);
    }

    @Test
    public void testRandomIvGeneratesDistinctCiphertext() throws Exception {
        String secret = "gsk_groq_super_secret_token_12345";
        String cipher1 = secureStore.encrypt(secret);
        String cipher2 = secureStore.encrypt(secret);

        assertNotNull(cipher1);
        assertNotNull(cipher2);
        // Random 12-byte IV must produce different output for identical plaintext
        assertNotEquals(cipher1, cipher2);

        // Both decrypt back to identical plaintext
        assertEquals(secret, secureStore.decrypt(cipher1));
        assertEquals(secret, secureStore.decrypt(cipher2));
    }

    @Test
    public void testTamperedCiphertextThrowsAuthenticationError() throws Exception {
        String secret = "confidential-llm-token";
        String cipher = secureStore.encrypt(secret);

        byte[] raw = java.util.Base64.getDecoder().decode(cipher);
        // Tamper with one byte in the ciphertext body
        raw[raw.length - 1] ^= 0xFF;
        String tamperedCipher = java.util.Base64.getEncoder().encodeToString(raw);

        try {
            secureStore.decrypt(tamperedCipher);
            fail("GCM AEAD authentication should reject tampered ciphertext");
        } catch (Exception expected) {
            // Success: GCM authenticated tag mismatch caught
            assertTrue(expected.getMessage() != null || expected instanceof javax.crypto.AEADBadTagException);
        }
    }

    @Test
    public void testUnicodeAndComplexStrings() throws Exception {
        String complex = "Tatap & Menatap 氷菓 - Overgeared (Satisfy) 🚀 \u0000 \t \n special chars: !@#$%^&*()_+";
        String encrypted = secureStore.encrypt(complex);
        String decrypted = secureStore.decrypt(encrypted);
        assertEquals(complex, decrypted);
    }

    @Test
    public void testNullAndEmptyInputs() throws Exception {
        assertNull(secureStore.encrypt(null));
        assertNull(secureStore.decrypt(null));
        assertNull(secureStore.decrypt(""));

        try {
            // Payload shorter than 12 bytes IV
            secureStore.decrypt("AAAA");
            fail("Short payload should throw IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Expected
        }
    }

    @Test
    public void testSharedPreferencesPersistence() {
        String key = "byok_gemini_key";
        String secret = "AIzaSySecretApiKey2026";

        assertTrue(secureStore.putString(key, secret));
        assertTrue(secureStore.contains(key));

        // Ensure raw stored value in preferences is encrypted and does NOT leak secret
        String storedRaw = fakePrefs.getString(key, null);
        assertNotNull(storedRaw);
        assertNotEquals(secret, storedRaw);
        assertFalse(storedRaw.contains(secret));

        // Retrieval through SecureStore decrypts correctly
        String retrieved = secureStore.getString(key, null);
        assertEquals(secret, retrieved);

        // Remove
        assertTrue(secureStore.remove(key));
        assertFalse(secureStore.contains(key));
        assertNull(secureStore.getString(key, null));
    }

    @Test
    public void testCorruptedPreferenceReturnsDefaultValue() {
        String key = "corrupted_key";
        fakePrefs.data.put(key, "not_a_valid_gcm_ciphertext_payload");

        String result = secureStore.getString(key, "fallback_default");
        assertEquals("fallback_default", result);
    }

    /**
     * In-memory FakeSharedPreferences for host JVM tests.
     */
    private static class FakeSharedPreferences implements SharedPreferences {
        final Map<String, Object> data = new HashMap<>();

        @Override
        public Map<String, ?> getAll() { return new HashMap<>(data); }

        @Override
        public String getString(String key, String defValue) {
            Object v = data.get(key);
            return v instanceof String ? (String) v : defValue;
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) { return null; }

        @Override
        public int getInt(String key, int defValue) { return defValue; }

        @Override
        public long getLong(String key, long defValue) { return defValue; }

        @Override
        public float getFloat(String key, float defValue) { return defValue; }

        @Override
        public boolean getBoolean(String key, boolean defValue) { return defValue; }

        @Override
        public boolean contains(String key) { return data.containsKey(key); }

        @Override
        public Editor edit() { return new FakeEditor(this); }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        private static class FakeEditor implements Editor {
            private final FakeSharedPreferences parent;
            private final Map<String, Object> temp = new HashMap<>();
            private boolean clearRequested = false;

            FakeEditor(FakeSharedPreferences parent) {
                this.parent = parent;
                this.temp.putAll(parent.data);
            }

            @Override
            public Editor putString(String key, String value) {
                temp.put(key, value);
                return this;
            }

            @Override
            public Editor putStringSet(String key, Set<String> values) { return this; }

            @Override
            public Editor putInt(String key, int value) { return this; }

            @Override
            public Editor putLong(String key, long value) { return this; }

            @Override
            public Editor putFloat(String key, float value) { return this; }

            @Override
            public Editor putBoolean(String key, boolean value) { return this; }

            @Override
            public Editor remove(String key) {
                temp.remove(key);
                return this;
            }

            @Override
            public Editor clear() {
                clearRequested = true;
                temp.clear();
                return this;
            }

            @Override
            public boolean commit() {
                apply();
                return true;
            }

            @Override
            public void apply() {
                if (clearRequested) {
                    parent.data.clear();
                }
                parent.data.clear();
                parent.data.putAll(temp);
            }
        }
    }
}
