package id.shisen.app.data.service;

import android.net.Uri;
import android.util.Base64;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import id.shisen.app.data.model.Anime;
import id.shisen.app.data.model.Episode;
import id.shisen.app.data.model.StreamSource;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OtakudesuScraper {
    public static final String BASE_URL = NetworkConfig.OTAKUDESU_BASE;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Throwable t);
    }

    public static void getOngoing(Callback<List<Anime>> callback) {
        HttpClientProvider.execute(() -> {
            try {
                String url = BASE_URL + "/ongoing-anime/";
                Document doc = fetchDoc(url, BASE_URL + "/");
                List<Anime> results = new ArrayList<>();
                Elements items = doc.select(".venz ul li, .rapi ul li");
                for (Element el : items) {
                    Element titleEl = el.selectFirst(".jdlfl, h2");
                    Element linkEl = el.selectFirst("a");
                    if (linkEl == null) continue;
                    String title = titleEl != null ? titleEl.text() : linkEl.text();
                    if (isHentai(title)) continue;
                    String link = linkEl.attr("href");
                    String slug = extractSlug(link);
                    String poster = "";
                    Element imgEl = el.selectFirst("img");
                    if (imgEl != null) {
                        poster = imgEl.attr("src");
                    }
                    Element epEl = el.selectFirst(".epz, .epztipe");
                    String eps = epEl != null ? epEl.text() : "Ongoing";

                    Anime a = new Anime();
                    a.title = title;
                    a.slug = slug;
                    a.poster = poster;
                    a.epsCounter = eps;
                    a.format = "TV";
                    a.status = "RELEASING";
                    results.add(a);
                }
                callback.onSuccess(results);
            } catch (Throwable e) {
                callback.onError(e);
            }
        });
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
                String searchUrl = BASE_URL + "/?s=" + URLEncoder.encode(cleanQ, "UTF-8") + "&post_type=anime";
                Document doc = fetchDoc(searchUrl, BASE_URL + "/");
                List<Anime> results = new ArrayList<>();
                Elements items = doc.select(".chivsrc li, .venz ul li");
                for (Element el : items) {
                    Element titleEl = el.selectFirst("h2 a, .jdlfl, .ot-k-j");
                    if (titleEl == null) titleEl = el.selectFirst("a");
                    if (titleEl == null) continue;
                    String title = titleEl.text();
                    if (isHentai(title)) continue;
                    String link = titleEl.hasAttr("href") ? titleEl.attr("href") : el.select("a").attr("href");
                    String slug = extractSlug(link);
                    String poster = "";
                    Element imgEl = el.selectFirst("img");
                    if (imgEl != null) {
                        poster = imgEl.attr("src");
                    }
                    Anime a = new Anime();
                    a.title = title;
                    a.slug = slug;
                    a.poster = poster;
                    a.format = "TV";
                    results.add(a);
                }
                callback.onSuccess(results);
            } catch (Throwable e) {
                callback.onError(e);
            }
        });
    }

    public static void getEpisodes(String animeSlug, Callback<List<Episode>> callback) {
        HttpClientProvider.execute(() -> {
            try {
                String animeUrl = animeSlug.startsWith("http") ? animeSlug : (BASE_URL + "/anime/" + animeSlug.replaceAll("^/+|/+$", "") + "/");
                Document doc = fetchDoc(animeUrl, BASE_URL + "/");
                List<Episode> list = new ArrayList<>();
                Elements epLinks = doc.select(".episodelst ul li a, .ot-ep");
                for (Element a : epLinks) {
                    String epText = a.text();
                    String epHref = a.attr("href");
                    int epNum = parseEpisodeNumber(epText);
                    if (epNum > 0) {
                        Episode ep = new Episode(epNum, epText, extractSlug(epHref));
                        ep.url = epHref;
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

    public static void resolveStream(String episodeSlugOrUrl, Callback<StreamSource> callback) {
        HttpClientProvider.execute(() -> {
            try {
                String epUrl = episodeSlugOrUrl.startsWith("http") ? episodeSlugOrUrl : (BASE_URL + "/episode/" + episodeSlugOrUrl.replaceAll("^/+|/+$", "") + "/");
                Document doc = fetchDoc(epUrl, BASE_URL + "/");

                // Check iframe
                Element iframeEl = doc.selectFirst(".responsive-embed-stream iframe");
                if (iframeEl != null) {
                    String src = iframeEl.attr("src");
                    if (!src.isEmpty()) {
                        StreamSource source = resolveEmbedPlayer(src);
                        if (source != null) {
                            callback.onSuccess(source);
                            return;
                        }
                    }
                }

                // Check mirrors
                Elements mirrorElements = doc.select(".mirrorstream ul li a");
                for (Element m : mirrorElements) {
                    String dataContent = m.attr("data-content");
                    if (!dataContent.isEmpty()) {
                        try {
                            String decoded = new String(Base64.decode(dataContent, Base64.DEFAULT), StandardCharsets.UTF_8);
                            if (decoded.contains("http")) {
                                StreamSource source = new StreamSource(decoded, "720p", "Mirror", decoded.contains(".m3u8"));
                                callback.onSuccess(source);
                                return;
                            }
                        } catch (Exception ignored) {}
                    }
                }

                callback.onError(new IOException("Stream URL tidak ditemukan di server Otakudesu."));
            } catch (Throwable e) {
                callback.onError(e);
            }
        });
    }

    private static StreamSource resolveEmbedPlayer(String embedUrl) {
        try {
            Document embedDoc = fetchDoc(embedUrl, BASE_URL + "/");
            String html = embedDoc.html();

            // Check if putarin window.__PX AES-256-GCM
            Pattern pxPattern = Pattern.compile("window\\.__PX\\s*=\\s*(\\{[^<]+?\\});");
            Matcher pxMatch = pxPattern.matcher(html);
            if (pxMatch.find()) {
                JSONObject px = new JSONObject(pxMatch.group(1));
                String nonce = px.optString("n", "");
                String ciphertextB64 = px.optString("d", "");

                Uri uri = Uri.parse(embedUrl);
                String origin = uri.getScheme() + "://" + uri.getHost();

                String keyUrl = origin + "/api/pk?n=" + URLEncoder.encode(nonce, "UTF-8");
                String keyHex = fetchString(keyUrl, embedUrl).trim();

                if (keyHex.length() >= 64) {
                    byte[] keyBytes = hexStringToByteArray(keyHex.substring(0, 64));
                    byte[] rawData = Base64.decode(ciphertextB64, Base64.DEFAULT);

                    if (rawData.length >= 28) {
                        byte[] iv = new byte[12];
                        System.arraycopy(rawData, 0, iv, 0, 12);

                        byte[] ct = new byte[rawData.length - 12];
                        System.arraycopy(rawData, 12, ct, 0, ct.length);

                        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
                        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
                        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);

                        byte[] decrypted = cipher.doFinal(ct);
                        String decStr = new String(decrypted, StandardCharsets.UTF_8);
                        JSONObject config = new JSONObject(decStr);

                        String fileUrl = config.optString("file", config.optString("url", ""));
                        if (fileUrl.isEmpty() && config.has("sources")) {
                            fileUrl = config.getJSONArray("sources").getJSONObject(0).optString("file", "");
                        }

                        if (!fileUrl.isEmpty()) {
                            StreamSource source = new StreamSource(fileUrl, "HD", "Putarin HLS", fileUrl.contains(".m3u8"));
                            source.headers.put("Referer", embedUrl);
                            source.headers.put("Origin", origin);
                            return source;
                        }
                    }
                }
            }

            // Direct file regex match in HTML
            Pattern p = Pattern.compile("file:\\s*[\"']([^\"']+\\.m3u8[^\"']*)[\"']");
            Matcher m = p.matcher(html);
            if (m.find()) {
                String hlsUrl = m.group(1);
                StreamSource s = new StreamSource(hlsUrl, "HD", "HLS", true);
                s.headers.put("Referer", embedUrl);
                return s;
            }

        } catch (Exception ignored) {}
        return null;
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static Document fetchDoc(String url, String referer) throws IOException {
        String body = fetchString(url, referer);
        return Jsoup.parse(body, url);
    }

    private static String fetchString(String url, String referer) throws IOException {
        OkHttpClient client = HttpClientProvider.getClient();
        Request.Builder b = new Request.Builder()
                .url(url)
                .header("User-Agent", HttpClientProvider.USER_AGENT);
        if (referer != null) {
            b.header("Referer", referer);
        }
        try (Response response = client.newCall(b.build()).execute()) {
            if (!response.isSuccessful()) throw new IOException("HTTP error: " + response.code());
            return response.body().string();
        }
    }

    private static String extractSlug(String url) {
        if (url == null) return "";
        url = url.replaceAll("/+$", "");
        int lastSlash = url.lastIndexOf('/');
        return lastSlash >= 0 ? url.substring(lastSlash + 1) : url;
    }

    private static int parseEpisodeNumber(String text) {
        try {
            Pattern p = Pattern.compile("(?i)(?:episode|eps|ep)\\s*#?\\s*(\\d+)");
            Matcher m = p.matcher(text);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
            Pattern p2 = Pattern.compile("(\\d+)");
            Matcher m2 = p2.matcher(text);
            if (m2.find()) {
                return Integer.parseInt(m2.group(1));
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static boolean isHentai(String title) {
        if (title == null) return false;
        String t = title.toLowerCase(Locale.US);
        return t.contains("hentai") || t.contains("overflow") || t.contains("uncensored");
    }
}
