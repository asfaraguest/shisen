package id.menatap.app.data.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AniSkipClient {
    public static class SkipInterval {
        public double startTime = 0;
        public double endTime = 0;
        public String type = ""; // "op" or "ed"

        public SkipInterval(double start, double end, String type) {
            this.startTime = start;
            this.endTime = end;
            this.type = type;
        }
    }

    public static class SkipResult {
        public SkipInterval intro;
        public SkipInterval outro;
    }

    public interface Callback {
        void onSuccess(SkipResult result);
        void onError(Throwable t);
    }

    public static void getSkipTimes(int malId, int episodeNum, double durationSec, Callback callback) {
        if (malId <= 0 || episodeNum <= 0) {
            callback.onSuccess(new SkipResult());
            return;
        }

        new Thread(() -> {
            try {
                String url = String.format("https://api.aniskip.com/v2/skip-times/%d/%d?types=op&types=ed&episodeLength=%.0f",
                        malId, episodeNum, durationSec > 0 ? durationSec : 0);
                OkHttpClient client = HttpClientProvider.getClient();
                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", HttpClientProvider.USER_AGENT)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    SkipResult res = new SkipResult();
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        if (root.optBoolean("found", false)) {
                            JSONArray results = root.optJSONArray("results");
                            if (results != null) {
                                for (int i = 0; i < results.length(); i++) {
                                    JSONObject item = results.getJSONObject(i);
                                    String skipType = item.optString("skipType", "");
                                    JSONObject interval = item.optJSONObject("interval");
                                    if (interval != null) {
                                        double start = interval.optDouble("startTime", 0);
                                        double end = interval.optDouble("endTime", 0);
                                        if ("op".equalsIgnoreCase(skipType)) {
                                            res.intro = new SkipInterval(start, end, "op");
                                        } else if ("ed".equalsIgnoreCase(skipType)) {
                                            res.outro = new SkipInterval(start, end, "ed");
                                        }
                                    }
                                }
                            }
                        }
                    }
                    callback.onSuccess(res);
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }
}
