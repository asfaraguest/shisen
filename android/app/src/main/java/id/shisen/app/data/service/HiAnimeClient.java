package id.shisen.app.data.service;

import android.net.Uri;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import id.shisen.app.data.model.Anime;
import id.shisen.app.data.model.Episode;
import id.shisen.app.data.model.StreamSource;
import id.shisen.app.data.model.SubtitleTrack;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HiAnimeClient {
    public static final String HI_BASE = NetworkConfig.HIANIME_BASE;

    // Static Megaplay AES-256-CBC keys (32 bytes key, 16 bytes IV)
    private static final byte[] MEGA_KEY = new byte[32];
    private static final byte[] MEGA_IV = "W0;27ToaUpl_P%'c".getBytes(StandardCharsets.UTF_8);

    static {
        byte[] keyPart = "i?LMTAx0Q6,:}50U".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(keyPart, 0, MEGA_KEY, 0, Math.min(keyPart.length, 16));
        // Remaining 16 bytes are 0x00 for 256-bit AES
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Throwable t);
    }

    public static void searchAnime(String query, Callback<List<Anime>> callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onSuccess(Collections.emptyList());
            return;
        }
        HttpClientProvider.execute(() -> {
            try {
                String cleanQ = query.replaceAll("[^\\p{L}\\p{N}\\s-]", "").trim();
                if (cleanQ.isEmpty()) {
                    cleanQ = query.trim();
                }
                String url = HI_BASE + "/search?keyword=" + URLEncoder.encode(cleanQ, "UTF-8");
                String html = fetchString(url, HI_BASE + "/");

                Document doc = Jsoup.parse(html, url);
                List<Anime> results = new ArrayList<>();
                Elements items = doc.select(".flw-item");

                for (Element el : items) {
                    Element titleEl = el.selectFirst(".film-name a");
                    if (titleEl == null) continue;

                    String title = titleEl.attr("title").trim();
                    if (title.isEmpty()) title = titleEl.text().trim();
                    String romaji = titleEl.attr("data-jname").trim();

                    String href = titleEl.attr("href").trim();
                    String slug = href;
                    if (slug.contains("hianime.at/")) {
                        slug = slug.substring(slug.indexOf("hianime.at/") + "hianime.at/".length());
                    }
                    if (slug.startsWith("/")) slug = slug.substring(1);
                    if (slug.startsWith("watch/")) slug = slug.substring("watch/".length());
                    if (slug.contains("?")) slug = slug.substring(0, slug.indexOf("?"));
                    if (slug.endsWith("/")) slug = slug.substring(0, slug.length() - 1);

                    if (slug.toLowerCase().contains("hentai") || title.toLowerCase().contains("hentai") || romaji.toLowerCase().contains("hentai")) {
                        continue;
                    }

                    Element posterEl = el.selectFirst("img.film-poster-img");
                    String poster = posterEl != null ? posterEl.attr("data-src") : "";
                    if (poster.isEmpty() && posterEl != null) {
                        poster = posterEl.attr("src");
                    }

                    Anime a = new Anime();
                    a.title = title;
                    if (!romaji.isEmpty() && !"null".equalsIgnoreCase(romaji)) {
                        a.romajiTitle = romaji;
                    }
                    a.slug = slug;
                    a.poster = poster;

                    Element subEl = el.selectFirst(".tick-sub");
                    if (subEl != null) {
                        try {
                            a.airedEpisodesCount = Integer.parseInt(subEl.text().trim());
                            a.epsCounter = a.airedEpisodesCount + " Ep";
                        } catch (Exception ignored) {}
                    }

                    Element typeEl = el.selectFirst(".fdi-item");
                    if (typeEl != null) {
                        a.format = typeEl.text().trim();
                    }

                    results.add(a);
                }

                callback.onSuccess(results);
            } catch (Throwable e) {
                callback.onError(e);
            }
        });
    }

    public static void getEpisodes(String slug, Callback<List<Episode>> callback) {
        HttpClientProvider.execute(() -> {
            try {
                String numericId = extractNumericId(slug);
                if (numericId.isEmpty()) {
                    callback.onError(new IOException("ID numerik HiAnime tidak ditemukan pada slug: " + slug));
                    return;
                }

                String url = HI_BASE + "/api/theme/episode/list/" + numericId;
                String jsonStr = fetchString(url, HI_BASE + "/");
                String html = "";
                try {
                    JSONObject obj = new JSONObject(jsonStr);
                    html = obj.optString("html", "");
                } catch (Exception e) {
                    html = jsonStr;
                }

                Pattern p = Pattern.compile("data-number=[\"']([0-9]+)[\"'][^>]*?data-id=[\"']([0-9]+)[\"']");
                Matcher m = p.matcher(html);
                List<Episode> list = new ArrayList<>();
                List<Integer> seen = new ArrayList<>();

                while (m.find()) {
                    int epNo = Integer.parseInt(m.group(1));
                    String epId = m.group(2);
                    if (!seen.contains(epNo)) {
                        seen.add(epNo);
                        Episode ep = new Episode(epNo);
                        ep.epId = epId;
                        ep.slug = slug;
                        ep.provider = "hianime";
                        list.add(ep);
                    }
                }

                Collections.sort(list, (e1, e2) -> Integer.compare(e1.number, e2.number));
                callback.onSuccess(list);
            } catch (Throwable e) {
                callback.onError(e);
            }
        });
    }

    public static void resolveStream(String epId, Callback<StreamSource> callback) {
        HttpClientProvider.execute(() -> {
            try {
                String serversUrl = HI_BASE + "/api/theme/episode/servers?episodeId=" + epId;
                String respStr = fetchString(serversUrl, HI_BASE + "/");
                String html = "";
                try {
                    JSONObject obj = new JSONObject(respStr);
                    html = obj.optString("html", "");
                } catch (Exception e) {
                    html = respStr;
                }

                Pattern pHash = Pattern.compile("data-hash=[\"']([^\"']+)[\"']");
                Matcher mHash = pHash.matcher(html);

                List<String> embedCandidates = new ArrayList<>();
                while (mHash.find()) {
                    try {
                        String b64 = mHash.group(1);
                        byte[] dec = Base64.decode(b64, Base64.DEFAULT);
                        String embed = new String(dec, StandardCharsets.UTF_8).trim();
                        if (embed.startsWith("http")) {
                            embedCandidates.add(embed);
                        }
                    } catch (Exception ignored) {}
                }

                if (embedCandidates.isEmpty()) {
                    callback.onError(new IOException("Tidak ada server embed pada HiAnime untuk epId " + epId));
                    return;
                }

                // Prioritize Megaplay
                Collections.sort(embedCandidates, (u1, u2) -> {
                    int s1 = u1.contains("megaplay") ? 0 : 1;
                    int s2 = u2.contains("megaplay") ? 0 : 1;
                    return Integer.compare(s1, s2);
                });

                Exception lastErr = null;
                for (String embedUrl : embedCandidates) {
                    try {
                        StreamSource source;
                        if (embedUrl.contains("zokoanime") || embedUrl.contains("dramahot")) {
                            String pageHtml = fetchString(embedUrl, HI_BASE + "/");
                            source = ZokoAnimeParser.parseEmbed(pageHtml, embedUrl);
                        } else {
                            source = resolveMegaplay(embedUrl);
                        }
                        if (source != null && !source.url.isEmpty()) {
                            callback.onSuccess(source);
                            return;
                        }
                    } catch (Exception e) {
                        lastErr = e;
                    }
                }

                callback.onError(lastErr != null ? lastErr : new IOException("Gagal menyelesaikan stream Megaplay / ZokoAnime"));
            } catch (Throwable e) {
                callback.onError(e);
            }
        });
    }

    public static StreamSource resolveMegaplay(String embedUrl) throws Exception {
        return resolveMegaplay(embedUrl, HI_BASE + "/");
    }

    public static StreamSource resolveMegaplay(String embedUrl, String referer) throws Exception {
        String pageHtml = fetchString(embedUrl, referer != null ? referer : HI_BASE + "/");
        Pattern pDataId = Pattern.compile("data-id=[\"'](\\d+)[\"']");
        Matcher mDataId = pDataId.matcher(pageHtml);
        if (!mDataId.find()) {
            throw new IOException("data-id Megaplay tidak ditemukan");
        }
        String dataId = mDataId.group(1);

        Uri embedUri = Uri.parse(embedUrl);
        String origin = embedUri.getScheme() + "://" + embedUri.getHost();

        String apiUrl = origin + "/stream/getSourcesNew?id=" + dataId;
        String jsonStr;
        try {
            jsonStr = fetchString(apiUrl, embedUrl);
        } catch (Exception e) {
            jsonStr = fetchString(origin + "/stream/getSources?id=" + dataId, embedUrl);
        }

        JSONObject cfg = new JSONObject(jsonStr);
        String masterUrl = "";

        if (cfg.has("sources")) {
            JSONArray sources = cfg.optJSONArray("sources");
            if (sources != null && sources.length() > 0) {
                masterUrl = sources.getJSONObject(0).optString("file", "");
            }
        }

        if (masterUrl.isEmpty() && cfg.has("enc")) {
            String enc = cfg.getString("enc");
            String decJson = decryptMegaPayload(enc);
            if (decJson != null) {
                JSONObject decObj = new JSONObject(decJson);
                masterUrl = decObj.optString("file", "");
            }
        }

        if (masterUrl.isEmpty()) {
            throw new IOException("Megaplay sources tidak memuat master URL m3u8");
        }

        if (masterUrl.startsWith("/")) {
            masterUrl = origin + masterUrl;
        }

        StreamSource streamSource = new StreamSource(masterUrl, "HD", "HiAnime (Megaplay)", true);
        streamSource.provider = "HiAnime";
        streamSource.headers.put("Referer", origin + "/");
        streamSource.headers.put("Origin", origin);

        // Parse Subtitles
        JSONArray tracksArr = cfg.optJSONArray("tracks");
        if (tracksArr != null) {
            String subReferer = origin + "/";
            for (int i = 0; i < tracksArr.length(); i++) {
                JSONObject tr = tracksArr.getJSONObject(i);
                String kind = tr.optString("kind", "");
                if ("captions".equalsIgnoreCase(kind) || "subtitles".equalsIgnoreCase(kind)) {
                    String file = tr.optString("file", "");
                    String label = tr.optString("label", "English");
                    boolean isDef = tr.optBoolean("default", false);
                    String lang = SubtitleTrack.inferLanguage(label, tr.optString("lang", ""));
                    if (!file.isEmpty()) {
                        streamSource.subtitles.add(new SubtitleTrack(file, lang, label, isDef, subReferer));
                    }
                }
            }
        }

        // Parse Intro / Outro (Skip OP/ED)
        JSONObject introObj = cfg.optJSONObject("intro");
        JSONObject outroObj = cfg.optJSONObject("outro");
        if (introObj != null || outroObj != null) {
            AniSkipClient.SkipResult skips = new AniSkipClient.SkipResult();
            if (introObj != null) {
                double start = introObj.optDouble("start", 0);
                double end = introObj.optDouble("end", 0);
                if (end > start) {
                    skips.intro = new AniSkipClient.SkipInterval(start, end, "op");
                }
            }
            if (outroObj != null) {
                double start = outroObj.optDouble("start", 0);
                double end = outroObj.optDouble("end", 0);
                if (end > start) {
                    skips.outro = new AniSkipClient.SkipInterval(start, end, "ed");
                }
            }
            if (skips.intro != null || skips.outro != null) {
                streamSource.skipTimes = skips;
            }
        }

        return streamSource;
    }

    public static String decryptMegaPayload(String base64UrlData) {
        try {
            String b64 = base64UrlData.replace('-', '+').replace('_', '/');
            while (b64.length() % 4 != 0) {
                b64 += "=";
            }
            byte[] cipherBytes = Base64.decode(b64, Base64.DEFAULT);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(MEGA_KEY, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(MEGA_IV);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decrypted = cipher.doFinal(cipherBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractNumericId(String slug) {
        if (slug == null || slug.isEmpty()) return "";
        Pattern p = Pattern.compile("-(\\d+)(?:[/?#]|$)");
        Matcher m = p.matcher(slug);
        if (m.find()) {
            return m.group(1);
        }
        if (slug.matches("^\\d+$")) {
            return slug;
        }
        return "";
    }

    private static volatile OkHttpClient resolveClient;

    private static OkHttpClient getResolveClient() {
        if (resolveClient == null) {
            synchronized (HiAnimeClient.class) {
                if (resolveClient == null) {
                    resolveClient = HttpClientProvider.getClient().newBuilder()
                            .connectTimeout(6, TimeUnit.SECONDS)
                            .readTimeout(8, TimeUnit.SECONDS)
                            .build();
                }
            }
        }
        return resolveClient;
    }

    private static String fetchString(String url, String referer) throws IOException {
        OkHttpClient client = getResolveClient();
        Request.Builder rb = new Request.Builder()
                .url(url)
                .header("User-Agent", HttpClientProvider.USER_AGENT)
                .header("Accept", "*/*");
        if (referer != null) {
            rb.header("Referer", referer);
        }
        try (Response resp = client.newCall(rb.build()).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("HTTP error " + resp.code() + " for " + url);
            }
            return resp.body() != null ? resp.body().string() : "";
        }
    }
}
