package id.menatap.app.data.service;

import android.net.Uri;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import id.menatap.app.data.model.StreamSource;
import id.menatap.app.data.model.SubtitleTrack;

/**
 * ZokoAnimeParser ports the legacy Tatap parsing logic for ZokoAnime video embeds
 * (hosts: zokoanime.video and dramahot.top / hls.dramahot.top), including XOR blob deobfuscation,
 * 404 detection, subtitle track harvesting, and HLS demuxed audio playlist handling.
 */
public class ZokoAnimeParser {

    public static final byte[] XOR_KEY = "otaku-embed-v1".getBytes(StandardCharsets.UTF_8);
    public static final String DEFAULT_REFERER = "https://zokoanime.video/";
    public static final String DEFAULT_ORIGIN = "https://zokoanime.video";

    public static class AudioTrack {
        public final String name;
        public final String language;
        public final String uri;
        public final boolean isDefault;

        public AudioTrack(String name, String language, String uri, boolean isDefault) {
            this.name = name;
            this.language = language;
            this.uri = uri;
            this.isDefault = isDefault;
        }
    }

    /**
     * Deobfuscates the window.__P base64 blob using byte-wise XOR with "otaku-embed-v1".
     */
    public static String deobfuscate(String b64) {
        if (b64 == null || b64.trim().isEmpty()) {
            throw new IllegalArgumentException("ZokoAnime payload cannot be null or empty");
        }
        byte[] raw;
        try {
            raw = java.util.Base64.getDecoder().decode(b64.trim());
        } catch (Throwable t) {
            raw = Base64.decode(b64.trim(), Base64.DEFAULT);
        }

        byte[] result = new byte[raw.length];
        for (int i = 0; i < raw.length; i++) {
            result[i] = (byte) (raw[i] ^ XOR_KEY[i % XOR_KEY.length]);
        }
        return new String(result, StandardCharsets.UTF_8);
    }

    /**
     * Parses the HTML page of a ZokoAnime embed into a StreamSource.
     */
    public static StreamSource parseEmbed(String pageHtml, String embedUrl) throws Exception {
        if (pageHtml == null || pageHtml.isEmpty()) {
            throw new IOException("Empty response from ZokoAnime server");
        }

        // 1. Detect 404 Not Found page signature from ZokoAnime
        if (pageHtml.contains("<title>Player</title>") && pageHtml.contains("Not ") && pageHtml.contains("found.")) {
            throw new IOException("Anime not found on ZokoAnime server (404 Not Found)");
        }

        // 2. Extract window.__P = "..."
        Pattern pBlob = Pattern.compile("window\\.__P\\s*=\\s*[\"']([^\"']+)[\"']");
        Matcher mBlob = pBlob.matcher(pageHtml);
        if (!mBlob.find()) {
            throw new IOException("window.__P payload not found in ZokoAnime page");
        }

        String blob = mBlob.group(1);
        String configJsonStr = deobfuscate(blob);

        JSONObject cfg = new JSONObject(configJsonStr);
        String masterUrl = cfg.optString("src", "");
        if (masterUrl.isEmpty() && cfg.has("sources")) {
            JSONArray sources = cfg.optJSONArray("sources");
            if (sources != null && sources.length() > 0) {
                masterUrl = sources.getJSONObject(0).optString("src", "");
            }
        }

        if (masterUrl.isEmpty() || !masterUrl.contains(".m3u8")) {
            throw new IOException("Valid m3u8 stream URL not found in ZokoAnime config");
        }

        String referer = DEFAULT_REFERER;
        String origin = DEFAULT_ORIGIN;
        if (embedUrl != null && embedUrl.contains("://")) {
            int schemeEnd = embedUrl.indexOf("://") + 3;
            int hostEnd = embedUrl.indexOf('/', schemeEnd);
            if (hostEnd != -1) {
                origin = embedUrl.substring(0, hostEnd);
            } else {
                origin = embedUrl;
            }
            referer = origin + "/";
        }

        StreamSource streamSource = new StreamSource(masterUrl, "HD", "ZokoAnime", true);
        streamSource.provider = "ZokoAnime";
        streamSource.headers.put("Referer", referer);
        streamSource.headers.put("Origin", origin);

        // 3. Extract Subtitles
        JSONArray subsArr = cfg.optJSONArray("subtitles");
        if (subsArr != null) {
            for (int i = 0; i < subsArr.length(); i++) {
                JSONObject sObj = subsArr.getJSONObject(i);
                String url = sObj.optString("src", sObj.optString("file", ""));
                String label = sObj.optString("label", "English");
                boolean isDefault = sObj.optBoolean("default", false);
                if (!url.isEmpty()) {
                    String lang = label.toLowerCase().contains("indonesia") ? "id" : "en";
                    streamSource.subtitles.add(new SubtitleTrack(url, lang, label, isDefault));
                }
            }
        }

        return streamSource;
    }

    /**
     * Parses demuxed HLS audio streams from Master m3u8 playlist content.
     * Identifies #EXT-X-MEDIA:TYPE=AUDIO tags and resolves their absolute or relative URIs.
     */
    public static List<AudioTrack> parseDemuxedAudioTracks(String masterM3u8Content, String masterUrl) {
        List<AudioTrack> tracks = new ArrayList<>();
        if (masterM3u8Content == null || masterM3u8Content.isEmpty()) {
            return tracks;
        }

        String baseUri = "";
        if (masterUrl != null && masterUrl.contains("/")) {
            baseUri = masterUrl.substring(0, masterUrl.lastIndexOf('/') + 1);
        }

        Pattern pAudio = Pattern.compile("#EXT-X-MEDIA:TYPE=AUDIO,([^\\r\\n]+)");
        Matcher mAudio = pAudio.matcher(masterM3u8Content);

        while (mAudio.find()) {
            String attrs = mAudio.group(1);
            String name = extractAttr(attrs, "NAME");
            String lang = extractAttr(attrs, "LANGUAGE");
            String uri = extractAttr(attrs, "URI");
            boolean isDefault = "YES".equalsIgnoreCase(extractAttr(attrs, "DEFAULT"));

            if (uri != null && !uri.isEmpty()) {
                String fullUri = uri;
                if (!uri.startsWith("http://") && !uri.startsWith("https://")) {
                    fullUri = baseUri + uri;
                }
                tracks.add(new AudioTrack(
                        name != null ? name : "Default Audio",
                        lang != null ? lang : "und",
                        fullUri,
                        isDefault
                ));
            }
        }

        return tracks;
    }

    private static String extractAttr(String attrs, String key) {
        Pattern p = Pattern.compile("(?:^|,)\\s*" + key + "=\"?([^\",]+)\"?");
        Matcher m = p.matcher(attrs);
        return m.find() ? m.group(1).trim() : null;
    }
}
