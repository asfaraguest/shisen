package id.menatap.app.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TitleMatcher {

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "the", "a", "an", "of", "in", "to", "and", "und", "der", "die", "das",
            "no", "wa", "ga", "ni", "de", "series", "season"
    ));

    private static final Set<String> MOVIE_KEYWORDS = new HashSet<>(Arrays.asList(
            "movie", "film", "gekijouban"
    ));

    private static final Set<String> OVA_KEYWORDS = new HashSet<>(Arrays.asList(
            "ova", "special", "oad"
    ));

    public static String normalize(String s) {
        if (s == null) return "";
        String clean = s.toLowerCase(Locale.US).replace("&", "and");
        clean = clean.replaceAll("\\bund\\b", "and");
        return clean.replaceAll("[^a-zA-Z0-9]", "");
    }

    public static int extractSeasonNumber(String title) {
        if (title == null || title.isEmpty()) return 1;
        String t = title.toLowerCase(Locale.US);

        Pattern p1 = Pattern.compile("\\b(\\d+)(?:nd|rd|th|st)?\\s*season\\b");
        Matcher m1 = p1.matcher(t);
        if (m1.find()) {
            try { return Integer.parseInt(m1.group(1)); } catch (Exception ignored) {}
        }

        Pattern p2 = Pattern.compile("\\bseason\\s*(\\d+)\\b");
        Matcher m2 = p2.matcher(t);
        if (m2.find()) {
            try { return Integer.parseInt(m2.group(1)); } catch (Exception ignored) {}
        }

        Pattern p3 = Pattern.compile("\\bpart\\s*(\\d+)\\b");
        Matcher m3 = p3.matcher(t);
        if (m3.find()) {
            try { return Integer.parseInt(m3.group(1)); } catch (Exception ignored) {}
        }

        Pattern p4 = Pattern.compile("\\bs(\\d+)\\b");
        Matcher m4 = p4.matcher(t);
        if (m4.find()) {
            try { return Integer.parseInt(m4.group(1)); } catch (Exception ignored) {}
        }

        if (t.contains("2nd season") || t.contains("season 2") || t.contains("part 2")) return 2;
        if (t.contains("3rd season") || t.contains("season 3") || t.contains("part 3")) return 3;
        if (t.contains("4th season") || t.contains("season 4") || t.contains("final season")) return 4;

        return 1;
    }

    public static Set<String> cleanTitleTokens(String title) {
        Set<String> tokens = new HashSet<>();
        if (title == null || title.isEmpty()) return tokens;

        String t = title.replaceAll("[^a-zA-Z0-9\\s]", " ").toLowerCase(Locale.US);
        for (String tok : t.split("\\s+")) {
            if (tok.isEmpty()) continue;
            if ("ovas".equals(tok)) tok = "ova";
            else if ("specials".equals(tok)) tok = "special";
            else if ("movies".equals(tok)) tok = "movie";
            tokens.add(tok);
        }
        return tokens;
    }

    public static boolean isTitleMatch(String target, String candidate) {
        if (target == null || candidate == null) return false;
        if (target.trim().isEmpty() || candidate.trim().isEmpty()) return false;

        // 1. Exact normalized match
        String normA = normalize(target);
        String normB = normalize(candidate);
        if (normA.equals(normB) && !normA.isEmpty()) {
            return true;
        }

        // 2. Strict season consistency (prevent S2 matching S1)
        int sA = extractSeasonNumber(target);
        int sB = extractSeasonNumber(candidate);
        if ((sA > 1 || sB > 1) && sA != sB) {
            return false;
        }

        Set<String> tokensA = cleanTitleTokens(target);
        Set<String> tokensB = cleanTitleTokens(candidate);
        if (tokensA.isEmpty() || tokensB.isEmpty()) return false;

        // 3. Isolate Movie/Film vs TV Series
        boolean isMovieA = false;
        for (String m : MOVIE_KEYWORDS) { if (tokensA.contains(m)) { isMovieA = true; break; } }
        boolean isMovieB = false;
        for (String m : MOVIE_KEYWORDS) { if (tokensB.contains(m)) { isMovieB = true; break; } }
        if (isMovieA != isMovieB) return false;

        // 4. Isolate OVA/Special vs TV Series
        boolean isOvaA = false;
        for (String o : OVA_KEYWORDS) { if (tokensA.contains(o)) { isOvaA = true; break; } }
        boolean isOvaB = false;
        for (String o : OVA_KEYWORDS) { if (tokensB.contains(o)) { isOvaB = true; break; } }
        if (isOvaA != isOvaB) return false;

        // 5. Significant token overlap calculation
        Set<String> common = new HashSet<>(tokensA);
        common.retainAll(tokensB);
        common.removeAll(STOPWORDS);

        Set<String> sigA = new HashSet<>(tokensA);
        sigA.removeAll(STOPWORDS);

        Set<String> sigB = new HashSet<>(tokensB);
        sigB.removeAll(STOPWORDS);

        if (sigA.isEmpty() || sigB.isEmpty()) {
            Set<String> allCommon = new HashSet<>(tokensA);
            allCommon.retainAll(tokensB);
            return ((double) allCommon.size() / Math.max(tokensA.size(), tokensB.size())) >= 0.75;
        }

        double overlap = (double) common.size() / Math.max(sigA.size(), sigB.size());
        return overlap >= 0.55;
    }

    public static String cleanSearchQuery(String title) {
        if (title == null) return "";
        // Remove text after colon or parenthesis if too long
        String q = title.replaceAll("(?i)(season\\s*\\d+|\\d+(?:nd|rd|th|st)\\s*season|part\\s*\\d+)", " ");
        q = q.replaceAll("[^a-zA-Z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        return q.isEmpty() ? title.trim() : q;
    }
}
