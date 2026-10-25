package id.menatap.app.data.subtitle;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import id.menatap.app.data.service.HttpClientProvider;
import id.menatap.app.util.SecureStore;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * AiSubtitleService provides BYOK (Bring Your Own Key) LLM-powered subtitle translation
 * supporting Google Gemini, Groq, OpenAI-compatible APIs, and local/remote Ollama.
 *
 * Security guardrails:
 * - API keys are stored exclusively in hardware-backed SecureStore (AES-256-GCM).
 * - Zero logging: keys are never printed to logcat or console.
 */
public class AiSubtitleService {

    public static final String KEY_PREF_LLM = "byok_llm_api_key";
    public static final String KEY_PREF_ENDPOINT = "byok_llm_custom_endpoint";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    public enum Provider {
        GEMINI("Google Gemini", "AIzaSy..."),
        GROQ("Groq Cloud", "gsk_..."),
        OPENAI("OpenAI Compatible", "sk-..."),
        OLLAMA("Ollama Local/Remote", "http://...");

        public final String displayName;
        public final String placeholder;

        Provider(String displayName, String placeholder) {
            this.displayName = displayName;
            this.placeholder = placeholder;
        }

        public static Provider detect(String keyOrUrl) {
            if (keyOrUrl == null) return GEMINI;
            String trimmed = keyOrUrl.trim();
            if (trimmed.startsWith("AIzaSy")) return GEMINI;
            if (trimmed.startsWith("gsk_")) return GROQ;
            if (trimmed.startsWith("sk-") || trimmed.startsWith("sk-proj-")) return OPENAI;
            if (trimmed.startsWith("ollama") || trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                return OLLAMA;
            }
            return GEMINI;
        }
    }

    public interface Callback {
        void onSuccess(String translatedText);
        void onError(Throwable t);
    }

    private final OkHttpClient httpClient;
    private String customBaseUrl; // For testing and custom endpoints

    public AiSubtitleService() {
        this(new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build());
    }

    public AiSubtitleService(OkHttpClient client) {
        this.httpClient = client != null ? client : HttpClientProvider.getClient();
    }

    public void setCustomBaseUrl(String baseUrl) {
        this.customBaseUrl = baseUrl;
    }

    public static boolean saveKey(Context context, String key) {
        if (context == null || key == null) return false;
        return SecureStore.getInstance(context).putString(KEY_PREF_LLM, key.trim());
    }

    public static String getKey(Context context) {
        if (context == null) return "";
        return SecureStore.getInstance(context).getString(KEY_PREF_LLM, "");
    }

    public static boolean clearKey(Context context) {
        if (context == null) return false;
        return SecureStore.getInstance(context).remove(KEY_PREF_LLM);
    }

    public static Provider getActiveProvider(Context context) {
        String key = getKey(context);
        return Provider.detect(key);
    }

