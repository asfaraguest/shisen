package id.shisen.app.data.subtitle;

import android.text.Html;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubtitleParser {

    private static final Pattern ASS_DIALOGUE_RE = Pattern.compile(
            "^\\s*Dialogue:\\s*[^,]*,\\s*([0-9]+:[0-5]?\\d:[0-5]?\\d[.,]\\d+)\\s*,\\s*([0-9]+:[0-5]?\\d:[0-5]?\\d[.,]\\d+)\\s*,[^,]*,[^,]*,[^,]*,[^,]*,[^,]*,[^,]*,(.*)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ASS_TAG_RE = Pattern.compile("\\{[^}]*\\}");
    private static final Pattern HTML_TAG_RE = Pattern.compile("<[^>]*>");

    public static List<SubtitleCue> parse(String rawContent) {
        if (rawContent == null || rawContent.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String content = rawContent.replace("\uFEFF", "").replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = content.split("\n");

        // Deteksi format ASS / SSA
        boolean isAss = false;
        int checkLimit = Math.min(lines.length, 250);
        for (int i = 0; i < checkLimit; i++) {
            if (lines[i].trim().toLowerCase().startsWith("dialogue:")) {
                isAss = true;
                break;
            }
        }

        if (isAss) {
            return parseAss(lines);
        }

        // WebVTT atau SRT parser
        return parseVttOrSrt(lines);
    }

    private static List<SubtitleCue> parseAss(String[] lines) {
        List<SubtitleCue> list = new ArrayList<>();
        for (String line : lines) {
            Matcher m = ASS_DIALOGUE_RE.matcher(line);
            if (!m.matches()) continue;

            long start = parseTimestamp(m.group(1));
            long end = parseTimestamp(m.group(2));
            if (start < 0 || end <= start) continue;

            String text = cleanText(m.group(3));
            if (!text.isEmpty()) {
                list.add(new SubtitleCue(start, end, text));
            }
        }
        return mergeOverlappingCues(list);
    }

    private static List<SubtitleCue> parseVttOrSrt(String[] lines) {
        List<SubtitleCue> list = new ArrayList<>();
        long start = -1;
        long end = -1;
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                if (start >= 0 && end > start && buf.length() > 0) {
                    String clean = cleanText(buf.toString());
                    if (!clean.isEmpty()) {
                        list.add(new SubtitleCue(start, end, clean));
                    }
                }
                start = -1;
                end = -1;
                buf.setLength(0);
                continue;
            }

            if (line.contains("-->")) {
                if (start >= 0 && end > start && buf.length() > 0) {
                    String clean = cleanText(buf.toString());
                    if (!clean.isEmpty()) {
                        list.add(new SubtitleCue(start, end, clean));
                    }
                }
                buf.setLength(0);

                String[] parts = line.split("-->");
                start = parseTimestamp(parts[0].trim());
                String endPart = parts[1].trim();
                int spaceIdx = endPart.indexOf(' ');
                if (spaceIdx > 0) {
                    endPart = endPart.substring(0, spaceIdx).trim();
                }
                end = parseTimestamp(endPart);
                continue;
            }

            if (start >= 0) {
                // Lewati nomor urut SRT jika baris berikutnya berisi "-->"
                if (isNumeric(line) && (i + 1 < lines.length) && lines[i + 1].contains("-->")) {
                    continue;
                }
                if (buf.length() > 0) buf.append("\n");
                buf.append(line);
            }
        }

        if (start >= 0 && end > start && buf.length() > 0) {
            String clean = cleanText(buf.toString());
            if (!clean.isEmpty()) {
                list.add(new SubtitleCue(start, end, clean));
            }
        }

        return mergeOverlappingCues(list);
    }

    public static String cleanText(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        // Bersihkan tag override ASS
        String t = ASS_TAG_RE.matcher(raw).replaceAll("");
        t = t.replace("\\N", "\n").replace("\\n", "\n").replace("\\h", " ");
        // Bersihkan tag HTML
        t = HTML_TAG_RE.matcher(t).replaceAll("");
        // Unescape HTML entities
        try {
            t = Html.fromHtml(t, Html.FROM_HTML_MODE_LEGACY).toString();
        } catch (Throwable ignored) {
            t = t.replace("&nbsp;", " ")
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">");
        }
        return t.trim();
    }

    public static long parseTimestamp(String token) {
        if (token == null) return -1;
        token = token.trim();
        int spaceIdx = token.indexOf(' ');
        if (spaceIdx > 0) {
            token = token.substring(0, spaceIdx).trim();
        }
        token = token.replace(',', '.');
        String[] parts = token.split(":");
        try {
            if (parts.length == 3) {
                long h = Long.parseLong(parts[0].trim());
                long m = Long.parseLong(parts[1].trim());
                double s = Double.parseDouble(parts[2].trim());
                return (long) ((h * 3600 + m * 60 + s) * 1000);
            } else if (parts.length == 2) {
                long m = Long.parseLong(parts[0].trim());
                double s = Double.parseDouble(parts[1].trim());
                return (long) ((m * 60 + s) * 1000);
            }
        } catch (Exception ignored) {}
        return -1;
    }

    public static List<SubtitleCue> mergeOverlappingCues(List<SubtitleCue> cues) {
        if (cues == null || cues.size() <= 1) return cues != null ? cues : new ArrayList<>();

        Collections.sort(cues, (a, b) -> {
            if (a.startMs != b.startMs) {
                return Long.compare(a.startMs, b.startMs);
            }
            return Long.compare(a.endMs, b.endMs);
        });

        List<SubtitleCue> merged = new ArrayList<>();
        for (SubtitleCue c : cues) {
            if (c.text.isEmpty()) continue;
            if (!merged.isEmpty()) {
                SubtitleCue last = merged.get(merged.size() - 1);
                if (Math.abs(c.startMs - last.startMs) <= 50 && Math.abs(c.endMs - last.endMs) <= 50) {
                    if (!last.text.contains(c.text)) {
                        last.text = last.text + "\n" + c.text;
                    }
                    continue;
                }
            }
            merged.add(new SubtitleCue(c.startMs, c.endMs, c.text));
        }
        return merged;
    }

    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) return false;
        }
        return true;
    }
}
