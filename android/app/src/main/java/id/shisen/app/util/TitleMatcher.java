package id.shisen.app.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import id.shisen.app.data.model.Anime;

public class TitleMatcher {

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "the", "a", "an", "of", "in", "to", "and", "und", "der", "die", "das",
            "no", "wa", "ga", "ni", "de", "series", "season", "s", "part", "cour", "stage",
            "1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th",
            "second", "third", "fourth", "fifth", "sixth",
            "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "ii", "iii", "iv", "v", "vi", "vii", "viii"
    ));

    private static final Set<String> MOVIE_KEYWORDS = new HashSet<>(Arrays.asList(
            "movie", "film", "gekijouban"
    ));

    private static final Set<String> OVA_KEYWORDS = new HashSet<>(Arrays.asList(
            "ova", "special", "oad"
    ));

    private static final Set<String> ARC_SUBTITLE_KEYWORDS = new HashSet<>(Arrays.asList(
            "arc", "saga", "hen", "tan", "new", "future", "conflict", "separation",
            "calamity", "shiken", "kouho", "stage", "chapter", "shou", "act", "cour"
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
        return s.replaceAll("(?i)\\b(tv|sub|dub|uncensored|censored)\\b|\\((?:tv|sub|dub|uncensored|censored)\\)|\\[(?:tv|sub|dub|uncensored|censored)\\]", "").trim();
    }

    public static String cleanSlug(String slug) {
        if (slug == null) return "";
        return slug.replaceAll("-\\d+$", "").trim();
    }

    private static String normalizeRomanUnicode(String s) {
        if (s == null) return "";
        return s.replace("\u2161", " II ").replace("\u2162", " III ").replace("\u2163", " IV ")
                .replace("\u2164", " V ").replace("\u2165", " VI ").replace("\u2166", " VII ")
                .replace("\u2167", " VIII ").replace("\u2171", " ii ").replace("\u2172", " iii ")
                .replace("\u2173", " iv ").replace("\u2174", " v ").replace("\u2175", " vi ");
    }

    public static String normalize(String s) {
        if (s == null) return "";
        String clean = normalizeRomanUnicode(s).toLowerCase(Locale.US).replace("&", "and");
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

    private static int wordToNumber(String w) {
        if (w == null) return 1;
        switch (w.toLowerCase(Locale.US)) {
            case "second": case "ii": case "ni": case "2nd": return 2;
            case "third": case "iii": case "san": case "3rd": return 3;
            case "fourth": case "iv": case "yon": case "shi": case "4th": return 4;
            case "fifth": case "v": case "go": case "5th": return 5;
            case "sixth": case "vi": case "roku": case "6th": return 6;
            case "seventh": case "vii": case "nana": case "7th": return 7;
            case "eighth": case "viii": case "hachi": case "8th": return 8;
            default:
                try { return Integer.parseInt(w.replaceAll("[^0-9]", "")); } catch (Exception e) { return 1; }
        }
    }

    /** Mengekstrak nomor Part / Cour eksplisit (default 1). */
    public static int extractPartNumber(String title) {
        if (title == null || title.isEmpty()) return 1;
        String t = normalizeRomanUnicode(title).toLowerCase(Locale.US);
        Matcher m1 = Pattern.compile("\\b(?:part|cour)\\s*(\\d+)\\b").matcher(t);
        if (m1.find()) {
            try { return Integer.parseInt(m1.group(1)); } catch (Exception ignored) {}
        }
        Matcher m2 = Pattern.compile("\\b(\\d+)(?:nd|rd|th|st)?\\s*(?:part|cour)\\b").matcher(t);
        if (m2.find()) {
            try { return Integer.parseInt(m2.group(1)); } catch (Exception ignored) {}
        }
        Matcher m3 = Pattern.compile("\\b(second|third|fourth|fifth)\\s+(?:part|cour)\\b").matcher(t);
        if (m3.find()) {
            return wordToNumber(m3.group(1));
        }
        if (t.contains("zenpen")) return 1;
        if (t.contains("kouhen")) return 2;
        return 1;
    }

    /** Mengekstrak nomor Season utama di luar Part/Cour. */
    public static int extractPrimarySeason(String title) {
        if (title == null || title.isEmpty()) return 1;
        String t = normalizeRomanUnicode(stripQualifiers(title)).toLowerCase(Locale.US);

        Matcher m1 = Pattern.compile("\\b(\\d+)(?:nd|rd|th|st)?\\s*(?:season|stage)\\b").matcher(t);
        if (m1.find()) {
            try { return Integer.parseInt(m1.group(1)); } catch (Exception ignored) {}
        }

        Matcher m2 = Pattern.compile("\\b(?:season|stage)\\s*(\\d+)\\b").matcher(t);
        if (m2.find()) {
            try { return Integer.parseInt(m2.group(1)); } catch (Exception ignored) {}
        }

        Matcher mWord = Pattern.compile("\\b(second|third|fourth|fifth|sixth|seventh|eighth)\\s+(?:season|stage)\\b").matcher(t);
        if (mWord.find()) {
            return wordToNumber(mWord.group(1));
        }

        Matcher mS = Pattern.compile("\\bs(\\d+)\\b").matcher(t);
        if (mS.find()) {
            try { return Integer.parseInt(mS.group(1)); } catch (Exception ignored) {}
        }

        Matcher mRoman = Pattern.compile("\\b(?:season\\s*)?(ii|iii|iv|v|vi|vii|viii)\\b").matcher(t);
        if (mRoman.find()) {
            return wordToNumber(mRoman.group(1));
        }

        Matcher mJp = Pattern.compile("\\b(?:(ni|san|yon|shi|go|roku)\\s+no\\s+shou|sono\\s+(ni|san|yon|go)|dai[-\\s]*(\\d+|ni|san|yon|go)[-\\s]*ki|([2-9])-ki)\\b").matcher(t);
        if (mJp.find()) {
            for (int g = 1; g <= mJp.groupCount(); g++) {
                if (mJp.group(g) != null) return wordToNumber(mJp.group(g));
            }
        }

        if (t.contains("final season")) return 4;
        if (t.contains("\u221aa")) return 2;

        // Ordinal di akhir judul: "... 2nd", "... 3rd"
        Matcher mOrdEnd = Pattern.compile("\\b([2-9])(?:nd|rd|th)\\s*$").matcher(t);
        if (mOrdEnd.find()) {
            try { return Integer.parseInt(mOrdEnd.group(1)); } catch (Exception ignored) {}
        }

        // Angka 2-9 di akhir judul (dengan spasi atau menempel pada kata >=4 huruf seperti "Rotten2"),
        // kecualikan bila didahului kata/simbol nomor bukan season.
        String tNoPart = t.replaceAll("\\b(?:(?:part|cour)\\s*\\d+|\\d+(?:nd|rd|th|st)?\\s*(?:part|cour))\\b", "").trim();
        Matcher mTrail = Pattern.compile("(?<!\\b(?:no\\.?|ep|episode|vol\\.?|volume|lv\\.?|level|part|cour|stage|rank|unit|ward|area|type|ver\\.?|mark|mk|apollo|route|room|ch|chapter))(?<![-–#0-9])(?:\\s|(?<=[a-z]{4}))([2-9])\\s*$").matcher(tNoPart);
        if (mTrail.find()) {
            try { return Integer.parseInt(mTrail.group(1)); } catch (Exception ignored) {}
        }

        return 1;
    }

    public static int extractSeasonNumber(String title) {
        int primary = extractPrimarySeason(title);
        if (primary > 1) return primary;
        int part = extractPartNumber(title);
        if (part > 1) return part;
        return 1;
    }

    public static int extractAnimeSeason(Anime a) {
        if (a == null) return 1;
        int s1 = extractSeasonNumber(a.title);
        int s2 = extractSeasonNumber(a.romajiTitle);
        int sRel = (a.relationLabel != null && a.relationLabel.toLowerCase(Locale.US).startsWith("season "))
                ? extractSeasonNumber(a.relationLabel) : 1;
        return Math.max(Math.max(s1, s2), sRel);
    }

    public static boolean isSeasonAndPartCompatible(String target, String candidate) {
        int sA = extractSeasonNumber(target);
        int sB = extractSeasonNumber(candidate);
        if ((sA > 1 || sB > 1) && sA != sB) return false;

        int primA = extractPrimarySeason(target);
        int primB = extractPrimarySeason(candidate);
        int partA = extractPartNumber(target);
        int partB = extractPartNumber(candidate);
        // Jika keduanya di Season N yang sama (prim > 1), tetapi salah satu Part > 1 (mis. Season 2 Part 2 vs Season 2), tolak
        if (primA > 1 && primB > 1 && primA == primB && partA != partB) {
            return false;
        }
        return true;
    }

    public static boolean isAnimeSeasonCompatible(Anime anime, Anime candidate) {
        if (anime == null || candidate == null) return false;
        int sA = extractAnimeSeason(anime);
        int sB = extractAnimeSeason(candidate);
        if ((sA > 1 || sB > 1) && sA != sB) return false;

        int primA = Math.max(extractPrimarySeason(anime.title), extractPrimarySeason(anime.romajiTitle));
        int primB = Math.max(extractPrimarySeason(candidate.title), extractPrimarySeason(candidate.romajiTitle));
        int partA = Math.max(extractPartNumber(anime.title), extractPartNumber(anime.romajiTitle));
        int partB = Math.max(extractPartNumber(candidate.title), extractPartNumber(candidate.romajiTitle));
        if (primA > 1 && primB > 1 && primA == primB && partA != partB) {
            return false;
        }
        return true;
    }

    public static Set<String> cleanTitleTokens(String title) {
        Set<String> tokens = new HashSet<>();
        if (title == null || title.isEmpty()) return tokens;

        String t = normalizeRomanUnicode(stripQualifiers(title))
                .replace("'", "").replace("\u2019", "")
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .toLowerCase(Locale.US);
        for (String tok : t.split("\\s+")) {
            if (tok.isEmpty()) continue;
            if ("ovas".equals(tok)) tok = "ova";
            else if ("specials".equals(tok)) tok = "special";
            else if ("movies".equals(tok)) tok = "movie";
            if (tok.length() >= 5 && Character.isDigit(tok.charAt(tok.length() - 1)) && !Character.isDigit(tok.charAt(tok.length() - 2))) {
                tok = tok.substring(0, tok.length() - 1);
            }
            tokens.add(tok);
        }
        return tokens;
    }

    private static Set<String> significantTokens(String title) {
        Set<String> sig = cleanTitleTokens(title);
        sig.removeAll(STOPWORDS);
        return sig;
    }

    private static String extractSubtitleAfterDelimiter(String title) {
        if (title == null) return "";
        int colon = title.indexOf(':');
        if (colon >= 0 && colon + 1 < title.length()) {
            return title.substring(colon + 1).trim();
        }
        int dash = title.indexOf(" - ");
        if (dash >= 0 && dash + 3 < title.length()) {
            return title.substring(dash + 3).trim();
        }
        return "";
    }

    public static boolean isTitleMatch(String target, String candidate) {
        if (target == null || candidate == null) return false;
        if (target.trim().isEmpty() || candidate.trim().isEmpty()) return false;

        // 1. Strict season & part consistency first
        if (!isSeasonAndPartCompatible(target, candidate)) {
            return false;
        }

        // 2. Strict year consistency if both explicitly provide a year
        int yA = extractYear(target);
        int yB = extractYear(candidate);
        if (yA != -1 && yB != -1 && yA != yB) {
            return false;
        }

        // 3. Contradictory Keyword Check
        if (hasContradictoryKeywords(target, candidate)) {
            return false;
        }

        // Punctuation sequel markers like "Love is War?" vs "Love is War"
        boolean qMarkA = target.contains("?");
        boolean qMarkB = candidate.contains("?");
        if (qMarkA != qMarkB && normalize(target).equals(normalize(candidate))) {
            return false;
        }

        // 4. Exact normalized / stripped qualifiers / clean slug match
        String normA = normalize(target);
        String normB = normalize(candidate);
        if (normA.equals(normB) && !normA.isEmpty()) {
            return true;
        }

        String strippedA = normalize(stripQualifiers(target));
        String strippedB = normalize(stripQualifiers(candidate));
        if (!strippedA.isEmpty() && strippedA.equals(strippedB)) {
            return true;
        }

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

        Set<String> tokensA = cleanTitleTokens(target);
        Set<String> tokensB = cleanTitleTokens(candidate);
        if (tokensA.isEmpty() || tokensB.isEmpty()) return false;

        // 5. Isolate Movie/Film vs TV Series
        boolean isMovieA = false;
        for (String m : MOVIE_KEYWORDS) { if (tokensA.contains(m)) { isMovieA = true; break; } }
        boolean isMovieB = false;
        for (String m : MOVIE_KEYWORDS) { if (tokensB.contains(m)) { isMovieB = true; break; } }
        if (isMovieA != isMovieB) return false;

        // 6. Isolate OVA/Special vs TV Series
        boolean isOvaA = false;
        for (String o : OVA_KEYWORDS) { if (tokensA.contains(o)) { isOvaA = true; break; } }
        boolean isOvaB = false;
        for (String o : OVA_KEYWORDS) { if (tokensB.contains(o)) { isOvaB = true; break; } }
        if (isOvaA != isOvaB) return false;

        // 7. Significant token overlap calculation
        Set<String> sigA = significantTokens(target);
        Set<String> sigB = significantTokens(candidate);

        Set<String> common = new HashSet<>(sigA);
        common.retainAll(sigB);

        if (sigA.isEmpty() || sigB.isEmpty()) {
            Set<String> allCommon = new HashSet<>(tokensA);
            allCommon.retainAll(tokensB);
            return ((double) allCommon.size() / Math.max(tokensA.size(), tokensB.size())) >= 0.75;
        }

        int sA = extractSeasonNumber(target);
        int sB = extractSeasonNumber(candidate);

        if (sigA.equals(sigB)) {
            return true;
        }

        // Special handling for prominent multi-word signature franchises like "Re:Zero" when season matches
        if (sA == sB && tokensA.contains("re") && tokensA.contains("zero") &&
            tokensB.contains("re") && tokensB.contains("zero")) {
            return true;
        }

        int nonGenericCommon = 0;
        for (String tok : common) {
            if (!GENERIC_ANIME_WORDS.contains(tok)) {
                nonGenericCommon++;
            }
        }

        if (sA == 1 && sB == 1) {
            // Cegah S2 ber-subjudul arc (tanpa angka season) mencocokkan diri dengan S1 atau arc lain
            Set<String> diff = new HashSet<>(sigA);
            diff.addAll(sigB);
            diff.removeAll(common);
            for (String d : diff) {
                if (ARC_SUBTITLE_KEYWORDS.contains(d)) {
                    return false;
                }
            }
            // Jika salah satu judul adalah subset murni dari judul lain dan selisihnya >= 2 kata signifikan,
            // itu indikasi kuat subjudul sekuel/arc (mis. "Dr. STONE" vs "Dr. STONE: NEW WORLD")
            if ((sigA.containsAll(sigB) || sigB.containsAll(sigA)) && diff.size() >= 2) {
                return false;
            }
            // Jika kedua judul punya subjudul setelah ':' atau ' - ', subjudulnya wajib memiliki kata signifikan yang sama
            String subA = extractSubtitleAfterDelimiter(target);
            String subB = extractSubtitleAfterDelimiter(candidate);
            if (!subA.isEmpty() && !subB.isEmpty()) {
                Set<String> sigSubA = significantTokens(subA);
                Set<String> sigSubB = significantTokens(subB);
                if (!sigSubA.isEmpty() && !sigSubB.isEmpty() && !sigSubA.equals(sigSubB)) {
                    return false;
                }
            }
            double r = (double) common.size() / Math.max(sigA.size(), sigB.size());
            return r >= 0.75 && common.size() >= 3 && nonGenericCommon >= 1;
        }

        double overlap = (double) common.size() / Math.max(sigA.size(), sigB.size());
        if (sA > 1 && sA == sB) {
            if ((sigA.containsAll(sigB) || sigB.containsAll(sigA)) && (nonGenericCommon >= 1 || common.size() >= 2)) {
                return true;
            }
            if (overlap >= 0.65 && (nonGenericCommon >= 1 || common.size() >= 2)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isMatchAny(Anime anime, Anime candidate) {
        if (anime == null || candidate == null) return false;
        if (!isAnimeSeasonCompatible(anime, candidate)) {
            return false;
        }
        String aTitle = anime.title;
        String aRomaji = anime.romajiTitle;
        String cTitle = candidate.title;
        String cRomaji = candidate.romajiTitle;

        int targetSeason = extractAnimeSeason(anime);
        int candSeason = extractAnimeSeason(candidate);

        if (isTitleMatchWithSeason(aTitle, cTitle, targetSeason, candSeason)) return true;
        if (aRomaji != null && !aRomaji.isEmpty() && isTitleMatchWithSeason(aRomaji, cTitle, targetSeason, candSeason)) return true;
        if (cRomaji != null && !cRomaji.isEmpty() && isTitleMatchWithSeason(aTitle, cRomaji, targetSeason, candSeason)) return true;
        if (aRomaji != null && !aRomaji.isEmpty() && cRomaji != null && !cRomaji.isEmpty() && isTitleMatchWithSeason(aRomaji, cRomaji, targetSeason, candSeason)) return true;

        return false;
    }

    private static boolean isTitleMatchWithSeason(String target, String candidate, int effectiveSeasonA, int effectiveSeasonB) {
        if (target == null || candidate == null) return false;
        if ((effectiveSeasonA > 1 || effectiveSeasonB > 1) && effectiveSeasonA != effectiveSeasonB) {
            return false;
        }
        if (effectiveSeasonA > 1 && effectiveSeasonA == effectiveSeasonB) {
            String augA = extractSeasonNumber(target) == effectiveSeasonA ? target : (target + " Season " + effectiveSeasonA);
            String augB = extractSeasonNumber(candidate) == effectiveSeasonB ? candidate : (candidate + " Season " + effectiveSeasonB);
            return isTitleMatch(augA, augB);
        }
        return isTitleMatch(target, candidate);
    }

    public static int matchScore(Anime anime, Anime candidate, String target, String alt) {
        if (candidate == null) return 0;
        Anime ref = anime;
        if (ref == null) {
            ref = new Anime();
            ref.title = target != null ? target : "";
            ref.romajiTitle = alt != null ? alt : "";
        }
        if (!isAnimeSeasonCompatible(ref, candidate)) {
            return 0;
        }
        if (!isMatchAny(ref, candidate)) {
            String slugClean = candidate.slug != null ? cleanSlug(candidate.slug).replace('-', ' ') : "";
            int sRef = extractAnimeSeason(ref);
            int sSlug = Math.max(extractAnimeSeason(candidate), extractSeasonNumber(slugClean));
            if ((sRef > 1 || sSlug > 1) && sRef != sSlug) return 0;
            boolean slugMatched = (target != null && isTitleMatchWithSeason(target, slugClean, sRef, sSlug))
                    || (alt != null && !alt.isEmpty() && isTitleMatchWithSeason(alt, slugClean, sRef, sSlug));
            if (!slugMatched) return 0;
        }

        String[] aTitles = {ref.title, ref.romajiTitle};
        String[] cTitles = {candidate.title, candidate.romajiTitle};
        int best = 50;
        for (String at : aTitles) {
            if (at == null || at.isEmpty()) continue;
            for (String ct : cTitles) {
                if (ct == null || ct.isEmpty()) continue;
                if (normalize(stripQualifiers(at)).equals(normalize(stripQualifiers(ct)))) {
                    return 100;
                }
                Set<String> sigA = significantTokens(at);
                Set<String> sigB = significantTokens(ct);
                if (!sigA.isEmpty() && sigA.equals(sigB)) {
                    best = Math.max(best, 90);
                } else if (!sigA.isEmpty() && !sigB.isEmpty()) {
                    Set<String> common = new HashSet<>(sigA);
                    common.retainAll(sigB);
                    int score = 50 + (int) (35.0 * common.size() / Math.max(sigA.size(), sigB.size()));
                    best = Math.max(best, score);
                }
            }
        }
        return best;
    }

    public static String pickBestSlug(List<Anime> candidates, Anime anime, String target, String alt) {
        if (candidates == null || candidates.isEmpty()) return null;
        String bestSlug = null;
        int bestScore = 0;

        for (Anime c : candidates) {
            if (c == null || c.slug == null || c.slug.isEmpty()) continue;
            int score = matchScore(anime, c, target, alt);
            if (score > bestScore) {
                bestScore = score;
                bestSlug = c.slug;
                if (score == 100) break;
            }
        }
        return bestSlug;
    }

    public static String cleanSearchQuery(String title) {
        if (title == null) return "";
        String q = title.replaceAll("(?i)(season\\s*\\d+|\\d+(?:nd|rd|th|st)\\s*season|part\\s*\\d+|cour\\s*\\d+|\\d+(?:nd|rd|th|st)\\s*cour)", " ");
        q = q.replace("'", "").replace("\u2019", "").replaceAll("[^a-zA-Z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        return q.isEmpty() ? title.trim() : q;
    }
}
