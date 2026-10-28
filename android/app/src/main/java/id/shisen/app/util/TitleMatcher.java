package id.shisen.app.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import id.shisen.app.data.model.Anime;

public class TitleMatcher {

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "the", "a", "an", "of", "in", "to", "and", "und", "der", "die", "das",
            "no", "wa", "ga", "ni", "de", "series", "season", "s", "part", "cour",
            "1st", "2nd", "3rd", "4th", "5th", "1", "2", "3", "4", "5"
    ));

    private static final Set<String> MOVIE_KEYWORDS = new HashSet<>(Arrays.asList(
            "movie", "film", "gekijouban"
    ));

    private static final Set<String> OVA_KEYWORDS = new HashSet<>(Arrays.asList(
            "ova", "special", "oad"
    ));

    private static final Set<String> GENERIC_ANIME_WORDS = new HashSet<>(Arrays.asList(
            "blue", "sword", "dungeon", "hero", "heroes", "lord", "king", "witch", "girl", "girls",
            "magic", "magical", "academy", "battle", "world", "game", "life", "isekai", "level",
            "reincarnated", "reincarnation", "master", "shinobi", "ninja", "monster", "monsters",
            "chronicles", "chronicle", "story", "stories", "tales", "tale", "legend", "legends",
            "war", "wars", "star", "stars", "dark", "blood", "gate", "zero", "devil", "demon",
            "demons", "exorcist", "prince", "princess", "knight", "knights", "danjo", "people",
            "meshi", "lock", "box", "super", "daima", "shikkaku", "online", "piece", "adventure",
            "order", "fate", "high", "live", "code", "fire", "city", "gold", "time", "dead"
    ));

    // Pairs of mutually exclusive franchise/subtitle keywords that must never cross-match
    private static final String[][] CONTRADICTORY_PAIRS = {
            {"box", "lock"},
            {"daima", "super"},
            {"daima", "kai"},
            {"people", "meshi"},
            {"brotherhood", "shamballa"},
            {"gakkou", "enmusubi"}
    };

    public static boolean hasContradictoryKeywords(String a, String b) {
        if (a == null || b == null) return false;
        Set<String> tokensA = cleanTitleTokens(a);
        Set<String> tokensB = cleanTitleTokens(b);
        for (String[] pair : CONTRADICTORY_PAIRS) {
            String word1 = pair[0];
            String word2 = pair[1];
            if ((tokensA.contains(word1) && tokensB.contains(word2)) ||
                (tokensA.contains(word2) && tokensB.contains(word1))) {
                return true;
            }
        }
        return false;
    }

    public static String stripQualifiers(String s) {
        if (s == null) return "";
        return s.replaceAll("(?i)\\b(tv|sub|dub|uncensored|censored)\\b|\\((?:tv|sub|dub|uncensored|censored)\\)", "").trim();
    }

    public static String cleanSlug(String slug) {
        if (slug == null) return "";
        return slug.replaceAll("-\\d+$", "").trim();
    }

    public static String normalize(String s) {
        if (s == null) return "";
        String clean = s.toLowerCase(Locale.US).replace("&", "and");
        clean = clean.replaceAll("\\bund\\b", "and");
        return clean.replaceAll("[^a-zA-Z0-9]", "");
    }

    public static int extractYear(String title) {
        if (title == null || title.isEmpty()) return -1;
        Pattern p = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b");
        Matcher m = p.matcher(title);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
        }
        return -1;
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

        Pattern pRoman = Pattern.compile("\\bseason\\s*(ii|iii|iv|v)\\b");
        Matcher mRoman = pRoman.matcher(t);
        if (mRoman.find()) {
            String rom = mRoman.group(1);
            if ("ii".equals(rom)) return 2;
            if ("iii".equals(rom)) return 3;
            if ("iv".equals(rom)) return 4;
            if ("v".equals(rom)) return 5;
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

        // 1b. Stripped qualifiers match (e.g. "Dan Da Dan (TV)" == "Dan Da Dan")
        String strippedA = normalize(stripQualifiers(target));
        String strippedB = normalize(stripQualifiers(candidate));
        if (!strippedA.isEmpty() && strippedA.equals(strippedB)) {
            return true;
        }

        // 1c. Clean slug match (e.g. "dan-da-dan-19319" stripped to "dan-da-dan" == "dandadan")
        String cleanSlugA = normalize(cleanSlug(target));
        String cleanSlugB = normalize(cleanSlug(candidate));
        if (!cleanSlugA.isEmpty() && cleanSlugA.equals(cleanSlugB)) {
            return true;
        }
        if (!normA.isEmpty() && normA.equals(cleanSlugB)) {
            return true;
        }
        if (!normB.isEmpty() && normB.equals(cleanSlugA)) {
            return true;
        }

        // 2. Strict season consistency (prevent S2 matching S1, S3 matching S2)
        int sA = extractSeasonNumber(target);
        int sB = extractSeasonNumber(candidate);
        if ((sA > 1 || sB > 1) && sA != sB) {
            return false;
        }

        // 3. Strict year consistency if both explicitly provide a year (e.g. 2024 remake vs 1989 original)
        int yA = extractYear(target);
        int yB = extractYear(candidate);
        if (yA != -1 && yB != -1 && yA != yB) {
            return false;
        }

        // 3b. Contradictory Keyword Check before substring comparison
        if (hasContradictoryKeywords(target, candidate)) {
            return false;
        }

        // Substring normalized check if one contains the other (min 5 chars)
        if (normA.length() >= 5 && normB.length() >= 5) {
            if (normA.contains(normB) || normB.contains(normA)) {
                if (sA == sB) {
                    if (yA == -1 || yB == -1 || yA == yB) {
                        return true;
                    }
                }
            }
        }

        Set<String> tokensA = cleanTitleTokens(target);
        Set<String> tokensB = cleanTitleTokens(candidate);
        if (tokensA.isEmpty() || tokensB.isEmpty()) return false;

        // 4. Isolate Movie/Film vs TV Series
        boolean isMovieA = false;
        for (String m : MOVIE_KEYWORDS) { if (tokensA.contains(m)) { isMovieA = true; break; } }
        boolean isMovieB = false;
        for (String m : MOVIE_KEYWORDS) { if (tokensB.contains(m)) { isMovieB = true; break; } }
        if (isMovieA != isMovieB) return false;

        // 5. Isolate OVA/Special vs TV Series
        boolean isOvaA = false;
        for (String o : OVA_KEYWORDS) { if (tokensA.contains(o)) { isOvaA = true; break; } }
        boolean isOvaB = false;
        for (String o : OVA_KEYWORDS) { if (tokensB.contains(o)) { isOvaB = true; break; } }
        if (isOvaA != isOvaB) return false;

        // 6. Contradictory Keyword Check (e.g. "box" vs "lock", "daima" vs "super")
        for (String[] pair : CONTRADICTORY_PAIRS) {
            String word1 = pair[0];
            String word2 = pair[1];
            if ((tokensA.contains(word1) && tokensB.contains(word2)) ||
                (tokensA.contains(word2) && tokensB.contains(word1))) {
                return false;
            }
        }

        // 7. Significant token overlap calculation
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

        // Special handling for prominent multi-word signature franchises like "Re:Zero"
        if (tokensA.contains("re") && tokensA.contains("zero") &&
            tokensB.contains("re") && tokensB.contains("zero") && sA == sB) {
            return true;
        }

        // Count non-generic common words
        int nonGenericCommon = 0;
        for (String tok : common) {
            if (!GENERIC_ANIME_WORDS.contains(tok)) {
                nonGenericCommon++;
            }
        }

        // Short titles (<= 2 significant words like "Blue Box", "Blue Lock", "Amagami SS")
        if (sigA.size() <= 2 || sigB.size() <= 2) {
            double ratio = (double) common.size() / Math.max(sigA.size(), sigB.size());
            if (ratio >= 0.99) {
                // If 100% matched, require at least one non-generic word or 2 words minimum
                return nonGenericCommon >= 1 || common.size() >= 2;
            }
            // For subset match in short title (e.g. "Frieren" in "Sousou no Frieren"):
            // Must have a strong non-generic distinctive name
            boolean aInB = sigB.containsAll(sigA);
            boolean bInA = sigA.containsAll(sigB);
            if ((aInB || bInA) && nonGenericCommon >= 1 && common.size() >= 2) {
                return true;
            }
            return false;
        }

        // Long titles (>= 3 significant words):
        // Require high token overlap (>= 70%) and at least one non-generic distinctive token
        double overlap = (double) common.size() / Math.max(sigA.size(), sigB.size());
        if (overlap >= 0.70 && (nonGenericCommon >= 1 || common.size() >= 3)) {
            return true;
        }

        // Full subset match for long titles
        if ((sigA.containsAll(sigB) || sigB.containsAll(sigA)) && common.size() >= 3 && (nonGenericCommon >= 1 || common.size() >= 4)) {
            return true;
        }

        return false;
    }

    public static boolean isMatchAny(Anime anime, Anime candidate) {
        if (anime == null || candidate == null) return false;
        String aTitle = anime.title;
        String aRomaji = anime.romajiTitle;
        String cTitle = candidate.title;
        String cRomaji = candidate.romajiTitle;

        if (isTitleMatch(aTitle, cTitle)) return true;
        if (aRomaji != null && !aRomaji.isEmpty() && isTitleMatch(aRomaji, cTitle)) return true;
        if (cRomaji != null && !cRomaji.isEmpty() && isTitleMatch(aTitle, cRomaji)) return true;
        if (aRomaji != null && !aRomaji.isEmpty() && cRomaji != null && !cRomaji.isEmpty() && isTitleMatch(aRomaji, cRomaji)) return true;

        return false;
    }

    public static String cleanSearchQuery(String title) {
        if (title == null) return "";
        // Remove text after colon or parenthesis if too long
        String q = title.replaceAll("(?i)(season\\s*\\d+|\\d+(?:nd|rd|th|st)\\s*season|part\\s*\\d+)", " ");
        q = q.replaceAll("[^a-zA-Z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        return q.isEmpty() ? title.trim() : q;
    }
}
