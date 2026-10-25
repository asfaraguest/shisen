package id.menatap.app.data.service;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import id.menatap.app.data.model.StreamSource;
import id.menatap.app.data.model.SubtitleTrack;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AniChanClient {
    public static final String ANICHAN_BASE = "https://anichan.to";

    private static final String STATIC_K = "f7312859";
    private static final String STATIC_O = "dF+YyX8ksuoyznJHUVeHE1yRdVJVwZM4/FEnWRi2Wbg=";
    private static final String STATIC_B = "Mh83TJtN3J8ZDnKT35ASzU31AL5zurEGSAsX9scP4Lk=";

    private static String cachedTokenN = "";
    private static Map<String, String> cachedCookies = new HashMap<>();
    private static long tokenExpiresEpoch = 0;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Throwable t);
    }

    public static void resolveStream(int anilistId, int epNumber, Callback<StreamSource> callback) {
        new Thread(() -> {
            try {
                if (anilistId <= 0) {
                    callback.onError(new IOException("AniList ID tidak valid untuk AniChan"));
                    return;
                }

                ensureSession();

                List<JSONObject> serversList = new ArrayList<>();
                String[] tiers = {"fast", "rest"};

                for (String tier : tiers) {
                    String url = ANICHAN_BASE + "/api/watch/servers?anilistId=" + anilistId + "&ep=" + epNumber + "&category=sub&tier=" + tier;
                    Request.Builder rb = new Request.Builder()
                            .url(url)
                            .header("User-Agent", HttpClientProvider.USER_AGENT)
                            .header("X-Wk", STATIC_K)
                            .header("Referer", ANICHAN_BASE + "/anime/" + anilistId)
                            .header("Origin", ANICHAN_BASE);

                    if (!cachedCookies.isEmpty()) {
                        StringBuilder cookieHeader = new StringBuilder();
                        for (Map.Entry<String, String> e : cachedCookies.entrySet()) {
                            cookieHeader.append(e.getKey()).append("=").append(e.getValue()).append("; ");
                        }
                        rb.header("Cookie", cookieHeader.toString());
                    }

                    OkHttpClient client = HttpClientProvider.getClient();
                    try (Response resp = client.newCall(rb.build()).execute()) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            String jsonStr = resp.body().string();
                            JSONObject raw = new JSONObject(jsonStr);
                            JSONObject data = decryptPayload(raw, cachedTokenN);
                            JSONArray srvArr = data.optJSONArray("servers");
                            if (srvArr != null && srvArr.length() > 0) {
                                for (int i = 0; i < srvArr.length(); i++) {
                                    serversList.add(srvArr.getJSONObject(i));
                                }
                                break;
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (serversList.isEmpty()) {
                    callback.onError(new IOException("Tidak ada server stream pada AniChan untuk ep " + epNumber));
                    return;
                }

                for (JSONObject srv : serversList) {
                    String type = srv.optString("type", "");
                    String streamUrl = srv.optString("stream", "");
                    if ("hls".equalsIgnoreCase(type) && !streamUrl.isEmpty()) {
                        String serverName = srv.optString("name", "AniChan HLS");
                        StreamSource source = new StreamSource(streamUrl, "HD", "AniChan (" + serverName + ")", true);
                        source.provider = "AniChan";
                        source.headers.put("Referer", ANICHAN_BASE + "/");
                        source.headers.put("Origin", ANICHAN_BASE);

                        JSONArray subs = srv.optJSONArray("subtitles");
                        if (subs != null) {
                            for (int i = 0; i < subs.length(); i++) {
                                JSONObject sub = subs.getJSONObject(i);
                                String sUrl = sub.optString("url", "");
                                if (!sUrl.isEmpty()) {
                                    if (sUrl.startsWith("/")) sUrl = ANICHAN_BASE + sUrl;
                                    String lang = sub.optString("lang", "English");
                                    source.subtitles.add(new SubtitleTrack(sUrl, lang.toLowerCase().contains("indonesia") ? "id" : "en", lang, false));
                                }
                            }
                        }

                        callback.onSuccess(source);
                        return;
                    }
                }

                callback.onError(new IOException("Tidak ditemukan server direct HLS pada AniChan"));
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    private static synchronized void ensureSession() throws Exception {
        long now = System.currentTimeMillis() / 1000;
        if (!cachedTokenN.isEmpty() && tokenExpiresEpoch > now + 60) {
            return;
        }

        OkHttpClient client = HttpClientProvider.getClient();
        RequestBody body = RequestBody.create("{\"token\":\"\"}", MediaType.get("application/json; charset=utf-8"));
        Request req = new Request.Builder()
                .url(ANICHAN_BASE + "/api/watch/session")
                .post(body)
                .header("User-Agent", HttpClientProvider.USER_AGENT)
                .header("Referer", ANICHAN_BASE + "/")
                .header("Origin", ANICHAN_BASE)
                .build();

        try (Response resp = client.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                throw new IOException("Gagal inisialisasi sesi AniChan: " + resp.code());
            }

            for (String cookie : resp.headers("Set-Cookie")) {
                int eq = cookie.indexOf('=');
                int sc = cookie.indexOf(';');
                if (eq > 0) {
                    String k = cookie.substring(0, eq).trim();
                    String v = sc > eq ? cookie.substring(eq + 1, sc).trim() : cookie.substring(eq + 1).trim();
                    cachedCookies.put(k, v);
                }
            }

            JSONObject json = new JSONObject(resp.body().string());
            cachedTokenN = json.optString("n", "");
            tokenExpiresEpoch = json.optLong("exp", now + 7200);
        }
    }

    private static JSONObject decryptPayload(JSONObject raw, String tokenN) {
        if (raw.optInt("v", 0) != 1 || !raw.has("i") || !raw.has("d")) {
            return raw;
        }

        try {
            byte[] iv = Base64.decode(raw.getString("i"), Base64.DEFAULT);
            byte[] cipherText = Base64.decode(raw.getString("d"), Base64.DEFAULT);
            byte[] rawO = Base64.decode(STATIC_O, Base64.DEFAULT);
            byte[] rawB = Base64.decode(STATIC_B, Base64.DEFAULT);

            byte[] hmacKey = new byte[Math.min(rawO.length, rawB.length)];
            for (int i = 0; i < hmacKey.length; i++) {
                hmacKey[i] = (byte) (rawO[i] ^ rawB[i]);
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            byte[] aesKey = mac.doFinal(tokenN.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), spec);

            byte[] plainBytes = cipher.doFinal(cipherText);
            return new JSONObject(new String(plainBytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return raw;
        }
    }
}