    public void translate(String text, String apiKey, Callback callback) {
        if (text == null || text.trim().isEmpty()) {
            callback.onSuccess("");
            return;
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Kunci API LLM belum dikonfigurasi"));
            return;
        }

        Provider provider = Provider.detect(apiKey);
        new Thread(() -> {
            try {
                String translated;
                switch (provider) {
                    case GROQ:
                        translated = callGroq(text, apiKey);
                        break;
                    case OPENAI:
                        translated = callOpenAi(text, apiKey);
                        break;
                    case OLLAMA:
                        translated = callOllama(text, apiKey);
                        break;
                    case GEMINI:
                    default:
                        translated = callGemini(text, apiKey);
                        break;
                }
                callback.onSuccess(translated != null ? translated.trim() : text);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public String callGemini(String text, String apiKey) throws Exception {
        String base = customBaseUrl != null ? customBaseUrl : "https://generativelanguage.googleapis.com";
        String url = base + "/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        JSONObject promptPart = new JSONObject();
        promptPart.put("text", "Translate this anime subtitle cue to natural Indonesian. Output ONLY the translated text:\n" + text);

        JSONArray parts = new JSONArray();
        parts.put(promptPart);

        JSONObject content = new JSONObject();
        content.put("parts", parts);

        JSONArray contents = new JSONArray();
        contents.put(content);

        JSONObject bodyObj = new JSONObject();
        bodyObj.put("contents", contents);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(bodyObj.toString(), JSON_MEDIA_TYPE))
                .build();

        try (Response resp = httpClient.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Gemini API error code: " + resp.code());
            }
            String respBody = resp.body() != null ? resp.body().string() : "";
            JSONObject json = new JSONObject(respBody);
            JSONArray candidates = json.optJSONArray("candidates");
            if (candidates != null && candidates.length() > 0) {
                JSONObject first = candidates.getJSONObject(0);
                JSONObject c = first.optJSONObject("content");
                if (c != null) {
                    JSONArray p = c.optJSONArray("parts");
                    if (p != null && p.length() > 0) {
                        return p.getJSONObject(0).optString("text", text);
                    }
                }
            }
            return text;
        }
    }

    public String callGroq(String text, String apiKey) throws Exception {
        String base = customBaseUrl != null ? customBaseUrl : "https://api.groq.com/openai";
        String url = base + "/v1/chat/completions";

        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", "You are an expert anime subtitle translator to Indonesian. Return ONLY the translated line.");

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", text);

        JSONArray messages = new JSONArray();
        messages.put(sysMsg);
        messages.put(userMsg);

        JSONObject bodyObj = new JSONObject();
        bodyObj.put("model", "llama-3.1-8b-instant");
        bodyObj.put("messages", messages);
        bodyObj.put("temperature", 0.3);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(bodyObj.toString(), JSON_MEDIA_TYPE))
                .build();

        try (Response resp = httpClient.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Groq API error code: " + resp.code());
            }
            String respBody = resp.body() != null ? resp.body().string() : "";
            JSONObject json = new JSONObject(respBody);
            JSONArray choices = json.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject msg = choices.getJSONObject(0).optJSONObject("message");
                if (msg != null) {
                    return msg.optString("content", text);
                }
            }
            return text;
        }
    }

    public String callOpenAi(String text, String apiKey) throws Exception {
        String base = customBaseUrl != null ? customBaseUrl : "https://api.openai.com";
        String url = base + "/v1/chat/completions";

        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", "You are an anime subtitle translator to Indonesian. Return ONLY the translation.");

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", text);

        JSONArray messages = new JSONArray();
        messages.put(sysMsg);
        messages.put(userMsg);

        JSONObject bodyObj = new JSONObject();
        bodyObj.put("model", "gpt-4o-mini");
        bodyObj.put("messages", messages);
        bodyObj.put("temperature", 0.3);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(bodyObj.toString(), JSON_MEDIA_TYPE))
                .build();

        try (Response resp = httpClient.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("OpenAI API error code: " + resp.code());
            }
            String respBody = resp.body() != null ? resp.body().string() : "";
            JSONObject json = new JSONObject(respBody);
            JSONArray choices = json.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject msg = choices.getJSONObject(0).optJSONObject("message");
                if (msg != null) {
                    return msg.optString("content", text);
                }
            }
            return text;
        }
    }

    public String callOllama(String text, String endpointOrKey) throws Exception {
        String endpoint = endpointOrKey;
        if (endpoint.startsWith("ollama:")) {
            endpoint = endpoint.substring("ollama:".length());
        }
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            endpoint = "http://" + endpoint;
        }
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        String base = customBaseUrl != null ? customBaseUrl : endpoint;
        String url = base + "/api/generate";

        JSONObject bodyObj = new JSONObject();
        bodyObj.put("model", "llama3");
        bodyObj.put("prompt", "Translate the following anime subtitle cue to Indonesian. Return ONLY the translated line:\n" + text);
        bodyObj.put("stream", false);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(bodyObj.toString(), JSON_MEDIA_TYPE))
                .build();

        try (Response resp = httpClient.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Ollama error code: " + resp.code());
            }
            String respBody = resp.body() != null ? resp.body().string() : "";
            JSONObject json = new JSONObject(respBody);
            return json.optString("response", text);
        }
    }
}
