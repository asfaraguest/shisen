package id.shisen.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;

/**
 * SecureStore provides hardware-backed, encrypted key-value persistence
 * using the Android Keystore system with AES-256-GCM authenticated encryption.
 *
 * Prepared for Phase 2 BYOK LLM subtitle keys (Gemini, Groq, Ollama, OpenAI).
 * Strictly forbids logging of secrets and API keys.
 * Hardcoded seeds and raw plaintext fallbacks are strictly prohibited in production.
 */
public class SecureStore {

    private static final String PREFS_NAME = "menatap_secure_store";
    private static final String KEY_ALIAS = "menatap_keystore_alias_v1";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    public interface LegacyFallbackDecryptor {
        String decryptFallback(String base64Combined) throws Exception;
    }

    private static volatile LegacyFallbackDecryptor legacyFallbackDecryptor;

    public static void setLegacyFallbackDecryptorForTesting(LegacyFallbackDecryptor decryptor) {
        legacyFallbackDecryptor = decryptor;
    }

    private static volatile SecureStore instance;
    private final SharedPreferences prefs;
    private final Key secretKey;
    private final SecureRandom secureRandom;

    private SecureStore(Context context) {
        this(context != null && context.getApplicationContext() != null
                ? context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                : (context != null ? context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) : null));
    }

    SecureStore(SharedPreferences prefs) {
        this(prefs, initOrGetSecretKey());
    }

    SecureStore(SharedPreferences prefs, Key secretKey) {
        this.prefs = prefs;
        this.secureRandom = new SecureRandom();
        this.secretKey = secretKey;
    }

    public static SecureStore getInstance(Context context) {
        if (instance == null) {
            synchronized (SecureStore.class) {
                if (instance == null) {
                    instance = new SecureStore(context);
                }
            }
        }
        return instance;
    }

    /**
     * Testing factory for isolated unit test environments without Android Context.
     * Generates an ephemeral in-memory AES key if Android Keystore is unavailable.
     */
    public static SecureStore createForTesting(SharedPreferences prefs) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            return new SecureStore(prefs, keyGen.generateKey());
        } catch (Throwable t) {
            return new SecureStore(prefs, null);
        }
    }

    public static SecureStore createForTesting(SharedPreferences prefs, Key key) {
        return new SecureStore(prefs, key);
    }

    /**
     * Initializes or retrieves an AES-256 key from Android Keystore.
     * Returns null if AndroidKeyStore provider is unavailable (e.g. Host JVM or broken keystore).
     */
    private static Key initOrGetSecretKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyGenerator keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
                KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .setRandomizedEncryptionRequired(true)
                        .build();
                keyGen.init(spec);
                return keyGen.generateKey();
            }
            return keyStore.getKey(KEY_ALIAS, null);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Encrypts plaintext string using AES-256-GCM.
     * Output format: [12-byte IV][Ciphertext + 16-byte Tag] encoded as Base64.
     */
    public synchronized String encrypt(String plainText) throws Exception {
        if (plainText == null) return null;
        if (secretKey == null) {
            throw new IllegalStateException("Hardware Keystore is unavailable");
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        byte[] iv;
        try {
            // AndroidKeyStore requires cipher.init without IV spec when randomizedEncryption is enabled
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            iv = cipher.getIV();
            if (iv == null || iv.length == 0) {
                throw new IllegalStateException("Cipher did not generate IV");
            }
        } catch (Throwable t) {
            // JVM Unit Test Fallback or standard JCE providers
            iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
        }

        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

        return encodeBase64(combined);
    }

    /**
     * Decrypts Base64 payload containing IV and ciphertext.
     */
    public synchronized String decrypt(String base64Combined) throws Exception {
        if (base64Combined == null || base64Combined.trim().isEmpty()) return null;
        if (secretKey == null) {
            throw new IllegalStateException("Hardware Keystore is unavailable");
        }
        byte[] combined = decodeBase64(base64Combined);
        if (combined.length <= GCM_IV_LENGTH_BYTES) {
            throw new IllegalArgumentException("Invalid payload length");
        }

        byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH_BYTES);
        byte[] cipherText = Arrays.copyOfRange(combined, GCM_IV_LENGTH_BYTES, combined.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        byte[] plainBytes = cipher.doFinal(cipherText);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    private static String encodeBase64(byte[] data) {
        try {
            return Base64.encodeToString(data, Base64.NO_WRAP);
        } catch (Throwable t) {
            return java.util.Base64.getEncoder().encodeToString(data);
        }
    }

    private static byte[] decodeBase64(String str) {
        try {
            return Base64.decode(str, Base64.NO_WRAP);
        } catch (Throwable t) {
            return java.util.Base64.getDecoder().decode(str);
        }
    }

    /**
     * Stores secret securely in encrypted preferences.
     * Returns false if Keystore is unavailable or encryption fails (never stores plaintext or hardcoded key).
     */
    public boolean putString(String key, String value) {
        if (key == null || prefs == null) return false;
        if (value == null) {
            return prefs.edit().remove(key).commit();
        }
        if (secretKey == null) {
            return false;
        }
        try {
            String encrypted = encrypt(value);
            return prefs.edit().putString(key, encrypted).commit();
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Retrieves and decrypts secret from storage. Returns defaultValue if missing or corrupted.
     * Performs one-way migration if legacy "raw:" or "fb:" prefix is detected.
     */
    public String getString(String key, String defaultValue) {
        if (key == null || prefs == null) return defaultValue;
        String raw = prefs.getString(key, null);
        if (raw == null) return defaultValue;

        // 1. One-way migration for legacy plaintext prefix "raw:"
        if (raw.startsWith("raw:")) {
            String plain = raw.substring(4);
            if (putString(key, plain)) {
                return plain;
            } else {
                remove(key);
                return defaultValue;
            }
        }

        // 2. One-way migration for legacy software fallback prefix "fb:"
        if (raw.startsWith("fb:")) {
            String payload = raw.substring(3);
            String plain = null;
            if (legacyFallbackDecryptor != null) {
                try {
                    plain = legacyFallbackDecryptor.decryptFallback(payload);
                } catch (Throwable ignored) {}
            }
            if (plain != null && putString(key, plain)) {
                return plain;
            } else {
                remove(key);
                return defaultValue;
            }
        }

        // 3. Normal Keystore decryption
        try {
            String decrypted = decrypt(raw);
            return (decrypted != null) ? decrypted : defaultValue;
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    public boolean contains(String key) {
        return prefs.contains(key);
    }

    public boolean remove(String key) {
        return prefs.edit().remove(key).commit();
    }

    public boolean clear() {
        return prefs.edit().clear().commit();
    }
}
