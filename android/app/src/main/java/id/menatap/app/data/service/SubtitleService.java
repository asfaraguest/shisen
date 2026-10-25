package id.menatap.app.data.service;

import org.json.JSONArray;

import java.io.IOException;
import java.net.URLEncoder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SubtitleService {
    public interface Callback {
        void onSuccess(String translatedText);
        void onError(Throwable t);
    }

    public static void translateText(String text, String targetLang, Callback callback) {
        if (text == null || text.trim().isEmpty()) {
            callback.onSuccess("");
            return;
        }

        new Thread(() -> {
            try {
                String encoded = URLEncoder.encode(text, "UTF-8");
                String url = "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&sl=auto&tl=" + targetLang + "&q=" + encoded;
                OkHttpClient client = HttpClientProvider.getClient();
                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", HttpClientProvider.USER_AGENT)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onError(new IOException("Translate HTTP error: " + response.code()));
                        return;
                    }
                    String body = response.body().string();
                    JSONArray arr = new JSONArray(body);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < arr.length(); i++) {
                        sb.append(arr.getString(i));
                    }
                    callback.onSuccess(sb.toString());
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }
}
