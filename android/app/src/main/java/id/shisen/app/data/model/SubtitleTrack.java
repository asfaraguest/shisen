package id.shisen.app.data.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SubtitleTrack implements Serializable {
    public String url = "";
    public String language = "id";
    public String lang = "id";
    public String label = "Indonesian";
    public boolean isDefault = true;
    public String referer = "";

    public SubtitleTrack(String url, String language, String label, boolean isDefault) {
        this(url, language, label, isDefault, "");
    }

    public SubtitleTrack(String url, String language, String label, boolean isDefault, String referer) {
        this.url = url;
        this.language = language;
        this.lang = language;
        this.label = label;
        this.isDefault = isDefault;
        this.referer = referer != null ? referer : "";
    }

    private static final List<String> NON_ENGLISH_KEYWORDS = Arrays.asList(
            "arabic", "arab", "indonesia", "bahasa", "spanish", "espanol", "español",
            "portuguese", "português", "french", "français", "francais", "german", "deutsch",
            "italian", "italiano", "korean", "chinese", "russian", "thai", "vietnamese",
            "turkish", "hindi", "czech", "danish", "dutch", "finnish", "greek",
            "hungarian", "norwegian", "polish", "romanian", "slovak", "swedish",
            "malay", "tagalog", "filipino", "ukrainian", "bulgarian", "croatian",
            "japanese", "japan"
    );

    public static String inferLanguage(String label, String rawLang) {
        String lab = label != null ? label.trim().toLowerCase(Locale.US) : "";
        String rl = rawLang != null ? rawLang.trim().toLowerCase(Locale.US) : "";

        // 1. Cek dari label terlebih dahulu
        String inferred = labelToLang(lab);
        if (!inferred.isEmpty()) {
            return inferred;
        }

        // 2. Jika label tidak mengandung kata kunci non-Inggris, boleh gunakan rawLang
        boolean hasNonEnglishKeyword = false;
        for (String kw : NON_ENGLISH_KEYWORDS) {
            if (lab.contains(kw)) {
                hasNonEnglishKeyword = true;
                break;
            }
        }

        if (!hasNonEnglishKeyword && !rl.isEmpty()) {
            String rlInferred = labelToLang(rl);
            if (!rlInferred.isEmpty()) {
                return rlInferred;
            }
            if (rl.length() == 2) {
                return rl;
            }
        }

        // Default ke en jika tidak diketahui sama sekali dan tidak ada indikasi non-Inggris
        return hasNonEnglishKeyword ? "" : "en";
    }

    private static String labelToLang(String lab) {
        if (lab.isEmpty()) return "";
        if (lab.contains("english") || lab.startsWith("eng") || lab.equals("en")) return "en";
        if (lab.contains("indonesia") || lab.contains("bahasa") || lab.startsWith("ind") || lab.equals("id")) return "id";
        if (lab.contains("arabic") || lab.contains("arab") || lab.startsWith("ara") || lab.equals("ar")) return "ar";
        if (lab.contains("japanese") || lab.contains("japan") || lab.startsWith("jpn") || lab.equals("ja")) return "ja";
        if (lab.contains("spanish") || lab.contains("espanol") || lab.contains("español") || lab.startsWith("spa") || lab.equals("es")) return "es";
        if (lab.contains("portuguese") || lab.contains("português") || lab.startsWith("por") || lab.equals("pt")) return "pt";
        if (lab.contains("french") || lab.contains("français") || lab.contains("francais") || lab.startsWith("fra") || lab.startsWith("fre") || lab.equals("fr")) return "fr";
        if (lab.contains("german") || lab.contains("deutsch") || lab.startsWith("ger") || lab.startsWith("deu") || lab.equals("de")) return "de";
        if (lab.contains("italian") || lab.contains("italiano") || lab.startsWith("ita") || lab.equals("it")) return "it";
        if (lab.contains("korean") || lab.startsWith("kor") || lab.equals("ko")) return "ko";
        if (lab.contains("chinese") || lab.startsWith("chi") || lab.startsWith("zho") || lab.equals("zh")) return "zh";
        if (lab.contains("russian") || lab.startsWith("rus") || lab.equals("ru")) return "ru";
        if (lab.contains("thai") || lab.startsWith("tha") || lab.equals("th")) return "th";
        if (lab.contains("vietnamese") || lab.startsWith("vie") || lab.equals("vi")) return "vi";
        if (lab.contains("turkish") || lab.startsWith("tur") || lab.equals("tr")) return "tr";
        if (lab.contains("hindi") || lab.startsWith("hin") || lab.equals("hi")) return "hi";
        if (lab.contains("czech") || lab.startsWith("cze") || lab.startsWith("ces") || lab.equals("cs")) return "cs";
        if (lab.contains("danish") || lab.startsWith("dan") || lab.equals("da")) return "da";
        if (lab.contains("dutch") || lab.startsWith("nld") || lab.startsWith("dut") || lab.equals("nl")) return "nl";
        if (lab.contains("finnish") || lab.startsWith("fin") || lab.equals("fi")) return "fi";
        if (lab.contains("greek") || lab.startsWith("ell") || lab.startsWith("gre") || lab.equals("el")) return "el";
        if (lab.contains("hungarian") || lab.startsWith("hun") || lab.equals("hu")) return "hu";
        if (lab.contains("norwegian") || lab.startsWith("nor") || lab.startsWith("nob") || lab.equals("no")) return "no";
        if (lab.contains("polish") || lab.startsWith("pol") || lab.equals("pl")) return "pl";
        if (lab.contains("romanian") || lab.startsWith("ron") || lab.startsWith("rum") || lab.equals("ro")) return "ro";
        if (lab.contains("slovak") || lab.startsWith("slk") || lab.startsWith("slo") || lab.equals("sk")) return "sk";
        if (lab.contains("swedish") || lab.startsWith("swe") || lab.equals("sv")) return "sv";
        if (lab.contains("malay") || lab.startsWith("msa") || lab.startsWith("may") || lab.equals("ms")) return "ms";
        if (lab.contains("tagalog") || lab.contains("filipino") || lab.startsWith("fil") || lab.equals("tl")) return "tl";
        if (lab.contains("ukrainian") || lab.startsWith("ukr") || lab.equals("uk")) return "uk";
        return "";
    }

    public static boolean isArabic(SubtitleTrack track) {
        if (track == null) return false;
        String l = track.lang != null ? track.lang.trim().toLowerCase(Locale.US) : "";
        String lbl = track.label != null ? track.label.trim().toLowerCase(Locale.US) : "";
        return "ar".equals(l) || "ara".equals(l) || lbl.contains("arabic") || lbl.contains("arab");
    }

    public static boolean isIndonesian(SubtitleTrack track) {
        if (track == null) return false;
        String l = track.lang != null ? track.lang.trim().toLowerCase(Locale.US) : "";
        String lbl = track.label != null ? track.label.trim().toLowerCase(Locale.US) : "";
        return "id".equals(l) || "ind".equals(l) || lbl.contains("indonesia") || lbl.contains("bahasa");
    }

    public static boolean isExactEnglish(SubtitleTrack track) {
        if (track == null || isArabic(track)) return false;
        String lbl = track.label != null ? track.label.trim().toLowerCase(Locale.US) : "";
        String l = track.lang != null ? track.lang.trim().toLowerCase(Locale.US) : "";
        return lbl.equals("english") || lbl.equals("english [full]") || lbl.equals("english (full)") || lbl.equals("eng")
                || ("en".equals(l) && lbl.equals("english"));
    }

    public static boolean isAnyEnglish(SubtitleTrack track) {
        if (track == null || isArabic(track)) return false;
        String lbl = track.label != null ? track.label.trim().toLowerCase(Locale.US) : "";
        String l = track.lang != null ? track.lang.trim().toLowerCase(Locale.US) : "";
        for (String kw : NON_ENGLISH_KEYWORDS) {
            if (!kw.equals("english") && !kw.equals("japan") && lbl.contains(kw)) {
                return false;
            }
        }
        return "en".equals(l) || lbl.contains("english") || lbl.startsWith("eng");
    }

    /**
     * Cari track English paling representatif dari daftar subtitle stream.
     * Paritas Tatap backend: Jangan izinkan track Arabic membajak seleksi English.
     */
    public static SubtitleTrack findEnglishTrack(List<SubtitleTrack> tracks) {
        if (tracks == null || tracks.isEmpty()) return null;

        // Prioritas 1: Exact "English" label
        for (SubtitleTrack t : tracks) {
            if (isExactEnglish(t)) return t;
        }

        // Prioritas 2: Any English track (e.g. "English - Signs", default flag)
        for (SubtitleTrack t : tracks) {
            if (isAnyEnglish(t)) return t;
        }

        // Prioritas 3: Track non-Arabic pertama
        for (SubtitleTrack t : tracks) {
            if (!isArabic(t)) return t;
        }

        // Prioritas 4: Fallback terakhir jika semua track adalah Arabic
        return tracks.get(0);
    }
}
