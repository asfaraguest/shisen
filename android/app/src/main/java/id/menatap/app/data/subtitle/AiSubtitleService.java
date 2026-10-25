package id.menatap.app.data.subtitle;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    public static final String KEY_PREF_CUSTOM_PROMPT = "byok_llm_custom_prompt";
    public static final String KEY_PREF_MODEL = "byok_llm_custom_model";

    public static final String DEFAULT_CUSTOM_PROMPT =
            "Terjemahkan subtitle anime ini ke Bahasa Indonesia yang alami, luwes, dan ekspresif selayaknya rilis fansub anime populer (bukan terjemahan kaku mesin). " +
            "Pertahankan istilah khas anime, nama jurus, dan honorifik Jepang (-san, -kun, -chan, -sama, Senpai, Sensei). " +
            "Keluarkan HANYA teks hasil terjemahan.";

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

    public static boolean saveCustomPrompt(Context context, String prompt) {
        if (context == null) return false;
        if (prompt == null || prompt.trim().isEmpty()) {
            return SecureStore.getInstance(context).remove(KEY_PREF_CUSTOM_PROMPT);
        }
        return SecureStore.getInstance(context).putString(KEY_PREF_CUSTOM_PROMPT, prompt.trim());
    }

    public static String getCustomPrompt(Context context) {
        if (context == null) return DEFAULT_CUSTOM_PROMPT;
        String prompt = SecureStore.getInstance(context).getString(KEY_PREF_CUSTOM_PROMPT, "");
        if (prompt == null || prompt.trim().isEmpty()) {
            return DEFAULT_CUSTOM_PROMPT;
        }
        return prompt.trim();
    }

    public static boolean clearCustomPrompt(Context context) {
        if (context == null) return false;
        return SecureStore.getInstance(context).remove(KEY_PREF_CUSTOM_PROMPT);
    }

    public static boolean saveModel(Context context, String model) {
        if (context == null) return false;
        if (model == null || model.trim().isEmpty()) {
            return SecureStore.getInstance(context).remove(KEY_PREF_MODEL);
        }
        return SecureStore.getInstance(context).putString(KEY_PREF_MODEL, model.trim());
    }

    public static String getModel(Context context) {
        if (context == null) return "";
        return SecureStore.getInstance(context).getString(KEY_PREF_MODEL, "");
    }

    public static boolean clearModel(Context context) {
        if (context == null) return false;
        return SecureStore.getInstance(context).remove(KEY_PREF_MODEL);
    }

    public static String getDefaultModel(Provider provider) {
        if (provider == null) return "gemini-2.5-flash";
        switch (provider) {
            case GROQ:
                return "llama-3.3-70b-versatile";
            case OPENAI:
                return "gpt-4o-mini";
            case OLLAMA:
                return "llama3.2";
            case GEMINI:
            default:
                return "gemini-2.5-flash";
        }
    }

    public static List<String> getAvailableModels(Provider provider) {
        List<String> list = new ArrayList<>();
        if (provider == null) return list;
        switch (provider) {
            case GEMINI:
                list.add("gemini-2.5-flash");
                list.add("gemini-2.0-flash");
                list.add("gemini-1.5-flash");
                list.add("gemini-1.5-pro");
                break;
            case GROQ:
                list.add("llama-3.3-70b-versatile");
                list.add("llama-3.1-8b-instant");
                list.add("mixtral-8x7b-32768");
                list.add("gemma2-9b-it");
                break;
            case OPENAI:
                list.add("gpt-4o-mini");
                list.add("gpt-4o");
                list.add("gpt-3.5-turbo");
                break;
            case OLLAMA:
                list.add("llama3.2");
                list.add("qwen2.5:7b");
                list.add("gemma2:9b");
                break;
        }
        return list;
    }

    public static String getEffectiveModel(Provider provider, String customModel) {
        if (customModel != null && !customModel.trim().isEmpty()) {
            return customModel.trim();
        }
        return getDefaultModel(provider);
    }

    public static String getEffectiveModel(Context context, Provider provider) {
        String saved = getModel(context);
        return getEffectiveModel(provider, saved);
    }

    public static Provider getActiveProvider(Context context) {
        String key = getKey(context);
        return Provider.detect(key);
    }

    public void translate(String text, String apiKey, Callback callback) {
        translate(text, apiKey, DEFAULT_CUSTOM_PROMPT, null, callback);
    }

    public void translate(String text, String apiKey, String customPrompt, Callback callback) {
        translate(text, apiKey, customPrompt, null, callback);
    }

    public void translate(String text, String apiKey, String customPrompt, String model, Callback callback) {
        if (text == null || text.trim().isEmpty()) {
            callback.onSuccess("");
            return;
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Kunci API LLM belum dikonfigurasi"));
            return;
        }

        final String activePrompt = (customPrompt != null && !customPrompt.trim().isEmpty())
                ? customPrompt.trim() : DEFAULT_CUSTOM_PROMPT;

        Provider provider = Provider.detect(apiKey);
        final String activeModel = (model != null && !model.trim().isEmpty())
                ? model.trim() : getDefaultModel(provider);

        new Thread(() -> {
            try {
                String translated;
                switch (provider) {
                    case GROQ:
                        translated = callGroq(text, apiKey, activePrompt, activeModel);
                        break;
                    case OPENAI:
                        translated = callOpenAi(text, apiKey, activePrompt, activeModel);
                        break;
                    case OLLAMA:
                        translated = callOllama(text, apiKey, activePrompt, activeModel);
                        break;
                    case GEMINI:
                    default:
                        translated = callGemini(text, apiKey, activePrompt, activeModel);
                        break;
                }
                callback.onSuccess(translated != null ? translated.trim() : text);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public String callGemini(String text, String apiKey) throws Exception {
        return callGemini(text, apiKey, DEFAULT_CUSTOM_PROMPT, "gemini-2.5-flash");
    }

    public String callGemini(String text, String apiKey, String customPrompt) throws Exception {
        return callGemini(text, apiKey, customPrompt, "gemini-2.5-flash");
    }

    public String callGemini(String text, String apiKey, String customPrompt, String model) throws Exception {
        String base = customBaseUrl != null ? customBaseUrl : "https://generativelanguage.googleapis.com";
        String effectiveModel = (model != null && !model.trim().isEmpty()) ? model.trim() : "gemini-2.5-flash";
        String url = base + "/v1beta/models/" + effectiveModel + ":generateContent?key=" + apiKey;

        String instruction = (customPrompt != null && !customPrompt.trim().isEmpty())
                ? customPrompt.trim() : DEFAULT_CUSTOM_PROMPT;

        JSONObject promptPart = new JSONObject();
        promptPart.put("text", instruction + "\n\n" + text);

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
        return callGroq(text, apiKey, DEFAULT_CUSTOM_PROMPT, "llama-3.3-70b-versatile");
    }

    public String callGroq(String text, String apiKey, String customPrompt) throws Exception {
        return callGroq(text, apiKey, customPrompt, "llama-3.3-70b-versatile");
    }

    public String callGroq(String text, String apiKey, String customPrompt, String model) throws Exception {
        String base = customBaseUrl != null ? customBaseUrl : "https://api.groq.com/openai";
        String url = base + "/v1/chat/completions";

        String instruction = (customPrompt != null && !customPrompt.trim().isEmpty())
                ? customPrompt.trim() : DEFAULT_CUSTOM_PROMPT;

        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", instruction);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", text);

        JSONArray messages = new JSONArray();
        messages.put(sysMsg);
        messages.put(userMsg);

        String effectiveModel = (model != null && !model.trim().isEmpty()) ? model.trim() : "llama-3.3-70b-versatile";
        JSONObject bodyObj = new JSONObject();
        bodyObj.put("model", effectiveModel);
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
        return callOpenAi(text, apiKey, DEFAULT_CUSTOM_PROMPT, "gpt-4o-mini");
    }

    public String callOpenAi(String text, String apiKey, String customPrompt) throws Exception {
        return callOpenAi(text, apiKey, customPrompt, "gpt-4o-mini");
    }

    public String callOpenAi(String text, String apiKey, String customPrompt, String model) throws Exception {
        String base = customBaseUrl != null ? customBaseUrl : "https://api.openai.com";
        String url = base + "/v1/chat/completions";

        String instruction = (customPrompt != null && !customPrompt.trim().isEmpty())
                ? customPrompt.trim() : DEFAULT_CUSTOM_PROMPT;

        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", instruction);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", text);

        JSONArray messages = new JSONArray();
        messages.put(sysMsg);
        messages.put(userMsg);

        String effectiveModel = (model != null && !model.trim().isEmpty()) ? model.trim() : "gpt-4o-mini";
        JSONObject bodyObj = new JSONObject();
        bodyObj.put("model", effectiveModel);
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
        return callOllama(text, endpointOrKey, DEFAULT_CUSTOM_PROMPT, "llama3.2");
    }

    public String callOllama(String text, String endpointOrKey, String customPrompt) throws Exception {
        return callOllama(text, endpointOrKey, customPrompt, "llama3.2");
    }

    public String callOllama(String text, String endpointOrKey, String customPrompt, String model) throws Exception {
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

        String instruction = (customPrompt != null && !customPrompt.trim().isEmpty())
                ? customPrompt.trim() : DEFAULT_CUSTOM_PROMPT;

        String effectiveModel = (model != null && !model.trim().isEmpty()) ? model.trim() : "llama3.2";
        JSONObject bodyObj = new JSONObject();
        bodyObj.put("model", effectiveModel);
        bodyObj.put("prompt", instruction + "\n\n" + text);
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
