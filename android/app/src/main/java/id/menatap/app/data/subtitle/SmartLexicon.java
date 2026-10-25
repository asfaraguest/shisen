package id.menatap.app.data.subtitle;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmartLexicon {

    private static class Rule {
        final Pattern pattern;
        final String replacement;

        Rule(String regex, String replacement) {
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            this.replacement = replacement;
        }
    }

    private static final Pattern FORMAL_INDICATOR = Pattern.compile(
            "\\b(yang mulia|paduka|baginda|kaisar|permaisuri|tuan putri|pangeran|jenderal|komandan|menteri|istana dalam|selir utama)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final List<Rule> UNIVERSAL_RULES = new ArrayList<>();
    private static final List<Rule> CASUAL_RULES = new ArrayList<>();

    static {
        // --- 1. Universal Lexicon (Anime Expressions & Honorifics) ---
        addUniversal("\\bterima kasih banyak\\b", "makasih banyak");
        addUniversal("\\bterima kasih\\b", "makasih");
        addUniversal("\\bsama-sama\\b", "sama-sama");
        addUniversal("\\bmaafkan aku\\b", "maaf ya");
        addUniversal("\\bminta maaf\\b", "minta maaf");
        addUniversal("\\bmohon maaf\\b", "maaf ya");
        addUniversal("\\bpermisi\\b", "permisi");
        addUniversal("\\bapa kabar\\b", "gimana kabarnya");
        addUniversal("\\bselamat pagi\\b", "selamat pagi");
        addUniversal("\\bselamat malam\\b", "selamat malam");
        addUniversal("\\bsampai jumpa\\b", "sampai ketemu lagi");

        addUniversal("\\bistana belakang\\b", "Istana Dalam");
        addUniversal("\\bselir tingkat tinggi\\b", "Selir Utama");
        addUniversal("\\bpencicip makanan\\b", "Pencicip Racun");
        addUniversal("\\bapoteker istana\\b", "Tabib Istana");
        addUniversal("\\bsenjata sihir\\b", "Senjata Sihir");
        addUniversal("\\bmerapal mantra\\b", "merapal sihir");
        addUniversal("\\blingkaran sihir\\b", "lingkaran sihir");
        addUniversal("\\bbatu mana\\b", "Batu Mana");
        addUniversal("\\bkapasitas mana\\b", "kapasitas mana");
        addUniversal("\\btingkat sihir\\b", "tingkat sihir");

        // --- 2. Casual Indonesian Dialogue Rules ---
        addCasual("\\bsaya tidak tahu anda ada di sana\\b", "aku nggak tahu kamu ada di sana");
        addCasual("\\bsaya nggak tahu anda ada di sana\\b", "aku nggak tahu kamu ada di sana");
        addCasual("\\baku tidak tahu anda ada di sana\\b", "aku nggak tahu kamu ada di sana");
        addCasual("\\baku tidak melakukan apa pun\\b", "aku nggak ngelakuin apa-apa");
        addCasual("\\btidak melakukan apa pun\\b", "nggak ngelakuin apa-apa");
        addCasual("\\bingin saya mengajari anda\\b", "mau kuajari");
        addCasual("\\banda perlu baju ganti\\b", "kamu butuh baju ganti");
        addCasual("\\bapa yang kamu bicarakan\\b", "ngomong apa sih kamu");
        addCasual("\\bapa yang kau bicarakan\\b", "ngomong apa sih kamu");
        addCasual("\\bapa maksudmu\\b", "maksudmu apa");
        addCasual("\\bapa yang kamu maksud\\b", "maksudmu apa");
        addCasual("\\bapa yang terjadi\\b", "ada apa sih");
        addCasual("\\bapa-apaan ini\\b", "ada apa-apaan nih");
        addCasual("\\bapa maumu\\b", "mau apa sih kamu");
        addCasual("\\bapa yang kamu inginkan\\b", "kamu mau apa sih");
        addCasual("\\bmana aku tahu\\b", "mana kutahu");
        addCasual("\\bbagaimana aku bisa tahu\\b", "mana kutahu");

        addCasual("\\baku melihat\\b", "begitu rupanya");
        addCasual("\\bkulihat\\b", "begitu rupanya");
        addCasual("\\btidak apa-apa\\b", "nggak apa-apa");
        addCasual("\\btak apa-apa\\b", "nggak apa-apa");
        addCasual("\\btak apa\\b", "nggak apa-apa");
        addCasual("\\bbaik-baik saja\\b", "nggak apa-apa");
        addCasual("\\btutup mulutmu\\b", "diem");
        addCasual("\\btidak ada jalan\\b", "nggak mungkin");
        addCasual("\\btidak mungkin\\b", "nggak mungkin");
        addCasual("\\btak mungkin\\b", "nggak mungkin");
        addCasual("\\btidak bisa\\b", "nggak bisa");
        addCasual("\\btak bisa\\b", "nggak bisa");
        addCasual("\\btidak dapat\\b", "nggak bisa");
        addCasual("\\btidak akan\\b", "nggak bakal");
        addCasual("\\btak akan\\b", "nggak bakal");
        addCasual("\\btidak tahu\\b", "nggak tahu");
        addCasual("\\btak tahu\\b", "nggak tahu");
        addCasual("\\btidak mau\\b", "nggak mau");
        addCasual("\\btak mau\\b", "nggak mau");
        addCasual("\\btidak pernah\\b", "nggak pernah");
        addCasual("\\btak pernah\\b", "nggak pernah");
        addCasual("\\btidak perlu\\b", "nggak usah");
        addCasual("\\btak perlu\\b", "nggak usah");
        addCasual("\\btidak usah\\b", "nggak usah");
        addCasual("\\btidak boleh\\b", "nggak boleh");
        addCasual("\\btak boleh\\b", "nggak boleh");
        addCasual("\\btidak ada\\b", "nggak ada");
        addCasual("\\btak ada\\b", "nggak ada");

        addCasual("\\bhanya saja\\b", "cuma");
        addCasual("\\bhanya\\b", "cuma");
        addCasual("\\bsudah\\b", "udah");
        addCasual("\\bsaja\\b", "aja");
        addCasual("\\bmengapa kamu\\b", "kenapa kamu");
        addCasual("\\bmengapa kau\\b", "kenapa kamu");
        addCasual("\\bmengapa dia\\b", "kenapa dia");
        addCasual("\\bmengapa\\b", "kenapa");
        addCasual("\\bbagaimana kalau\\b", "gimana kalau");
        addCasual("\\bbagaimana jika\\b", "gimana kalau");
        addCasual("\\bbagaimana cara\\b", "gimana cara");
        addCasual("\\bbagaimana bisa\\b", "gimana bisa");

        addCasual("\\blelaki itu\\b", "cowok itu");
        addCasual("\\bpria itu\\b", "cowok itu");
        addCasual("\\bwanita itu\\b", "cewek itu");
        addCasual("\\bgadis itu\\b", "cewek itu");
        addCasual("\\bjangan khawatir\\b", "tenang aja");
        addCasual("\\bdengarkan aku\\b", "dengerin aku");
        addCasual("\\bdengarkan\\b", "dengerin");
        addCasual("\\bbodoh sekali\\b", "bodoh banget");
        addCasual("\\bbagus sekali\\b", "hebat banget");
        addCasual("\\bhebat sekali\\b", "hebat banget");
        addCasual("\\bmudah sekali\\b", "gampang banget");
        addCasual("\\bgampang sekali\\b", "gampang banget");
        addCasual("\\bsangat keren\\b", "keren banget");
        addCasual("\\bsangat manis\\b", "manis banget");
        addCasual("\\bsangat imut\\b", "imut banget");
        addCasual("\\bsangat lezat\\b", "enak banget");
        addCasual("\\bsangat enak\\b", "enak banget");
        addCasual("\\bsangat lelah\\b", "capek banget");
        addCasual("\\bsangat lapar\\b", "laper banget");
        addCasual("\\bbeneran nih\\b", "beneran nih");
        addCasual("\\bpantas saja\\b", "pantesan");
        addCasual("\\bjangan bercanda\\b", "jangan bercanda deh");
        addCasual("\\bjangan konyol\\b", "yang bener aja");
        addCasual("\\baku mengerti\\b", "aku paham");
        addCasual("\\baku tidak mengerti\\b", "aku nggak ngerti");
    }

    private static void addUniversal(String regex, String repl) {
        UNIVERSAL_RULES.add(new Rule(regex, repl));
    }

    private static void addCasual(String regex, String repl) {
        CASUAL_RULES.add(new Rule(regex, repl));
    }

    public static String applyUniversalLexicon(String text) {
        if (text == null || text.isEmpty()) return "";
        String result = text;
        for (Rule rule : UNIVERSAL_RULES) {
            result = applyRulePreservingCase(rule, result);
        }
        return result;
    }

    public static String sanitizeGtxPlusId(String text, boolean forceFormal) {
        if (text == null || text.isEmpty()) return "";
        // 1. Terapkan istilah universal
        String result = applyUniversalLexicon(text);

        // 2. Deteksi adegan formal/kerajaan
        boolean isFormal = forceFormal || FORMAL_INDICATOR.matcher(result).find();
        if (!isFormal) {
            for (Rule rule : CASUAL_RULES) {
                result = applyRulePreservingCase(rule, result);
            }
        }
        return result;
    }

    private static String applyRulePreservingCase(Rule rule, String input) {
        Matcher matcher = rule.pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String match = matcher.group();
            String repl = rule.replacement;
            if (Character.isUpperCase(match.charAt(0)) && !repl.isEmpty()) {
                repl = Character.toUpperCase(repl.charAt(0)) + (repl.length() > 1 ? repl.substring(1) : "");
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(repl));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
