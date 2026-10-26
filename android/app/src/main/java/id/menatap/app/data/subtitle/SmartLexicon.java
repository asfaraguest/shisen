package id.menatap.app.data.subtitle;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SmartLexicon - Kamus Leksikon Universal & Subtitle Sanitizer Shisen
 * Paritas Penuh Subtitle Engine Tatap (610+ Aturan Universal, Koreksi Overgeared Satisfy,
 * Diksi Kasual Fansub, AIGTX Smooth Rules, dan Otomasi Kata Ganti Scene Context).
 */
public class SmartLexicon {

    public interface RuleReplacer {
        String replace(Matcher matcher);
    }

    private static class Rule {
        final Pattern pattern;
        final String replacement;
        final RuleReplacer replacer;

        Rule(String regex, String replacement) {
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            this.replacement = replacement;
            this.replacer = null;
        }

        Rule(String regex, RuleReplacer replacer) {
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            this.replacement = null;
            this.replacer = replacer;
        }
    }

    public static final Pattern FORMAL_INDICATORS_RE = Pattern.compile(
        "(?:\\b(jenderal|komandan|panglima|marsekal|laksamana|letnan|kapten|mayor|kolonel|sersan|kopral|ajudan|perwira|inspektur|batalyon|batalion|regimen|divisi|peleton|brigade|skuadron|armada|infantri|kavaleri|artileri|markas besar|pos komando|pos terdepan|garis depan|garis belakang|garis pertahanan|benteng pertahanan|koordinat|radar|evakuasi|gencatan senjata|penyerahan tanpa syarat|serangan udara|serangan fajar|manuver tempur|formasi tempur|protokol darurat|protokol keamanan|pengadilan militer|dewan militer|otoritas militer|pasukan pengintai|pasukan pertahanan|pasukan penyerang|baret|peluru kendali|rudal|peluncur roket|amunisi|senjata api|siap laksanakan|lapor, komandan)\\b)|(?:\\b(paduka|yang mulia|baginda|hamba|tuan|nyonya|tuan putri|putri mahkota|pangeran|pangeran mahkota|kaisar|permaisuri|ratu|raja|nobilitas|kaum bangsawan|bangsawan|adipati|duke|duchess|baron|baroness|marquis|marquess|count|viscount|lord|lady|nona besar|tuan muda|tuanku|takhta|istana kekaisaran|dewan kekaisaran|titah baginda|titah kaisar|uskup agung|paus|kardinal|pendeta agung|santo|santa|kuil suci|ksatria suci|kesatria kekaisaran|sihir terlarang|sihir agung)\\b)|(?:\\b(siaran langsung|pemirsa|bintang tamu|hadirin|yang terhormat|dewi rebecca|dewa yatan|earl steim|earl ashur|paus drevigo)\\b)",
        Pattern.CASE_INSENSITIVE
    );

    public static final Pattern RAGE_OUTBURST_RE = Pattern.compile(
        "\\b(sial|sialan|keparat|brengsek|bajingan|mampus|gila|bodoh|mati kau|habisi|bunuh|bangsat|persetan)\\b|[!?]{2,}",
        Pattern.CASE_INSENSITIVE
    );

    public static final Pattern MONOLOGUE_RE = Pattern.compile(
        "^\\s*[\\(\\[\\*].*[\\)\\]\\*]\\s*$"
    );

    private static final List<Rule> UNIVERSAL_RULES = new ArrayList<>();
    private static final List<Rule> CASUAL_RULES = new ArrayList<>();
    private static final List<Rule> AIGTX_SMOOTH_RULES = new ArrayList<>();
    private static final List<Rule> CASUAL_PRONOUN_RULES = new ArrayList<>();

    static {
        initOvergearedAndGameRules();
        initUniversalBatch1();
        initUniversalBatch2();
        initUniversalBatch3();
        initUniversalBatch4();
        initUniversalBatch5();
        initUniversalBatch6();
        initUniversalBatch7();
        initCasualBatch1();
        initCasualBatch2();
        initAigtxSmooth();
        initCasualPronouns();
    }

    private static void initOvergearedAndGameRules() {
        // --- Proteksi & Restorasi Game Satisfy (Anime Overgeared) ---
        UNIVERSAL_RULES.add(new Rule("(?i)\\bselamat\\s+datang\\s+di\\s+(?:memuaskan|puaskan|puas|kepuasan|pemuasan)\\b", "Selamat datang di Satisfy"));
        UNIVERSAL_RULES.add(new Rule("(?i)\\bkompetisi\\s+nasional\\s+(?:memuaskan|puaskan|puas|kepuasan|pemuasan)\\b", "Kompetisi Nasional Satisfy"));
        UNIVERSAL_RULES.add(new Rule("(?i)\\b(?:puaskan|memuaskan|puas|kepuasan|pemuasan),?\\s+((?:(?:sebuah\\s+)?(?:gim|game|dunia|realitas (?:virtual|maya)|vrmmo|permainan))\\b)", m -> "Satisfy, " + m.group(1)));
        UNIVERSAL_RULES.add(new Rule("(?i)\\b(?:puaskan|memuaskan|puas|kepuasan|pemuasan)\\s+((?:pengguna|user|pemain|akun|karakter|server|kapsul|peringkat|item|quest|dunia)\\b)", m -> "Satisfy " + m.group(1)));
        UNIVERSAL_RULES.add(new Rule("(?i)\\b(?:berita|warta|kabar|saluran|siaran|tayangan|acara|koran|harian)\\s+(?:yang\\s+)?(?:memuaskan|puaskan|puas|kepuasan|pemuasan)\\b", m -> m.group(0).replaceAll("(?i)\\s+(?:yang\\s+)?(?:memuaskan|puaskan|puas|kepuasan|pemuasan)$", " Satisfy")));
        UNIVERSAL_RULES.add(new Rule("(?i)\\b(?:memuaskan|puaskan|puas|kepuasan|pemuasan)\\s+(times|news|every|harian|koran|berita)\\b", m -> "Satisfy " + m.group(1)));
        UNIVERSAL_RULES.add(new Rule("(?i)\\bsatisfy\\s+(?:berita|warta)\\b", "Berita Satisfy"));
        UNIVERSAL_RULES.add(new Rule("(?i)\\b(?:gim|game|dunia|bermain|main|mainkan|pemain|pengguna|user|peringkat|sejarah|server|masuk ke|log in ke|login ke|terkoneksi ke|sambungkan ke|hubungkan ke|mengakses|akun|karakter|kapsul|di|ke|dalam|di dalam|tentang|soal|seputar)\\s+(?:memuaskan|puaskan|puas|kepuasan|pemuasan)\\b", m -> m.group(0).replaceAll("(?i)\\s+(?:memuaskan|puaskan|puas|kepuasan|pemuasan)$", " Satisfy")));
        UNIVERSAL_RULES.add(new Rule("(?i)\\b(?:tidak|bukan)\\s+(?:memuaskan|puaskan|puas|kepuasan)(!|\\b)", m -> "Bukan Satisfy" + (m.group(1).equals("!") ? "!" : "")));
        UNIVERSAL_RULES.add(new Rule("(?i)\\b(?:puaskan|memuaskan|puas)!\\s*(siap|ready)\\b", "Satisfy! Siap"));
        UNIVERSAL_RULES.add(new Rule("(?i)^\\s*([-–—]?\\s*)(?:Puaskan|Memuaskan|Kepuasan)\\s*([!.]*)\\s*$", m -> (m.group(1) != null ? m.group(1) : "") + "Satisfy" + (m.group(2) != null && !m.group(2).isEmpty() ? m.group(2) : "!")));
        UNIVERSAL_RULES.add(new Rule("(?i)\\b(?:memuaskan|puaskan|kepuasan)\\s+(?:adalah|merupakan)\\b", "Satisfy adalah"));

        // --- Judul-Judul Game & Anime Populer ---
        UNIVERSAL_RULES.add(new Rule("\\bseni pedang (?:daring|online)\\b", "Sword Art Online"));
        UNIVERSAL_RULES.add(new Rule("\\b(?:badai|angin kencang) senjata (?:daring|online)\\b", "Gun Gale Online"));
        UNIVERSAL_RULES.add(new Rule("\\balfheim (?:daring|online)\\b", "ALfheim Online"));
        UNIVERSAL_RULES.add(new Rule("\\bperbatasan shangri-?la\\b", "Shangri-La Frontier"));
        UNIVERSAL_RULES.add(new Rule("\\bdunia baru (?:daring|online)\\b", "NewWorld Online"));
        UNIVERSAL_RULES.add(new Rule("\\bledakan otak\\b", "Brain Burst"));
        UNIVERSAL_RULES.add(new Rule("\\b(?:kisah|cerita) (?:tua|tetua)\\b", "Elder Tale"));
        UNIVERSAL_RULES.add(new Rule("\\bdendrogram tak terbatas\\b", "Infinite Dendrogram"));
        UNIVERSAL_RULES.add(new Rule("\\b(?:gim|game|dunia|pemain|pemain pro)\\s+(?:kejayaan|kemuliaan)\\b", m -> m.group(0).replaceAll("(?i)\\s+(?:kejayaan|kemuliaan)$", " Glory")));
        UNIVERSAL_RULES.add(new Rule("\\blamunan silang\\b", "Cross Reverie"));
        UNIVERSAL_RULES.add(new Rule("\\bpenyelaman penuh\\b", "Full Dive"));
        UNIVERSAL_RULES.add(new Rule("\\bpenyelam penuh\\b", "Full Diver"));
        UNIVERSAL_RULES.add(new Rule("\\bskala ordinal\\b", "Ordinal Scale"));
        UNIVERSAL_RULES.add(new Rule("\\bproyek alisisasi\\b", "Project Alicization"));
    }

    private static void initUniversalBatch1() {
        UNIVERSAL_RULES.add(new Rule("\\bapakah kamu\\b", "kamu"));
        UNIVERSAL_RULES.add(new Rule("\\bapakah kau\\b", "kamu"));
        UNIVERSAL_RULES.add(new Rule("\\bapakah anda\\b", "Anda"));
        UNIVERSAL_RULES.add(new Rule("\\bapakah dia\\b", "dia"));
        UNIVERSAL_RULES.add(new Rule("\\bapakah mereka\\b", "mereka"));
        UNIVERSAL_RULES.add(new Rule("\\bapakah kita\\b", "kita"));
        UNIVERSAL_RULES.add(new Rule("\\bapakah ini\\b", "ini"));
        UNIVERSAL_RULES.add(new Rule("\\bapakah itu\\b", "itu"));
        UNIVERSAL_RULES.add(new Rule("\\bapakah ada\\b", "ada"));
        UNIVERSAL_RULES.add(new Rule("\\bapakah bisa\\b", "bisa"));
        UNIVERSAL_RULES.add(new Rule("\\bapakah benar\\b", "beneran"));
        UNIVERSAL_RULES.add(new Rule("\\bapakah sungguh\\b", "beneran"));
        UNIVERSAL_RULES.add(new Rule("\\bterkutuklah\\b", "sial"));
        UNIVERSAL_RULES.add(new Rule("\\bbawalah itu\\b", "sini maju"));
        UNIVERSAL_RULES.add(new Rule("\\bpenjara bawah tanah\\b", "dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\bserikat petualang\\b", "guild petualang"));
        UNIVERSAL_RULES.add(new Rule("\\bbanjir tinggi\\b", "incaranmu tinggi juga"));
        UNIVERSAL_RULES.add(new Rule("\\bsangat embarrassing\\b", "memalukan banget"));
        UNIVERSAL_RULES.add(new Rule("\\bembarrassing\\b", "memalukan"));
        UNIVERSAL_RULES.add(new Rule("\\bdiharamkan masuk\\b", "dilarang masuk"));
        UNIVERSAL_RULES.add(new Rule("\\bdi mata pertama\\b", "pada pandangan pertama"));
        UNIVERSAL_RULES.add(new Rule("\\bmata pertama\\b", "pandangan pertama"));
        UNIVERSAL_RULES.add(new Rule("\\bngeplug\\b", "menutup"));
        UNIVERSAL_RULES.add(new Rule("\\bngegoblok\\b", "bertingkah konyol"));
        UNIVERSAL_RULES.add(new Rule("\\bkelewatan satu sama lain\\b", "berpapasan"));
        UNIVERSAL_RULES.add(new Rule("\\bberapa curiga itu\\b", "mencurigakan banget, kan"));
        UNIVERSAL_RULES.add(new Rule("\\bberapa curiganya\\b", "mencurigakan"));
        UNIVERSAL_RULES.add(new Rule("\\bberapa curiga\\b", "mencurigakan"));
        UNIVERSAL_RULES.add(new Rule("\\bberapa lama aku keluar\\b", "berapa lama aku pingsan"));
        UNIVERSAL_RULES.add(new Rule("\\bduit jahat\\b", "untung besar"));
        UNIVERSAL_RULES.add(new Rule("\\buang jahat\\b", "untung besar"));
        UNIVERSAL_RULES.add(new Rule("\\bdengan kecepatan kayak gini\\b", "kalau begini terus"));
        UNIVERSAL_RULES.add(new Rule("\\btinggalkan wajah\\b", "terlepas dari wajah"));
        UNIVERSAL_RULES.add(new Rule("\\bbasis rahasia\\b", "markas rahasia"));
        UNIVERSAL_RULES.add(new Rule("\\blihat\\? katanya\\b", "tuh, kan! Apa kubilang"));
        UNIVERSAL_RULES.add(new Rule("\\bkatanya\\! kamu harus santai\\b", "apa kubilang! Kamu harus santai"));
        UNIVERSAL_RULES.add(new Rule("\\bngomong balas\\b", "membantah"));
        UNIVERSAL_RULES.add(new Rule("\\bbertembung kepala\\b", "berselisih"));
        UNIVERSAL_RULES.add(new Rule("\\bbertembung\\b", "bentrok"));
        UNIVERSAL_RULES.add(new Rule("\\bberputus di neraka\\b", "mampus di neraka"));
        UNIVERSAL_RULES.add(new Rule("\\banjing campuran\\b", "anjing buduk"));
        UNIVERSAL_RULES.add(new Rule("\\bkemarahan kerajaan\\b", "mengamuk hebat"));
        UNIVERSAL_RULES.add(new Rule("\\bngelakuin luka\\b", "melukai"));
        UNIVERSAL_RULES.add(new Rule("\\bmenghapus anda\\b", "menghabisi Anda"));
        UNIVERSAL_RULES.add(new Rule("\\bmenghapus kamu\\b", "menghabisi kamu"));
        UNIVERSAL_RULES.add(new Rule("\\bpenipu optik\\b", "umpan optik"));
        UNIVERSAL_RULES.add(new Rule("\\bkorpul\\b", "kopral"));
        UNIVERSAL_RULES.add(new Rule("\\bsanggah, ma'am\\b", "siap, Ma'am"));
        UNIVERSAL_RULES.add(new Rule("\\bsanggah, sir\\b", "siap, Sir"));
        UNIVERSAL_RULES.add(new Rule("\\bkalian ngapain ngapain\\b", "kalian ngerjain aku ya"));
        UNIVERSAL_RULES.add(new Rule("\\bgak ada cara aku ngelakuin itu\\b", "mana sudi aku ngelakuin itu"));
        UNIVERSAL_RULES.add(new Rule("\\bgak ada cara aku ngelakuin\\b", "mana sudi aku ngelakuin"));
        UNIVERSAL_RULES.add(new Rule("\\bdasar kau pervert\\b", "dasar mesum"));
        UNIVERSAL_RULES.add(new Rule("\\bdasar kamu pervert\\b", "dasar mesum"));
        UNIVERSAL_RULES.add(new Rule("\\bkau pervert\\b", "dasar mesum"));
        UNIVERSAL_RULES.add(new Rule("\\bkamu pervert\\b", "dasar mesum"));
        UNIVERSAL_RULES.add(new Rule("\\bpervert\\b", "mesum"));
        UNIVERSAL_RULES.add(new Rule("\\bpembunuh anak kecil\\b", "pembunuh kekanak-kanakan"));
        UNIVERSAL_RULES.add(new Rule("\\bsatu-satunya jahat yang tersisa\\b", "satu-satunya penjahat yang tersisa"));
        UNIVERSAL_RULES.add(new Rule("\\bsatu-satunya jahat\\b", "satu-satunya penjahat"));
        UNIVERSAL_RULES.add(new Rule("\\bmelihatmu dieksekusi\\b", "memastikanmu dieksekusi"));
        UNIVERSAL_RULES.add(new Rule("\\blantai bawah dua\\b", "lantai bawah tanah kedua"));
        UNIVERSAL_RULES.add(new Rule("\\bmorgue\\b", "kamar jenazah"));
        UNIVERSAL_RULES.add(new Rule("\\bjangan bilang itu di luar\\b", "jangan katakan itu keras-keras"));
        UNIVERSAL_RULES.add(new Rule("\\bngelupas blok\\b", "menerobos blok"));
        UNIVERSAL_RULES.add(new Rule("\\bngeliat di depan aku\\b", "menjulang di depanku"));
        UNIVERSAL_RULES.add(new Rule("\\bngeliat di depanku\\b", "menjulang di depanku"));
        UNIVERSAL_RULES.add(new Rule("\\bdi kolam darah\\b", "bersimbah darah"));
        UNIVERSAL_RULES.add(new Rule("\\bterbaring di kolam darah\\b", "terbaring bersimbah darah"));
        UNIVERSAL_RULES.add(new Rule("\\bdisayat sampai mati\\b", "ditusuk sampai mati"));
        UNIVERSAL_RULES.add(new Rule("\\bmenegaskan nasib kita\\b", "menentukan nasib kita"));
        UNIVERSAL_RULES.add(new Rule("\\bmenegaskan nasib\\b", "menentukan nasib"));
        UNIVERSAL_RULES.add(new Rule("\\btutup nasib kita\\b", "menentukan nasib kita"));
        UNIVERSAL_RULES.add(new Rule("\\btutup nasib\\b", "menentukan nasib"));
        UNIVERSAL_RULES.add(new Rule("\\bmenegaskan begitu banyak hantu\\b", "menyegel begitu banyak hantu"));
        UNIVERSAL_RULES.add(new Rule("\\bmenegaskan banyak hantu\\b", "menyegel banyak hantu"));
        UNIVERSAL_RULES.add(new Rule("\\btutup banyak hantu\\b", "menyegel banyak hantu"));
        UNIVERSAL_RULES.add(new Rule("\\bngendang roh jahat supaya tidur\\b", "menenangkan roh jahat"));
        UNIVERSAL_RULES.add(new Rule("\\bngendang roh\\b", "menenangkan roh"));
        UNIVERSAL_RULES.add(new Rule("\\bkucing takut\\b", "penakut"));
        UNIVERSAL_RULES.add(new Rule("\\bkumpulin diri\\b", "kuatkan dirimu"));
        UNIVERSAL_RULES.add(new Rule("\\byang gema di lorong\\b", "yang bergema di lorong"));
        UNIVERSAL_RULES.add(new Rule("\\bsemua unit, segera keluar\\b", "semua unit, segera meluncur"));
        UNIVERSAL_RULES.add(new Rule("\\bsegera keluar bertempur\\b", "segera meluncur"));
        UNIVERSAL_RULES.add(new Rule("\\bambil tindakan menghindar\\b", "lakukan manuver menghindar"));
    }

    private static void initUniversalBatch2() {
        UNIVERSAL_RULES.add(new Rule("\\bterkena pukulan langsung\\b", "terkena tembakan langsung"));
        UNIVERSAL_RULES.add(new Rule("\\bpukulan langsung ke\\b", "tembakan telak ke"));
        UNIVERSAL_RULES.add(new Rule("\\bsistem peluncuran darurat\\b", "sistem ejeksi darurat"));
        UNIVERSAL_RULES.add(new Rule("\\btembak sesuai keinginan\\b", "bebas tembak"));
        UNIVERSAL_RULES.add(new Rule("\\bsemua bersorak britannia\\b", "hidup Britannia"));
        UNIVERSAL_RULES.add(new Rule("\\btertangkap dalam tembakan silang\\b", "terjebak dalam baku tembak"));
        UNIVERSAL_RULES.add(new Rule("\\btundukkan tanpa syarat\\b", "menyerahlah tanpa syarat"));
        UNIVERSAL_RULES.add(new Rule("\\bdi perut binatang\\b", "di sarang musuh"));
        UNIVERSAL_RULES.add(new Rule("\\bmenari langsung ke jebakan\\b", "masuk langsung ke jebakan"));
        UNIVERSAL_RULES.add(new Rule("\\bmenari tepat ke perangkap\\b", "masuk tepat ke perangkap"));
        UNIVERSAL_RULES.add(new Rule("\\bmenari ke perangkap\\b", "masuk ke perangkap"));
        UNIVERSAL_RULES.add(new Rule("\\bmenari tepat ke jebakan\\b", "masuk tepat ke jebakan"));
        UNIVERSAL_RULES.add(new Rule("\\bmenari ke jebakan\\b", "masuk ke jebakan"));
        UNIVERSAL_RULES.add(new Rule("\\bmemegang tali\\b", "mengendalikan semuanya"));
        UNIVERSAL_RULES.add(new Rule("\\bwarga harta karun\\b", "warga kehormatan"));
        UNIVERSAL_RULES.add(new Rule("\\bcheckmate\\b", "skakmat"));
        UNIVERSAL_RULES.add(new Rule("\\bCheckmate\\b", "Skakmat"));
        UNIVERSAL_RULES.add(new Rule("\\bklub classic lit\\b", "Klub Sastra Klasik"));
        UNIVERSAL_RULES.add(new Rule("\\bKlub Classic Lit\\b", "Klub Sastra Klasik"));
        UNIVERSAL_RULES.add(new Rule("\\bdatang buat ngelamun\\b", "datang untuk memberi salam"));
        UNIVERSAL_RULES.add(new Rule("\\bpeeping tom\\b", "tukang intip"));
        UNIVERSAL_RULES.add(new Rule("\\bPeeping Tom\\b", "tukang intip"));
        UNIVERSAL_RULES.add(new Rule("\\bwaktu cekout di perpustakaan\\b", "masa peminjaman di perpustakaan"));
        UNIVERSAL_RULES.add(new Rule("\\bwaktu cekout\\b", "masa peminjaman"));
        UNIVERSAL_RULES.add(new Rule("\\bpunya lidah tajam\\b", "mulutnya cukup pedas"));
        UNIVERSAL_RULES.add(new Rule("\\blidah yang tajam\\b", "mulut yang pedas"));
        UNIVERSAL_RULES.add(new Rule("\\blidah tajam\\b", "mulut pedas"));
        UNIVERSAL_RULES.add(new Rule("\\bresmi mati\\b", "dinyatakan meninggal"));
        UNIVERSAL_RULES.add(new Rule("\\bfavor pribadi\\b", "permintaan pribadi"));
        UNIVERSAL_RULES.add(new Rule("\\bseluruh tubuh siswa\\b", "seluruh siswa"));
        UNIVERSAL_RULES.add(new Rule("\\btubuh siswa\\b", "para siswa"));
        UNIVERSAL_RULES.add(new Rule("\\bdomba korban\\b", "tumbal"));
        UNIVERSAL_RULES.add(new Rule("\\bpara punks\\b", "berandalan"));
        UNIVERSAL_RULES.add(new Rule("\\banthologi\\b", "antologi"));
        UNIVERSAL_RULES.add(new Rule("\\bAnthologi\\b", "Antologi"));
        UNIVERSAL_RULES.add(new Rule("\\bsetelah semua, marah\\b", "lagipula, marah"));
        UNIVERSAL_RULES.add(new Rule("\\bsetelah semua\\b", "lagipula"));
        UNIVERSAL_RULES.add(new Rule("\\bSetelah semua\\b", "Lagipula"));
        UNIVERSAL_RULES.add(new Rule("\\bjam periode kelima\\b", "jam kelima"));
        UNIVERSAL_RULES.add(new Rule("\\bperiode kelima\\b", "jam kelima"));
        UNIVERSAL_RULES.add(new Rule("\\bbath open-air-nya\\b", "pemandian terbukanya"));
        UNIVERSAL_RULES.add(new Rule("\\bbath open-air\\b", "pemandian terbuka"));
        UNIVERSAL_RULES.add(new Rule("\\blayarnya kertas\\b", "pintu geser kertas"));
        UNIVERSAL_RULES.add(new Rule("\\blayar kertas\\b", "pintu geser kertas"));
        UNIVERSAL_RULES.add(new Rule("\\bbikin kudetas\\b", "bikin merinding"));
        UNIVERSAL_RULES.add(new Rule("\\btransom kayu\\b", "ventilasi kayu"));
        UNIVERSAL_RULES.add(new Rule("\\border up\\b", "pesanan siap"));
        UNIVERSAL_RULES.add(new Rule("\\bOrder up\\b", "Pesanan siap"));
        UNIVERSAL_RULES.add(new Rule("\\bgak banyak juga\\b", "bukan apa-apa kok"));
        UNIVERSAL_RULES.add(new Rule("\\bpencurilah kapal\\b", "jarahlah kapal"));
        UNIVERSAL_RULES.add(new Rule("\\bpencuri kapal\\b", "penjarah kapal"));
        UNIVERSAL_RULES.add(new Rule("\\bngelamun dan memohon\\b", "berlutut dan memohon"));
        UNIVERSAL_RULES.add(new Rule("\\bKeren banget\\.\\.\\. Itu yang bakal dikatakan Shinomiya\\b", "Manis sekali... Itu yang bakal dikatakan Shinomiya"));
        UNIVERSAL_RULES.add(new Rule("\\bkeren banget\\.\\.\\. itu yang bakal dikatakan\\b", "manis sekali... itu yang bakal dikatakan"));
        UNIVERSAL_RULES.add(new Rule("\\brouge ninjas\\b", "Ninja Pelarian"));
        UNIVERSAL_RULES.add(new Rule("\\brouge ninja\\b", "Ninja Pelarian"));
        UNIVERSAL_RULES.add(new Rule("\\bninja art:\\b", "Jutsu:"));
        UNIVERSAL_RULES.add(new Rule("\\bNinja Art:\\b", "Jutsu:"));
        UNIVERSAL_RULES.add(new Rule("\\btipe orochimaru\\b", "antek Orochimaru"));
        UNIVERSAL_RULES.add(new Rule("\\bTipe Orochimaru\\b", "Antek Orochimaru"));
        UNIVERSAL_RULES.add(new Rule("\\bsekali seumur hidup\\.\\.\\.permintaanku\\b", "permintaan sekali seumur hidupku"));
        UNIVERSAL_RULES.add(new Rule("\\btolong bawakan sasuke kembali\\b", "tolong bawa Sasuke kembali"));
        UNIVERSAL_RULES.add(new Rule("\\bTolong bawakan Sasuke kembali\\b", "Tolong bawa Sasuke kembali"));
        UNIVERSAL_RULES.add(new Rule("\\bdesa daun tersembunyi\\b", "Desa Konoha"));
        UNIVERSAL_RULES.add(new Rule("\\bDesa Daun Tersembunyi\\b", "Desa Konoha"));
        UNIVERSAL_RULES.add(new Rule("\\bdesa daun\\b", "Desa Konoha"));
        UNIVERSAL_RULES.add(new Rule("\\bDesa Daun\\b", "Desa Konoha"));
        UNIVERSAL_RULES.add(new Rule("\\bpencinta pertarungan\\b", "Fighting Dreamers"));
        UNIVERSAL_RULES.add(new Rule("\\bPencinta Pertarungan\\b", "Fighting Dreamers"));
        UNIVERSAL_RULES.add(new Rule("\\bjalur berkendara\\b", "garis lurus"));
        UNIVERSAL_RULES.add(new Rule("\\bkembali ke bentuk semula\\b", "kembali ke wujud semula"));
        UNIVERSAL_RULES.add(new Rule("\\bkena terbentur\\b", "terkena serangan"));
        UNIVERSAL_RULES.add(new Rule("\\bseperti yang diharapkan\\b", "sudah kuduga"));
        UNIVERSAL_RULES.add(new Rule("\\bSeperti yang diharapkan\\b", "Sudah kuduga"));
        UNIVERSAL_RULES.add(new Rule("\\bmemutuskan yang mana kata sandi\\b", "menentukan kata sandinya"));
        UNIVERSAL_RULES.add(new Rule("\\blayar stevenson\\b", "kotak Stevenson"));
        UNIVERSAL_RULES.add(new Rule("\\bLayar Stevenson\\b", "Kotak Stevenson"));
        UNIVERSAL_RULES.add(new Rule("\\bjujutsu tech\\b", "SMK Jujutsu"));
        UNIVERSAL_RULES.add(new Rule("\\bJujutsu Tech\\b", "SMK Jujutsu"));
        UNIVERSAL_RULES.add(new Rule("\\bpendeta yang korup\\b", "pendeta bejat"));
        UNIVERSAL_RULES.add(new Rule("\\bPendeta yang korup\\b", "Pendeta bejat"));
        UNIVERSAL_RULES.add(new Rule("\\bsekarang, sekarang, frieren\\b", "sudahlah, Frieren"));
        UNIVERSAL_RULES.add(new Rule("\\bSekarang, sekarang, Frieren\\b", "Sudahlah, Frieren"));
        UNIVERSAL_RULES.add(new Rule("\\bsekarang, sekarang\\b", "sudahlah"));
        UNIVERSAL_RULES.add(new Rule("\\bSekarang, sekarang\\b", "Sudahlah"));
    }

    private static void initUniversalBatch3() {
        UNIVERSAL_RULES.add(new Rule("\\bkita bilang dia jebakan\\b", "kan udah dibilang itu jebakan"));
        UNIVERSAL_RULES.add(new Rule("\\btuan tanah orochimaru\\b", "antek Orochimaru"));
        UNIVERSAL_RULES.add(new Rule("\\btinggalkan aku sendiri\\b", "jangan ganggu aku"));
        UNIVERSAL_RULES.add(new Rule("\\bTinggalkan aku sendiri\\b", "Jangan ganggu aku"));
        UNIVERSAL_RULES.add(new Rule("\\bmembuat guru kita kabur demi uangnya\\b", "memberikan perlawanan sengit kepada guru kita"));
        UNIVERSAL_RULES.add(new Rule("\\bkabur demi uangnya\\b", "memberikan perlawanan sengit"));
        UNIVERSAL_RULES.add(new Rule("\\bberikan mereka neraka\\b", "hajar mereka"));
        UNIVERSAL_RULES.add(new Rule("\\bBerikan mereka neraka\\b", "Hajar mereka"));
        UNIVERSAL_RULES.add(new Rule("\\bmengirim lawannya menyerang setiap tahun\\b", "membuat lawannya pulang tertunduk setiap tahun"));
        UNIVERSAL_RULES.add(new Rule("\\bmengirim lawannya menyerang\\b", "membuat lawannya pulang tertunduk"));
        UNIVERSAL_RULES.add(new Rule("\\baku akan menyerahkannya padamu, raja pedang\\b", "harus kuakui kehebatanmu, Raja Pedang"));
        UNIVERSAL_RULES.add(new Rule("\\baku akan menyerahkannya padamu, sword lord\\b", "harus kuakui kehebatanmu, Sword Lord"));
        UNIVERSAL_RULES.add(new Rule("\\bsetiap kedua\\b", "setiap detik"));
        UNIVERSAL_RULES.add(new Rule("\\bSetiap kedua\\b", "Setiap detik"));
        UNIVERSAL_RULES.add(new Rule("\\biblis tuhan\\b", "Raja Iblis"));
        UNIVERSAL_RULES.add(new Rule("\\bIblis Tuhan\\b", "Raja Iblis"));
        UNIVERSAL_RULES.add(new Rule("\\bnaga berat\\b", "Heavy Dragon"));
        UNIVERSAL_RULES.add(new Rule("\\bNaga Berat\\b", "Naga Lapis Baja"));
        UNIVERSAL_RULES.add(new Rule("\\bpenguasa bawahan keluarga\\b", "tuan feodal keluarga"));
        UNIVERSAL_RULES.add(new Rule("\\bsungguh pengalaman yang luar biasa dari masa lalu\\b", "bikin bernostalgia banget"));
        UNIVERSAL_RULES.add(new Rule("\\bSangat kuat! Gunakan Pengganti\\b", "Steely! Gunakan Substitute"));
        UNIVERSAL_RULES.add(new Rule("\\bSangat kuat! Gunakan\\b", "Steely! Gunakan"));
        UNIVERSAL_RULES.add(new Rule("\\bruang bawah tanah peringkat ([A-Za-z0-9]+)\\b", "dungeon peringkat $1"));
        UNIVERSAL_RULES.add(new Rule("\\bruang bawah tanah peringkat-([A-Za-z0-9]+)\\b", "dungeon peringkat-$1"));
        UNIVERSAL_RULES.add(new Rule("\\bmenjelajahi ruang bawah tanah\\b", "menjelajahi dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\bmenaklukkan (\\d+|satu|dua|tiga|empat|lima) ruang bawah tanah\\b", "menaklukkan $1 dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\bmenaklukkan ruang bawah tanah\\b", "menaklukkan dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\bmenantang satu ruang bawah tanah lagi\\b", "menantang satu dungeon lagi"));
        UNIVERSAL_RULES.add(new Rule("\\bmenantang ruang bawah tanah\\b", "menantang dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\bmenyelesaikan ruang bawah tanah\\b", "menyelesaikan dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\bbos ruang bawah tanah\\b", "bos dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\bmonster ruang bawah tanah\\b", "monster dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\blantai ruang bawah tanah\\b", "lantai dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\bmengulangi perintah itu\\.\\s*dungeon\\b", "mengulang dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\buntuk pesta itu\\b", "untuk party"));
        UNIVERSAL_RULES.add(new Rule("\\bapakah sedang mengisi daya\\b", "apakah dia sedang mengumpulkan kekuatan"));
        UNIVERSAL_RULES.add(new Rule("\\bsedang mengisi daya\\b", "sedang mengumpulkan kekuatan"));
        UNIVERSAL_RULES.add(new Rule("\\bparty yang dimiliki oleh\\b", "party tempat bergabungnya"));
        UNIVERSAL_RULES.add(new Rule("\\bpanggilanku tidak bisa digunakan untuk bertarung\\b", "monster summon-ku sedang bertarung"));
        UNIVERSAL_RULES.add(new Rule("\\btanpa panggilan itu\\b", "tanpa monster summon itu"));
        UNIVERSAL_RULES.add(new Rule("\\bmelakukan casting di depan\\b", "merapal sihir di depan"));
        UNIVERSAL_RULES.add(new Rule("\\bmelakukan casting\\b", "merapal sihir"));
        UNIVERSAL_RULES.add(new Rule("\\bmelakukan cast\\b", "merapal sihir"));
        UNIVERSAL_RULES.add(new Rule("\\bcasting di depan orang\\b", "merapal sihir di depan orang"));
        UNIVERSAL_RULES.add(new Rule("\\bcasting seal\\b", "segel sihir"));
        UNIVERSAL_RULES.add(new Rule("\\btinta sulap\\b", "tinta sihir"));
        UNIVERSAL_RULES.add(new Rule("\\btoko alat tulis ajaib\\b", "toko alat sihir"));
        UNIVERSAL_RULES.add(new Rule("\\bcelah di tongkat\\b", "retakan di tongkat"));
        UNIVERSAL_RULES.add(new Rule("\\bmenjadi magang penyihir\\b", "menjadi murid penyihir"));
        UNIVERSAL_RULES.add(new Rule("\\bmenjadi magang\\b", "menjadi murid sihir"));
        UNIVERSAL_RULES.add(new Rule("\\bmemilih ramuan mahkota\\b", "memetik herba mahkota"));
        UNIVERSAL_RULES.add(new Rule("\\bmemilih ramuan\\b", "memetik herba"));
        UNIVERSAL_RULES.add(new Rule("\\bramuan mahkota\\b", "herba mahkota"));
        UNIVERSAL_RULES.add(new Rule("\\bpersetujuan mahkota\\b", "Ujian Mahkota"));
        UNIVERSAL_RULES.add(new Rule("\\bpegasus carriage\\b", "kereta Pegasos"));
        UNIVERSAL_RULES.add(new Rule("\\bsangat baik andal\\b", "sangat mantap"));
        UNIVERSAL_RULES.add(new Rule("\\btidak ada yang perlu ditakutkan of\\b", "tidak ada yang perlu ditakuti"));
        UNIVERSAL_RULES.add(new Rule("\\bperlu ditakutkan of\\b", "perlu ditakuti"));
        UNIVERSAL_RULES.add(new Rule("\\bditakutkan of\\b", "ditakuti"));
        UNIVERSAL_RULES.add(new Rule("\\btiketnya terjual habis dalam waktu singkat\\b", "langsung ludes dalam sekejap"));
        UNIVERSAL_RULES.add(new Rule("\\bterjual habis dalam waktu singkat\\b", "langsung ludes dalam sekejap"));
        UNIVERSAL_RULES.add(new Rule("\\bterjual habis dalam sekejap\\b", "langsung ludes dalam sekejap"));
        UNIVERSAL_RULES.add(new Rule("\\btak punya apa pun untuk ditawarkan di dunia sihir\\b", "tak punya tempat di dunia sihir"));
        UNIVERSAL_RULES.add(new Rule("\\btidak punya apa pun untuk ditawarkan di dunia sihir\\b", "tidak punya tempat di dunia sihir"));
        UNIVERSAL_RULES.add(new Rule("\\btidak memiliki apa pun untuk ditawarkan di dunia sihir\\b", "tidak punya tempat di dunia sihir"));
        UNIVERSAL_RULES.add(new Rule("\\baku bertanggung jawab atas berdirinya di dekat pintu\\b", "salahku sendiri kok berdiri di dekat pintu"));
        UNIVERSAL_RULES.add(new Rule("\\bbertanggung jawab atas berdirinya di dekat pintu\\b", "salah sendiri kok berdiri di dekat pintu"));
        UNIVERSAL_RULES.add(new Rule("\\bdi sini untuk mengambil pesanan\\b", "mau ambil pesanan"));
        UNIVERSAL_RULES.add(new Rule("\\banda pikir anda bisa memperbaikinya\\b", "kamu rasa kamu bisa memperbaikinya"));
        UNIVERSAL_RULES.add(new Rule("\\bapakah anda berpikir anda bisa\\b", "apa kamu rasa kamu bisa"));
        UNIVERSAL_RULES.add(new Rule("\\bberi aku istirahat\\b", "yang benar saja"));
        UNIVERSAL_RULES.add(new Rule("\\bitu melayanimu dengan benar\\b", "rasakan itu"));
        UNIVERSAL_RULES.add(new Rule("\\bitu melayanimu dengan pas\\b", "rasakan itu"));
        UNIVERSAL_RULES.add(new Rule("\\bmelayanimu dengan benar\\b", "rasakan itu"));
        UNIVERSAL_RULES.add(new Rule("\\bmelayanimu dengan pas\\b", "rasakan itu"));
        UNIVERSAL_RULES.add(new Rule("\\bitu melayanimu\\b", "rasakan itu"));
        UNIVERSAL_RULES.add(new Rule("\\bitu tidak bisa ditolong\\b", "mau bagaimana lagi"));
        UNIVERSAL_RULES.add(new Rule("\\btak bisa ditolong\\b", "mau bagaimana lagi"));
        UNIVERSAL_RULES.add(new Rule("\\baku akan merawat ini\\b", "biar aku yang urus ini"));
        UNIVERSAL_RULES.add(new Rule("\\baku akan merawatnya\\b", "biar aku yang urus"));
        UNIVERSAL_RULES.add(new Rule("\\bmerawat ini\\b", "mengurus ini"));
        UNIVERSAL_RULES.add(new Rule("\\bjangan biarkan penjagamu turun\\b", "jangan lengah"));
        UNIVERSAL_RULES.add(new Rule("\\bmenurunkan penjagamu\\b", "lengah"));
        UNIVERSAL_RULES.add(new Rule("\\bmenurunkan penjaga\\b", "lengah"));
        UNIVERSAL_RULES.add(new Rule("\\bperhatikan punggungmu\\b", "waspadalah di belakangmu"));
    }

    private static void initUniversalBatch4() {
        UNIVERSAL_RULES.add(new Rule("\\bberdiri kembali\\b", "mundur"));
        UNIVERSAL_RULES.add(new Rule("\\bkembali ke bawah\\b", "mundur"));
        UNIVERSAL_RULES.add(new Rule("\\bkamu lagi memukulku\\b", "kamu lagi merayuku ya"));
        UNIVERSAL_RULES.add(new Rule("\\blagi memukulku\\b", "lagi menggodaku"));
        UNIVERSAL_RULES.add(new Rule("\\bjangan sombong ayam\\b", "jangan besar kepala"));
        UNIVERSAL_RULES.add(new Rule("\\bbentuk pikiranmu\\b", "tentukan pilihanmu"));
        UNIVERSAL_RULES.add(new Rule("\\bperubahan hati\\b", "berubah pikiran"));
        UNIVERSAL_RULES.add(new Rule("\\baku tidak bisa mengambilnya lagi\\b", "aku sudah tidak tahan lagi"));
        UNIVERSAL_RULES.add(new Rule("\\bmenyeberangi pikiranku\\b", "terlintas di pikiranku"));
        UNIVERSAL_RULES.add(new Rule("\\bsimpan untuk dirimu sendiri\\b", "rahasiakan ini"));
        UNIVERSAL_RULES.add(new Rule("\\bdi atas tubuh matiku\\b", "langkahi dulu mayatku"));
        UNIVERSAL_RULES.add(new Rule("\\bsiapa yang mengisi daya\\b", "siapa penanggung jawabnya"));
        UNIVERSAL_RULES.add(new Rule("\\bgigit lidahmu\\b", "jaga ucapanmu"));
        UNIVERSAL_RULES.add(new Rule("\\bputar mata buta\\b", "tutup mata"));
        UNIVERSAL_RULES.add(new Rule("\\bkaki dingin\\b", "ciut nyali"));
        UNIVERSAL_RULES.add(new Rule("\\bdi atas rumah\\b", "gratis dari kedai"));
        UNIVERSAL_RULES.add(new Rule("\\baturan jempol\\b", "pedoman umum"));
        UNIVERSAL_RULES.add(new Rule("\\bambil untuk diberikan\\b", "menganggap remeh"));
        UNIVERSAL_RULES.add(new Rule("\\bpotong itu\\b", "hentikan itu"));
        UNIVERSAL_RULES.add(new Rule("\\bsepotong kue\\b", "gampang banget"));
        UNIVERSAL_RULES.add(new Rule("\\bhilang dalam pikiran\\b", "melamun"));
        UNIVERSAL_RULES.add(new Rule("\\btumpahkan kacang\\b", "bocorkan rahasianya"));
        UNIVERSAL_RULES.add(new Rule("\\btumpahkan teh\\b", "ceritakan gosipnya"));
        UNIVERSAL_RULES.add(new Rule("\\bdi air panas\\b", "dalam masalah besar"));
        UNIVERSAL_RULES.add(new Rule("\\bdi bawah cuaca\\b", "kurang enak badan"));
        UNIVERSAL_RULES.add(new Rule("\\bmenghadapi musik\\b", "menanggung akibatnya"));
        UNIVERSAL_RULES.add(new Rule("\\bgigit peluru\\b", "tabahkan dirimu"));
        UNIVERSAL_RULES.add(new Rule("\\bbicara tentang iblis\\b", "panjang umur, baru diomongin"));
        UNIVERSAL_RULES.add(new Rule("\\bpukul paku di kepala\\b", "tepat sasaran"));
        UNIVERSAL_RULES.add(new Rule("\\bmemukul semak-semak\\b", "berbelit-belit"));
        UNIVERSAL_RULES.add(new Rule("\\bpatahkan kaki\\b", "semoga sukses"));
        UNIVERSAL_RULES.add(new Rule("\\bbiaya lengan dan kaki\\b", "harganya selangit"));
        UNIVERSAL_RULES.add(new Rule("\\bkeluar dari tangan\\b", "lepas kendali"));
        UNIVERSAL_RULES.add(new Rule("\\bgantung di sana\\b", "bertahanlah"));
        UNIVERSAL_RULES.add(new Rule("\\btarik kaki\\b", "mengerjai"));
        UNIVERSAL_RULES.add(new Rule("\\bdi bawah hidungmu\\b", "tepat di depan matamu"));
        UNIVERSAL_RULES.add(new Rule("\\bkehilangan kesejukan\\b", "hilang kesabaran"));
        UNIVERSAL_RULES.add(new Rule("\\bambil dengan mudah\\b", "santai saja"));
        UNIVERSAL_RULES.add(new Rule("\\bhitung aku masuk\\b", "aku ikut"));
        UNIVERSAL_RULES.add(new Rule("\\bhitung aku keluar\\b", "aku nggak ikut"));
        UNIVERSAL_RULES.add(new Rule("\\bbukan di jam tanganku\\b", "tidak akan kubiarkan"));
        UNIVERSAL_RULES.add(new Rule("\\bdapatkan pegangan\\b", "sadarlah"));
        UNIVERSAL_RULES.add(new Rule("\\btundukkan kepalamu\\b", "tiarap"));
        UNIVERSAL_RULES.add(new Rule("\\btanpa pelanggaran\\b", "tanpa mengurangi rasa hormat"));
        UNIVERSAL_RULES.add(new Rule("\\btidak ada pelanggaran\\b", "tanpa mengurangi rasa hormat"));
        UNIVERSAL_RULES.add(new Rule("\\btidak diambil\\b", "santai saja"));
        UNIVERSAL_RULES.add(new Rule("\\bmiliki punggungku\\b", "lindungi punggungku"));
        UNIVERSAL_RULES.add(new Rule("\\bmenyeberangi garis\\b", "melampaui batas"));
        UNIVERSAL_RULES.add(new Rule("\\bjangan menyanjung dirimu\\b", "jangan kepedean"));
        UNIVERSAL_RULES.add(new Rule("\\bbukan bisnis anda\\b", "bukan urusanmu"));
        UNIVERSAL_RULES.add(new Rule("\\bbukan bisnis kamu\\b", "bukan urusanmu"));
        UNIVERSAL_RULES.add(new Rule("\\burus bisnismu sendiri\\b", "urus urusanmu sendiri"));
        UNIVERSAL_RULES.add(new Rule("\\bambil waktumu\\b", "santai saja, jangan terburu-buru"));
        UNIVERSAL_RULES.add(new Rule("\\bmaju ke depan\\b", "silakan"));
        UNIVERSAL_RULES.add(new Rule("\\btepat pergi\\b", "sekarang juga"));
        UNIVERSAL_RULES.add(new Rule("\\bsemua tiba-tiba\\b", "tiba-tiba"));
        UNIVERSAL_RULES.add(new Rule("\\bpada akhirnya panjang\\b", "setelah penantian panjang"));
        UNIVERSAL_RULES.add(new Rule("\\bhal pertama di pagi hari\\b", "pagi-pagi buta"));
        UNIVERSAL_RULES.add(new Rule("\\btidur di atasnya\\b", "pikirkan matang-matang besok"));
        UNIVERSAL_RULES.add(new Rule("\\bkembali ke kotak satu\\b", "mulai dari awal lagi"));
        UNIVERSAL_RULES.add(new Rule("\\bharus memantul\\b", "harus cabut"));
        UNIVERSAL_RULES.add(new Rule("\\btidak ada batasan,\\s*itu\\b", "beneran, itu"));
        UNIVERSAL_RULES.add(new Rule("\\btidak ada topi,\\s*itu\\b", "beneran, itu"));
        UNIVERSAL_RULES.add(new Rule("\\btidak ada topi\\b", "beneran"));
        UNIVERSAL_RULES.add(new Rule("\\bbukan topi\\b", "beneran"));
        UNIVERSAL_RULES.add(new Rule("\\baku turun untuk itu\\b", "aku ikut"));
        UNIVERSAL_RULES.add(new Rule("\\baku turun untuk\\b", "aku mau"));
        UNIVERSAL_RULES.add(new Rule("\\bsaya kecewa karena itu\\b", "aku setuju dengan itu"));
        UNIVERSAL_RULES.add(new Rule("\\bmendingin keluar\\b", "santai dulu"));
        UNIVERSAL_RULES.add(new Rule("\\bdisimpan oleh belnya\\b", "selamat di saat kritis"));
        UNIVERSAL_RULES.add(new Rule("\\bdisimpan oleh bel\\b", "selamat di saat kritis"));
        UNIVERSAL_RULES.add(new Rule("\\bdiselamatkan oleh bel\\b", "selamat di saat kritis"));
        UNIVERSAL_RULES.add(new Rule("\\bsudah dimuntahkan\\b", "cepat katakan"));
        UNIVERSAL_RULES.add(new Rule("\\bkeluarkan ludahmu\\b", "cepat katakan"));
        UNIVERSAL_RULES.add(new Rule("\\bludahkan itu\\b", "cepat katakan"));
        UNIVERSAL_RULES.add(new Rule("\\bpegang kudamu\\b", "sabar dulu"));
        UNIVERSAL_RULES.add(new Rule("\\btahan kudamu\\b", "tunggu sebentar"));
        UNIVERSAL_RULES.add(new Rule("\\bambil cek hujan\\b", "lain kali saja"));
        UNIVERSAL_RULES.add(new Rule("\\btidak mendapat rimpang\\b", "tidak punya pesona"));
        UNIVERSAL_RULES.add(new Rule("\\btanpa rimpang\\b", "tidak punya pesona"));
        UNIVERSAL_RULES.add(new Rule("\\bnol rizz\\b", "tidak punya karisma"));
        UNIVERSAL_RULES.add(new Rule("\\bsentuh rumput\\b", "sadar realita"));
        UNIVERSAL_RULES.add(new Rule("\\bhubungi kenyataan\\b", "sadar realita"));
        UNIVERSAL_RULES.add(new Rule("\\bkeluar dari biru\\b", "tiba-tiba"));
        UNIVERSAL_RULES.add(new Rule("\\bmelalui tebal dan tipis\\b", "dalam suka dan duka"));
    }

    private static void initUniversalBatch5() {
        UNIVERSAL_RULES.add(new Rule("\\bmembakar jembatan\\b", "memutus hubungan"));
        UNIVERSAL_RULES.add(new Rule("\\bmenembak angin\\b", "mengobrol santai"));
        UNIVERSAL_RULES.add(new Rule("\\blempar ke bawah bus\\b", "mengorbankan kawan"));
        UNIVERSAL_RULES.add(new Rule("\\bmelempar ke bawah bus\\b", "mengorbankan kawan"));
        UNIVERSAL_RULES.add(new Rule("\\bdalam kulit kacang\\b", "singkatnya"));
        UNIVERSAL_RULES.add(new Rule("\\bpotong untuk mengejar\\b", "langsung ke intinya"));
        UNIVERSAL_RULES.add(new Rule("\\bberkah dalam penyamaran\\b", "berkah tersembunyi"));
        UNIVERSAL_RULES.add(new Rule("\\bmelihat mata ke mata\\b", "sependapat"));
        UNIVERSAL_RULES.add(new Rule("\\bair di bawah jembatan\\b", "yang lalu biarlah berlalu"));
        UNIVERSAL_RULES.add(new Rule("\\bmemecahkan es\\b", "mencairkan suasana"));
        UNIVERSAL_RULES.add(new Rule("\\bbaju besi plot\\b", "kebal berkat skenario"));
        UNIVERSAL_RULES.add(new Rule("\\benergi karakter utama\\b", "aura protagonis"));
        UNIVERSAL_RULES.add(new Rule("\\bhidup bebas sewa di kepalaku\\b", "selalu terngiang di kepalaku"));
        UNIVERSAL_RULES.add(new Rule("\\bhidup bebas sewa\\b", "selalu terngiang-ngiang"));
        UNIVERSAL_RULES.add(new Rule("\\bpantat mati\\b", "bener-bener serius"));
        UNIVERSAL_RULES.add(new Rule("\\btertangkap di 4k\\b", "tertangkap basah ada buktinya"));
        UNIVERSAL_RULES.add(new Rule("\\bturun buruk\\b", "ngenes banget"));
        UNIVERSAL_RULES.add(new Rule("\\bpemeriksaan getaran\\b", "cek suasana"));
        UNIVERSAL_RULES.add(new Rule("\\bmengambil w\\b", "meraih kemenangan"));
        UNIVERSAL_RULES.add(new Rule("\\bambil w\\b", "raih kemenangan"));
        UNIVERSAL_RULES.add(new Rule("\\bmengambil l\\b", "menelan kekalahan"));
        UNIVERSAL_RULES.add(new Rule("\\bambil l\\b", "telan kekalahan"));
        UNIVERSAL_RULES.add(new Rule("\\bbos penjara bawah tanah\\b", "bos dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\btuan serikat\\b", "ketua guild"));
        UNIVERSAL_RULES.add(new Rule("\\bbersihkan pesta\\b", "seluruh party gugur"));
        UNIVERSAL_RULES.add(new Rule("\\btingkat penurunan\\b", "drop rate"));
        UNIVERSAL_RULES.add(new Rule("\\bkolam mana\\b", "cadangan mana"));
        UNIVERSAL_RULES.add(new Rule("\\bperiode pendinginan\\b", "waktu cooldown"));
        UNIVERSAL_RULES.add(new Rule("\\bsatu tembakan mati\\b", "sekali serang tewas"));
        UNIVERSAL_RULES.add(new Rule("\\bsebut itu hari\\b", "sudahi dulu hari ini"));
        UNIVERSAL_RULES.add(new Rule("\\bsebut itu sehari\\b", "sudahi dulu hari ini"));
        UNIVERSAL_RULES.add(new Rule("\\bpanggil itu sehari\\b", "sudahi dulu hari ini"));
        UNIVERSAL_RULES.add(new Rule("\\bpukul karung\\b", "pergi tidur"));
        UNIVERSAL_RULES.add(new Rule("\\bmemukul karung\\b", "pergi tidur"));
        UNIVERSAL_RULES.add(new Rule("\\bjangan berkeringat\\b", "jangan dipikirkan"));
        UNIVERSAL_RULES.add(new Rule("\\bburukku\\b", "salahku"));
        UNIVERSAL_RULES.add(new Rule("\\binti ruang bawah tanah\\b", "inti dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\binti penjara bawah tanah\\b", "inti dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\bjebakan ruang bawah tanah\\b", "jebakan dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\bgerbang ruang bawah tanah\\b", "gerbang dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\bgerbang penjara bawah tanah\\b", "gerbang dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\bledakan ruang bawah tanah\\b", "dungeon break"));
        UNIVERSAL_RULES.add(new Rule("\\bkebobolan ruang bawah tanah\\b", "dungeon break"));
        UNIVERSAL_RULES.add(new Rule("\\bpenjelajahan ruang bawah tanah\\b", "eksplorasi dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\bpenjelajah ruang bawah tanah\\b", "penjelajah dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\bruang bawah tanah tingkat ([A-Za-z0-9]+)\\b", "dungeon tingkat $1"));
        UNIVERSAL_RULES.add(new Rule("\\bruang bawah tanah level ([A-Za-z0-9]+)\\b", "dungeon level $1"));
        UNIVERSAL_RULES.add(new Rule("\\bmasuk ke ruang bawah tanah\\b", "masuk ke dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\bkeluar dari ruang bawah tanah\\b", "keluar dari dungeon"));
        UNIVERSAL_RULES.add(new Rule("\\bruang bos\\b", "boss room"));
        UNIVERSAL_RULES.add(new Rule("\\bkamar bos\\b", "boss room"));
        UNIVERSAL_RULES.add(new Rule("\\bpesta penyerbuan\\b", "party raid"));
        UNIVERSAL_RULES.add(new Rule("\\bpesta petualang\\b", "party petualang"));
        UNIVERSAL_RULES.add(new Rule("\\banggota pesta\\b", "anggota party"));
        UNIVERSAL_RULES.add(new Rule("\\bpemimpin pesta\\b", "party leader"));
        UNIVERSAL_RULES.add(new Rule("\\bketua pesta\\b", "party leader"));
        UNIVERSAL_RULES.add(new Rule("\\bmembubarkan pesta\\b", "membubarkan party"));
        UNIVERSAL_RULES.add(new Rule("\\bpesta bubar\\b", "party bubar"));
        UNIVERSAL_RULES.add(new Rule("\\bbergabung dengan pesta\\b", "bergabung dengan party"));
        UNIVERSAL_RULES.add(new Rule("\\bbergabung ke pesta\\b", "bergabung ke party"));
        UNIVERSAL_RULES.add(new Rule("\\bmencari pesta\\b", "mencari party"));
        UNIVERSAL_RULES.add(new Rule("\\bmembentuk pesta\\b", "membentuk party"));
        UNIVERSAL_RULES.add(new Rule("\\bkartu serikat\\b", "kartu guild"));
        UNIVERSAL_RULES.add(new Rule("\\banggota serikat\\b", "anggota guild"));
        UNIVERSAL_RULES.add(new Rule("\\bmisi serikat\\b", "misi guild"));
        UNIVERSAL_RULES.add(new Rule("\\bpapan serikat\\b", "papan misi guild"));
        UNIVERSAL_RULES.add(new Rule("\\bstaf serikat\\b", "resepsionis guild"));
        UNIVERSAL_RULES.add(new Rule("\\bpegawai serikat\\b", "resepsionis guild"));
        UNIVERSAL_RULES.add(new Rule("\\bmenarik kebencian\\b", "menarik aggro"));
        UNIVERSAL_RULES.add(new Rule("\\bmengambil kebencian\\b", "mengambil aggro"));
        UNIVERSAL_RULES.add(new Rule("\\bkehilangan kebencian\\b", "kehilangan aggro"));
        UNIVERSAL_RULES.add(new Rule("\\btitik bertelur\\b", "titik spawn"));
        UNIVERSAL_RULES.add(new Rule("\\btitik kemunculan\\b", "titik spawn"));
        UNIVERSAL_RULES.add(new Rule("\\btitik bibit\\b", "titik spawn"));
        UNIVERSAL_RULES.add(new Rule("\\bwaktu pendinginan\\b", "waktu cooldown"));
        UNIVERSAL_RULES.add(new Rule("\\bmasa pendinginan\\b", "waktu cooldown"));
        UNIVERSAL_RULES.add(new Rule("\\btingkat drop\\b", "drop rate"));
        UNIVERSAL_RULES.add(new Rule("\\bbarang penurunan\\b", "drop item"));
        UNIVERSAL_RULES.add(new Rule("\\bbarang drop\\b", "item drop"));
        UNIVERSAL_RULES.add(new Rule("\\btetesan monster\\b", "drop item monster"));
        UNIVERSAL_RULES.add(new Rule("\\bbilah kesehatan\\b", "bar HP"));
        UNIVERSAL_RULES.add(new Rule("\\bpoin kesehatan\\b", "HP"));
        UNIVERSAL_RULES.add(new Rule("\\btitik hit\\b", "HP"));
        UNIVERSAL_RULES.add(new Rule("\\bbilah mana\\b", "bar MP"));
        UNIVERSAL_RULES.add(new Rule("\\bpoin mana\\b", "MP"));
    }

    private static void initUniversalBatch6() {
        UNIVERSAL_RULES.add(new Rule("\\bbos serbuan\\b", "raid boss"));
        UNIVERSAL_RULES.add(new Rule("\\bbos penyerbuan\\b", "raid boss"));
        UNIVERSAL_RULES.add(new Rule("\\bjendela negara bagian\\b", "jendela status"));
        UNIVERSAL_RULES.add(new Rule("\\bpohon keterampilan\\b", "skill tree"));
        UNIVERSAL_RULES.add(new Rule("\\bpoin keterampilan\\b", "skill point"));
        UNIVERSAL_RULES.add(new Rule("\\bpemburu peringkat ([SABCDE])\\b", "hunter rank $1"));
        UNIVERSAL_RULES.add(new Rule("\\bpemburu peringkat-([SABCDE])\\b", "hunter rank-$1"));
        UNIVERSAL_RULES.add(new Rule("\\bpemburu kelas ([SABCDE])\\b", "hunter kelas $1"));
        UNIVERSAL_RULES.add(new Rule("\\bpemain tunggal\\b", "pemain solo"));
        UNIVERSAL_RULES.add(new Rule("\\bpemburu tunggal\\b", "hunter solo"));
        UNIVERSAL_RULES.add(new Rule("\\bpembunuh pemain\\b", "player killer"));
        UNIVERSAL_RULES.add(new Rule("\\bpermainan kematian\\b", "death game"));
        UNIVERSAL_RULES.add(new Rule("\\bpenggemar status\\b", "buff status"));
        UNIVERSAL_RULES.add(new Rule("\\bdiberi penggemar\\b", "diberi buff"));
        UNIVERSAL_RULES.add(new Rule("\\bmendapat penggemar\\b", "mendapat buff"));
        UNIVERSAL_RULES.add(new Rule("\\bapakah kamu nyata\\b", "beneran nih"));
        UNIVERSAL_RULES.add(new Rule("\\bkamu nyata\\b", "kamu serius"));
        UNIVERSAL_RULES.add(new Rule("\\btidak ada cara di neraka\\b", "mana mungkin"));
        UNIVERSAL_RULES.add(new Rule("\\btak ada cara di neraka\\b", "mana mungkin"));
        UNIVERSAL_RULES.add(new Rule("\\bpotong omong kosong\\b", "hentikan omong kosongmu"));
        UNIVERSAL_RULES.add(new Rule("\\bpotong kotoran itu\\b", "jangan banyak omong"));
        UNIVERSAL_RULES.add(new Rule("\\bkamu daging mati\\b", "mampus kau"));
        UNIVERSAL_RULES.add(new Rule("\\bdaging mati\\b", "habis riwayatmu"));
        UNIVERSAL_RULES.add(new Rule("\\bketakutan keluar\\b", "panik banget"));
        UNIVERSAL_RULES.add(new Rule("\\bkacau denganku\\b", "mengerjaiku"));
        UNIVERSAL_RULES.add(new Rule("\\bkacau dengan aku\\b", "mengerjaiku"));
        UNIVERSAL_RULES.add(new Rule("\\bdiberi makan dengan\\b", "muak dengan"));
        UNIVERSAL_RULES.add(new Rule("\\bsudah diberi makan dengan\\b", "sudah muak dengan"));
        UNIVERSAL_RULES.add(new Rule("\\bpergi mudah padaku\\b", "jangan kejam-kejam ya"));
        UNIVERSAL_RULES.add(new Rule("\\bpergi mudah pada saya\\b", "jangan kejam-kejam ya"));
        UNIVERSAL_RULES.add(new Rule("\\bapa di bumi\\b", "sebenarnya apa"));
        UNIVERSAL_RULES.add(new Rule("\\bbuat dirimu di rumah\\b", "anggap saja rumah sendiri"));
        UNIVERSAL_RULES.add(new Rule("\\bmerasa seperti di rumah\\b", "anggap rumah sendiri"));
        UNIVERSAL_RULES.add(new Rule("\\bgigit aku\\b", "persetan"));
        UNIVERSAL_RULES.add(new Rule("\\bjatuhkan itu\\b", "sudahlah, lupakan saja"));
        UNIVERSAL_RULES.add(new Rule("\\batasi itu\\b", "ikhlaskan saja"));
        UNIVERSAL_RULES.add(new Rule("\\bmelenturkan otot\\b", "pamer kekuatan"));
        UNIVERSAL_RULES.add(new Rule("\\bberhenti melenturkan\\b", "jangan pamer"));
        UNIVERSAL_RULES.add(new Rule("\\bkeluar dalam kemarahan\\b", "keluar sambil ngamuk"));
        UNIVERSAL_RULES.add(new Rule("\\bkeluar karena marah\\b", "cabut gara-gara emosi"));
        UNIVERSAL_RULES.add(new Rule("\\bhadiah yang jelas\\b", "hadiah penyelesaian misi"));
        UNIVERSAL_RULES.add(new Rule("\\bkelas yang hebat\\b", "kelas epik"));
        UNIVERSAL_RULES.add(new Rule("\\bpekerjaan yang luar biasa\\b", "pekerjaan kelas epik"));
        UNIVERSAL_RULES.add(new Rule("\\bmembuat masuk akal\\b", "masuk akal"));
        UNIVERSAL_RULES.add(new Rule("\\btidak membuat masuk akal\\b", "tidak masuk akal"));
        UNIVERSAL_RULES.add(new Rule("\\bmembuat rasa\\b", "masuk akal"));
        UNIVERSAL_RULES.add(new Rule("\\bambil perhatian\\b", "hati-hati"));
        UNIVERSAL_RULES.add(new Rule("\\bambil hati-hati\\b", "hati-hati"));
        UNIVERSAL_RULES.add(new Rule("\\bambil waktumu\\b", "santai saja, tidak usah buru-buru"));
        UNIVERSAL_RULES.add(new Rule("\\bambil waktu anda\\b", "silakan santai saja"));
        UNIVERSAL_RULES.add(new Rule("\\bmemegang kembali\\b", "menahan diri"));
        UNIVERSAL_RULES.add(new Rule("\\bjangan memegang kembali\\b", "jangan menahan diri"));
        UNIVERSAL_RULES.add(new Rule("\\bberdiri kembali\\b", "mundur"));
        UNIVERSAL_RULES.add(new Rule("\\bmelangkah kembali\\b", "mundur"));
        UNIVERSAL_RULES.add(new Rule("\\bmemanggil tembakan\\b", "memegang kendali"));
        UNIVERSAL_RULES.add(new Rule("\\bmemanggil semua tembakan\\b", "memegang semua keputusan"));
        UNIVERSAL_RULES.add(new Rule("\\bmengambil kesempatan\\b", "mencoba peruntungan"));
        UNIVERSAL_RULES.add(new Rule("\\bmemberinya tembakan\\b", "mencobanya"));
        UNIVERSAL_RULES.add(new Rule("\\bmemberi tembakan\\b", "mencobanya"));
        UNIVERSAL_RULES.add(new Rule("\\bdi luar ligaku\\b", "terlalu tinggi untukku"));
        UNIVERSAL_RULES.add(new Rule("\\bdi luar liga saya\\b", "terlalu tinggi untuk saya"));
        UNIVERSAL_RULES.add(new Rule("\\bdi luar ligamu\\b", "bukan tandinganmu"));
        UNIVERSAL_RULES.add(new Rule("\\bmelompat ke kesimpulan\\b", "buru-buru menyimpulkan"));
        UNIVERSAL_RULES.add(new Rule("\\bmenarik kakiku\\b", "mengerjaiku"));
        UNIVERSAL_RULES.add(new Rule("\\bmenarik kaki saya\\b", "mengerjai saya"));
        UNIVERSAL_RULES.add(new Rule("\\bmelempar handuk\\b", "menyerah"));
        UNIVERSAL_RULES.add(new Rule("\\bmelemparkan handuk\\b", "menyerah"));
        UNIVERSAL_RULES.add(new Rule("\\bmendorong keberuntunganmu\\b", "ngelunjak"));
        UNIVERSAL_RULES.add(new Rule("\\bmendorong keberuntungan anda\\b", "bertindak terlalu jauh"));
        UNIVERSAL_RULES.add(new Rule("\\bmembeli waktu\\b", "mengulur waktu"));
        UNIVERSAL_RULES.add(new Rule("\\bmembeli beberapa waktu\\b", "mengulur waktu"));
        UNIVERSAL_RULES.add(new Rule("\\bbermain bodoh\\b", "pura-pura tidak tahu"));
        UNIVERSAL_RULES.add(new Rule("\\broda ketiga\\b", "obat nyamuk"));
        UNIVERSAL_RULES.add(new Rule("\\btergelincir pikiranku\\b", "aku kelupaan"));
        UNIVERSAL_RULES.add(new Rule("\\btergelincir dari pikiranku\\b", "aku kelupaan"));
        UNIVERSAL_RULES.add(new Rule("\\bmembunyikan lonceng\\b", "terdengar tidak asing"));
        UNIVERSAL_RULES.add(new Rule("\\bmembalikkan meja\\b", "membalikkan keadaan"));
        UNIVERSAL_RULES.add(new Rule("\\bkupu-kupu di perutku\\b", "perutku rasanya mulas karena gugup"));
        UNIVERSAL_RULES.add(new Rule("\\bsemua telinga\\b", "siap mendengarkan"));
        UNIVERSAL_RULES.add(new Rule("\\baku semua telinga\\b", "aku siap mendengarkan"));
        UNIVERSAL_RULES.add(new Rule("\\bmenangkap napasku\\b", "mengatur napasku"));
        UNIVERSAL_RULES.add(new Rule("\\bmenangkap napas\\b", "mengatur napas"));
        UNIVERSAL_RULES.add(new Rule("\\bonii-chan\\b", "Kakak"));
        UNIVERSAL_RULES.add(new Rule("\\bonii-san\\b", "Kakak"));
        UNIVERSAL_RULES.add(new Rule("\\bonee-chan\\b", "Kakak"));
    }

    private static void initUniversalBatch7() {
        UNIVERSAL_RULES.add(new Rule("\\bonee-san\\b", "Kakak"));
        UNIVERSAL_RULES.add(new Rule("\\baniki\\b", "Bang"));
        UNIVERSAL_RULES.add(new Rule("\\bojou-sama\\b", "Nona Muda"));
        UNIVERSAL_RULES.add(new Rule("\\botou-san\\b", "Ayah"));
        UNIVERSAL_RULES.add(new Rule("\\bokaa-san\\b", "Ibu"));
        UNIVERSAL_RULES.add(new Rule("\\bojii-san\\b", "Kakek"));
        UNIVERSAL_RULES.add(new Rule("\\bojii-chan\\b", "Kakek"));
        UNIVERSAL_RULES.add(new Rule("\\bobaa-san\\b", "Nenek"));
        UNIVERSAL_RULES.add(new Rule("\\bobaa-chan\\b", "Nenek"));
        UNIVERSAL_RULES.add(new Rule("\\bosananajimi\\b", "teman masa kecil"));
        UNIVERSAL_RULES.add(new Rule("\\bgyaru\\b", "cewek gyaru"));
        UNIVERSAL_RULES.add(new Rule("\\bchuunibyou\\b", "sindrom chuunibyou"));
        UNIVERSAL_RULES.add(new Rule("\\bkokuhaku\\b", "nembak perasaan"));
        UNIVERSAL_RULES.add(new Rule("\\binti emas\\b", "Inti Emas (Golden Core)"));
        UNIVERSAL_RULES.add(new Rule("\\bpembentukan inti\\b", "Pembentukan Inti"));
        UNIVERSAL_RULES.add(new Rule("\\bpembangunan fondasi\\b", "Tahap Fondasi"));
        UNIVERSAL_RULES.add(new Rule("\\bjiwa baru lahir\\b", "Jiwa Nascent"));
        UNIVERSAL_RULES.add(new Rule("\\bkesengsaraan surgawi\\b", "Kesengsaraan Langit (Tribulation)"));
        UNIVERSAL_RULES.add(new Rule("\\bkesengsaraan petir\\b", "Petir Kesengsaraan"));
        UNIVERSAL_RULES.add(new Rule("\\bsaudara senior\\b", "Kakak Senior"));
        UNIVERSAL_RULES.add(new Rule("\\bsaudari senior\\b", "Kakak Senior"));
        UNIVERSAL_RULES.add(new Rule("\\bsaudara junior\\b", "Adik Junior"));
        UNIVERSAL_RULES.add(new Rule("\\bsaudari junior\\b", "Adik Junior"));
        UNIVERSAL_RULES.add(new Rule("\\bsekte seni bela diri\\b", "Sekte Bela Diri"));
        UNIVERSAL_RULES.add(new Rule("\\benergi sejati\\b", "Qi Sejati"));
        UNIVERSAL_RULES.add(new Rule("\\benergi spiritual\\b", "Qi Spiritual"));
        UNIVERSAL_RULES.add(new Rule("\\bqi dalam\\b", "Tenaga Dalam"));
        UNIVERSAL_RULES.add(new Rule("\\bkaisar langit\\b", "Kaisar Langit"));
        UNIVERSAL_RULES.add(new Rule("\\btetua agung\\b", "Tetua Agung"));
        UNIVERSAL_RULES.add(new Rule("\\bpemimpin sekte\\b", "Ketua Sekte"));
        UNIVERSAL_RULES.add(new Rule("\\bistana belakang\\b", "Istana Dalam"));
        UNIVERSAL_RULES.add(new Rule("\\bselir tingkat tinggi\\b", "Selir Utama"));
        UNIVERSAL_RULES.add(new Rule("\\bpencicip makanan\\b", "Pencicip Racun"));
        UNIVERSAL_RULES.add(new Rule("\\bapoteker istana\\b", "Tabib Istana"));
        UNIVERSAL_RULES.add(new Rule("\\bpagma'?s successor\\b", "Keturunan Pagma"));
        UNIVERSAL_RULES.add(new Rule("\\bpagma'?s descendant\\b", "Keturunan Pagma"));
        UNIVERSAL_RULES.add(new Rule("\\bpenerus pagma\\b", "Keturunan Pagma"));
        UNIVERSAL_RULES.add(new Rule("\\bpagma'?s (?:rare book|tome)\\b", "Kitab Langka Pagma"));
        UNIVERSAL_RULES.add(new Rule("\\bbuku langka pagma\\b", "Kitab Langka Pagma"));
        UNIVERSAL_RULES.add(new Rule("\\blegendary craftsman'?s tome\\b", "Kitab Pengrajin Legendaris"));
        UNIVERSAL_RULES.add(new Rule("\\blegendary blacksmith\\b", "Pandai Besi Legendaris"));
        UNIVERSAL_RULES.add(new Rule("\\bblacksmith master creation\\b", "Kreasi Master Pandai Besi"));
        UNIVERSAL_RULES.add(new Rule("\\bbreath of the legendary blacksmith\\b", "Napas Pandai Besi Legendaris"));
        UNIVERSAL_RULES.add(new Rule("\\bblue orichalcum\\b", "Orichalcum Biru"));
        UNIVERSAL_RULES.add(new Rule("\\bpavranium\\b", "Pavranium"));
        UNIVERSAL_RULES.add(new Rule("\\btzedakah guild\\b", "Guild Tzedakah"));
        UNIVERSAL_RULES.add(new Rule("\\bblood witch\\b", "Penyihir Darah"));
        UNIVERSAL_RULES.add(new Rule("\\bblood warrior\\b", "Blood Warrior"));
        UNIVERSAL_RULES.add(new Rule("\\bdoran'?s ring\\b", "Cincin Doran"));
        UNIVERSAL_RULES.add(new Rule("\\bdoran'?s sword\\b", "Pedang Doran"));
        UNIVERSAL_RULES.add(new Rule("\\bbehen archipelago\\b", "Kepulauan Behen"));
        UNIVERSAL_RULES.add(new Rule("\\bbairan village\\b", "Desa Bairan"));
        UNIVERSAL_RULES.add(new Rule("\\bwinston village\\b", "Desa Winston"));
        UNIVERSAL_RULES.add(new Rule("\\beternal kingdom\\b", "Kerajaan Eternal"));
        UNIVERSAL_RULES.add(new Rule("\\bearl steim\\b", "Earl Steim"));
        UNIVERSAL_RULES.add(new Rule("\\bearl ashur\\b", "Earl Ashur"));
        UNIVERSAL_RULES.add(new Rule("\\blord winston\\b", "Penguasa Winston"));
        UNIVERSAL_RULES.add(new Rule("\\bchurch of rebecca\\b", "Gereja Rebecca"));
        UNIVERSAL_RULES.add(new Rule("\\bgoddess rebecca\\b", "Dewi Rebecca"));
        UNIVERSAL_RULES.add(new Rule("\\bchurch of yatan\\b", "Gereja Yatan"));
        UNIVERSAL_RULES.add(new Rule("\\bgod yatan\\b", "Dewa Yatan"));
        UNIVERSAL_RULES.add(new Rule("\\bpope drevigo\\b", "Paus Drevigo"));
        UNIVERSAL_RULES.add(new Rule("\\bsupercomputer morpheus\\b", "Superkomputer Morpheus"));
        UNIVERSAL_RULES.add(new Rule("\\bgreat demon\\b", "Iblis Agung"));
        UNIVERSAL_RULES.add(new Rule("\\bhidden quest\\b", "Quest Tersembunyi"));
        UNIVERSAL_RULES.add(new Rule("\\blinked quest\\b", "Quest Berantai"));
        UNIVERSAL_RULES.add(new Rule("\\bss-rank quest\\b", "Quest Peringkat SS"));
        UNIVERSAL_RULES.add(new Rule("\\bs-rank quest\\b", "Quest Peringkat S"));
        UNIVERSAL_RULES.add(new Rule("\\blegendary (?:class|job)\\b", "Profesi Legendaris"));
        UNIVERSAL_RULES.add(new Rule("\\bepic (?:class|job)\\b", "Profesi Epic"));
        UNIVERSAL_RULES.add(new Rule("\\bunique (?:class|job)\\b", "Profesi Unique"));
        UNIVERSAL_RULES.add(new Rule("\\brare (?:class|job)\\b", "Profesi Rare"));
        UNIVERSAL_RULES.add(new Rule("\\bnormal (?:class|job)\\b", "Profesi Normal"));
        UNIVERSAL_RULES.add(new Rule("\\bbaal'?s contractor\\b", "Kontraktor Baal"));
        UNIVERSAL_RULES.add(new Rule("\\bsword saint\\b", "Santo Pedang"));
        UNIVERSAL_RULES.add(new Rule("\\bbow saint\\b", "Santo Busur"));
        UNIVERSAL_RULES.add(new Rule("\\bshadow striker\\b", "Shadow Striker"));
        UNIVERSAL_RULES.add(new Rule("\\bproduction method\\b", "Metode Pembuatan"));
    }

    private static void initCasualBatch1() {
        CASUAL_RULES.add(new Rule("\\bsaya tidak tahu anda ada di sana\\b", "aku nggak tahu kamu ada di sana"));
        CASUAL_RULES.add(new Rule("\\bsaya nggak tahu anda ada di sana\\b", "aku nggak tahu kamu ada di sana"));
        CASUAL_RULES.add(new Rule("\\baku tidak tahu anda ada di sana\\b", "aku nggak tahu kamu ada di sana"));
        CASUAL_RULES.add(new Rule("\\baku nggak tahu anda ada di sana\\b", "aku nggak tahu kamu ada di sana"));
        CASUAL_RULES.add(new Rule("\\bsaya tidak tahu kamu ada di sana\\b", "aku nggak tahu kamu ada di sana"));
        CASUAL_RULES.add(new Rule("\\bsaya nggak tahu kamu ada di sana\\b", "aku nggak tahu kamu ada di sana"));
        CASUAL_RULES.add(new Rule("\\baku tidak melakukan apa pun\\b", "aku nggak ngelakuin apa-apa"));
        CASUAL_RULES.add(new Rule("\\btidak melakukan apa pun\\b", "nggak ngelakuin apa-apa"));
        CASUAL_RULES.add(new Rule("\\bingin saya mengajari anda\\b", "mau kuajari"));
        CASUAL_RULES.add(new Rule("\\banda perlu baju ganti\\b", "kamu butuh baju ganti"));
        CASUAL_RULES.add(new Rule("\\bapa yang kamu bicarakan\\b", "ngomong apa sih kamu"));
        CASUAL_RULES.add(new Rule("\\bapa yang kau bicarakan\\b", "ngomong apa sih kamu"));
        CASUAL_RULES.add(new Rule("\\bkoleksi hotnya\\b", "cewek-cewek cakepnya"));
        CASUAL_RULES.add(new Rule("\\bkoleksi hot\\b", "cewek-cewek cakep"));
        CASUAL_RULES.add(new Rule("\\bkalau dengerin saya\\b", "kalau menurutku sih"));
        CASUAL_RULES.add(new Rule("\\bkalau dengerin aku\\b", "kalau menurutku sih"));
        CASUAL_RULES.add(new Rule("\\bgak usah dikira\\b", "nggak usah ditanya lagi"));
        CASUAL_RULES.add(new Rule("\\bbertahanlah\\b", "tunggu sebentar"));
        CASUAL_RULES.add(new Rule("\\baku melihat\\b", "begitu rupanya"));
        CASUAL_RULES.add(new Rule("\\bkulihat\\b", "begitu rupanya"));
        CASUAL_RULES.add(new Rule("\\btidak apa-apa\\b", "nggak apa-apa"));
        CASUAL_RULES.add(new Rule("\\btak apa-apa\\b", "nggak apa-apa"));
        CASUAL_RULES.add(new Rule("\\btak apa\\b", "nggak apa-apa"));
        CASUAL_RULES.add(new Rule("\\bbaik-baik saja\\b", "nggak apa-apa"));
        CASUAL_RULES.add(new Rule("\\btutup mulutmu\\b", "diem"));
        CASUAL_RULES.add(new Rule("\\btidak ada jalan\\b", "nggak mungkin"));
        CASUAL_RULES.add(new Rule("\\btidak mungkin\\b", "nggak mungkin"));
        CASUAL_RULES.add(new Rule("\\btidak bisa\\b", "nggak bisa"));
        CASUAL_RULES.add(new Rule("\\btak bisa\\b", "nggak bisa"));
        CASUAL_RULES.add(new Rule("\\btidak dapat\\b", "nggak bisa"));
        CASUAL_RULES.add(new Rule("\\btidak akan\\b", "nggak bakal"));
        CASUAL_RULES.add(new Rule("\\btak akan\\b", "nggak bakal"));
        CASUAL_RULES.add(new Rule("\\btidak tahu\\b", "nggak tahu"));
        CASUAL_RULES.add(new Rule("\\btak tahu\\b", "nggak tahu"));
        CASUAL_RULES.add(new Rule("\\btidak mau\\b", "nggak mau"));
        CASUAL_RULES.add(new Rule("\\btak mau\\b", "nggak mau"));
        CASUAL_RULES.add(new Rule("\\btidak pernah\\b", "nggak pernah"));
        CASUAL_RULES.add(new Rule("\\btak pernah\\b", "nggak pernah"));
        CASUAL_RULES.add(new Rule("\\btidak perlu\\b", "nggak usah"));
        CASUAL_RULES.add(new Rule("\\btak perlu\\b", "nggak usah"));
        CASUAL_RULES.add(new Rule("\\btidak usah\\b", "nggak usah"));
        CASUAL_RULES.add(new Rule("\\btidak boleh\\b", "nggak boleh"));
        CASUAL_RULES.add(new Rule("\\btak boleh\\b", "nggak boleh"));
        CASUAL_RULES.add(new Rule("\\btidak ada\\b", "nggak ada"));
        CASUAL_RULES.add(new Rule("\\btak ada\\b", "nggak ada"));
        CASUAL_RULES.add(new Rule("\\b(?:emang\\s+)?(?:sudah|udah)\\s+kuduga\\b", "emang udah kuduga"));
        CASUAL_RULES.add(new Rule("\\bhanya saja\\b", "cuma"));
        CASUAL_RULES.add(new Rule("\\bsudah\\b", "udah"));
        CASUAL_RULES.add(new Rule("\\bsaja\\b", "aja"));
        CASUAL_RULES.add(new Rule("\\bhanya\\b", "cuma"));
        CASUAL_RULES.add(new Rule("\\bmengapa kamu\\b", "kenapa kamu"));
        CASUAL_RULES.add(new Rule("\\bmengapa kau\\b", "kenapa kamu"));
        CASUAL_RULES.add(new Rule("\\bmengapa dia\\b", "kenapa dia"));
        CASUAL_RULES.add(new Rule("\\bmengapa\\b", "kenapa"));
        CASUAL_RULES.add(new Rule("\\bbagaimana kalau\\b", "gimana kalau"));
        CASUAL_RULES.add(new Rule("\\bbagaimana jika\\b", "gimana kalau"));
        CASUAL_RULES.add(new Rule("\\bbagaimana cara\\b", "gimana cara"));
        CASUAL_RULES.add(new Rule("\\bbagaimana bisa\\b", "gimana bisa"));
        CASUAL_RULES.add(new Rule("\\blelaki itu\\b", "cowok itu"));
        CASUAL_RULES.add(new Rule("\\bpria itu\\b", "cowok itu"));
        CASUAL_RULES.add(new Rule("\\bwanita itu\\b", "cewek itu"));
        CASUAL_RULES.add(new Rule("\\bgadis itu\\b", "cewek itu"));
        CASUAL_RULES.add(new Rule("\\bwanita tersebut\\b", "cewek itu"));
        CASUAL_RULES.add(new Rule("\\blelaki tersebut\\b", "cowok itu"));
        CASUAL_RULES.add(new Rule("\\btersesatlah\\b", "pergi sana"));
        CASUAL_RULES.add(new Rule("\\bjangan khawatir\\b", "tenang aja"));
        CASUAL_RULES.add(new Rule("\\btak perlu khawatir\\b", "tenang aja"));
        CASUAL_RULES.add(new Rule("\\bterima kasih banyak\\b", "makasih banyak"));
        CASUAL_RULES.add(new Rule("\\bterima kasih\\b", "makasih"));
        CASUAL_RULES.add(new Rule("\\bdengarkan aku\\b", "dengerin aku"));
        CASUAL_RULES.add(new Rule("\\bdengarkan\\b", "dengerin"));
        CASUAL_RULES.add(new Rule("\\bsangat banyak\\b", "banget"));
        CASUAL_RULES.add(new Rule("\\bbodoh sekali\\b", "bodoh banget"));
        CASUAL_RULES.add(new Rule("\\bbagus sekali\\b", "hebat banget"));
        CASUAL_RULES.add(new Rule("\\bdi sini untuk mengambil\\b", "mau mengambil"));
        CASUAL_RULES.add(new Rule("\\banda berpikir\\b", "kamu rasa"));
        CASUAL_RULES.add(new Rule("\\bmau bagaimana lagi\\b", "mau gimana lagi"));
        CASUAL_RULES.add(new Rule("\\bmau tak mau\\b", "mau nggak mau"));
        CASUAL_RULES.add(new Rule("\\bapa yang terjadi\\b", "ada apa sih"));
        CASUAL_RULES.add(new Rule("\\bkamu tahu\\b", "tahu nggak"));
        CASUAL_RULES.add(new Rule("\\bapakah kamu serius\\b", "kamu serius"));
        CASUAL_RULES.add(new Rule("\\bapakah kamu bercanda\\b", "kamu bercanda ya"));
        CASUAL_RULES.add(new Rule("\\bbukan urusanmu\\b", "bukan urusanmu kok"));
        CASUAL_RULES.add(new Rule("\\btidak akan kumaafkan\\b", "nggak bakal kumaafkan"));
        CASUAL_RULES.add(new Rule("\\btak akan kumaafkan\\b", "nggak bakal kumaafkan"));
    }

    private static void initCasualBatch2() {
        CASUAL_RULES.add(new Rule("\\bsungguh merepotkan\\b", "merepotkan banget"));
        CASUAL_RULES.add(new Rule("\\bapa yang kamu maksud\\b", "maksudmu apa"));
        CASUAL_RULES.add(new Rule("\\bapa maksudmu\\b", "maksudmu apa"));
        CASUAL_RULES.add(new Rule("\\bmana aku tahu\\b", "mana kutahu"));
        CASUAL_RULES.add(new Rule("\\bbagaimana aku bisa tahu\\b", "mana kutahu"));
        CASUAL_RULES.add(new Rule("\\bapakah itu benar\\b", "beneran nih"));
        CASUAL_RULES.add(new Rule("\\bpantas saja\\b", "pantesan"));
        CASUAL_RULES.add(new Rule("\\bjangan konyol\\b", "yang bener aja"));
        CASUAL_RULES.add(new Rule("\\bjangan bercanda\\b", "jangan bercanda deh"));
        CASUAL_RULES.add(new Rule("\\btentu saja\\b", "tentu saja"));
        CASUAL_RULES.add(new Rule("\\bsama sekali tidak\\b", "sama sekali nggak"));
        CASUAL_RULES.add(new Rule("\\bsama sekali tak\\b", "sama sekali nggak"));
        CASUAL_RULES.add(new Rule("\\bbiar aku yang urus ini\\b", "biar aku yang urus"));
        CASUAL_RULES.add(new Rule("\\bbiar aku yang menangani ini\\b", "biar aku yang urus"));
        CASUAL_RULES.add(new Rule("\\bgampang sekali\\b", "gampang banget"));
        CASUAL_RULES.add(new Rule("\\bmudah sekali\\b", "gampang banget"));
        CASUAL_RULES.add(new Rule("\\bapa-apaan ini\\b", "ada apa-apaan nih"));
        CASUAL_RULES.add(new Rule("\\bsedikit pun\\b", "dikit pun"));
        CASUAL_RULES.add(new Rule("\\bbercanda kan\\b", "bercanda ya"));
        CASUAL_RULES.add(new Rule("\\bjangan macam-macam\\b", "jangan macam-macam deh"));
        CASUAL_RULES.add(new Rule("\\bada apa sebenarnya\\b", "ada apa sih sebenarnya"));
        CASUAL_RULES.add(new Rule("\\bkan sudah kubilang\\b", "kan udah kubilang"));
        CASUAL_RULES.add(new Rule("\\bsudah kubilang\\b", "kan udah kubilang"));
        CASUAL_RULES.add(new Rule("\\btidak mungkin kan\\b", "nggak mungkin kan"));
        CASUAL_RULES.add(new Rule("\\btak mungkin kan\\b", "nggak mungkin kan"));
        CASUAL_RULES.add(new Rule("\\bapa yang salah denganmu\\b", "kamu kenapa sih"));
        CASUAL_RULES.add(new Rule("\\bapa yang salah denganku\\b", "aku kenapa sih"));
        CASUAL_RULES.add(new Rule("\\bapa yang salah\\b", "ada apa sih"));
        CASUAL_RULES.add(new Rule("\\bbukan urusanku\\b", "bukan urusanku kok"));
        CASUAL_RULES.add(new Rule("\\baku tidak peduli\\b", "aku nggak peduli"));
        CASUAL_RULES.add(new Rule("\\btak peduli\\b", "nggak peduli"));
        CASUAL_RULES.add(new Rule("\\bsampai jumpa nanti\\b", "sampai ketemu lagi"));
        CASUAL_RULES.add(new Rule("\\btenanglah sedikit\\b", "santai dikit dong"));
        CASUAL_RULES.add(new Rule("\\btenang sedikit\\b", "santai dikit dong"));
        CASUAL_RULES.add(new Rule("\\bapa maumu\\b", "mau apa sih kamu"));
        CASUAL_RULES.add(new Rule("\\bapa yang kamu inginkan\\b", "kamu mau apa sih"));
        CASUAL_RULES.add(new Rule(",\\s*bukan\\s*\\?", ", kan?"));
        CASUAL_RULES.add(new Rule(",\\s*tidakkah begitu\\s*\\?", ", kan?"));
        CASUAL_RULES.add(new Rule(",\\s*bukankah begitu\\s*\\?", ", kan?"));
        CASUAL_RULES.add(new Rule(",\\s*benar kan\\s*\\?", ", ya kan?"));
        CASUAL_RULES.add(new Rule("\\bapakah kamu baik-baik saja\\b", "kamu nggak apa-apa"));
        CASUAL_RULES.add(new Rule("\\bapakah kamu nggak apa-apa\\b", "kamu nggak apa-apa"));
        CASUAL_RULES.add(new Rule("\\bapakah kau baik-baik saja\\b", "kamu nggak apa-apa"));
        CASUAL_RULES.add(new Rule("\\bapakah kamu yakin\\b", "kamu yakin"));
        CASUAL_RULES.add(new Rule("\\bapakah kau yakin\\b", "kamu yakin"));
        CASUAL_RULES.add(new Rule("\\bapakah kamu sudah gila\\b", "kamu udah gila ya"));
        CASUAL_RULES.add(new Rule("\\bapakah kamu gila\\b", "kamu gila ya"));
        CASUAL_RULES.add(new Rule("\\bapakah itu mungkin\\b", "memangnya mungkin"));
        CASUAL_RULES.add(new Rule("\\bapakah kamu mengerti\\b", "kamu ngerti kan"));
        CASUAL_RULES.add(new Rule("\\baku mengerti sekarang\\b", "oh, sekarang aku paham"));
        CASUAL_RULES.add(new Rule("\\baku tidak mengerti\\b", "aku nggak ngerti"));
        CASUAL_RULES.add(new Rule("\\bnggak mengerti\\b", "nggak ngerti"));
        CASUAL_RULES.add(new Rule("\\bcepatlah sedikit\\b", "cepetan dikit dong"));
        CASUAL_RULES.add(new Rule("\\btunggu aku\\b", "tungguin aku"));
        CASUAL_RULES.add(new Rule("\\blihat itu\\b", "lihat deh itu"));
        CASUAL_RULES.add(new Rule("\\bhentikan itu\\b", "udah dong, hentikan"));
        CASUAL_RULES.add(new Rule("\\bserahkan padaku\\b", "serahin aja padaku"));
        CASUAL_RULES.add(new Rule("\\bserahkan saja padaku\\b", "serahin aja padaku"));
        CASUAL_RULES.add(new Rule("\\bpercayalah padaku\\b", "percaya deh sama aku"));
        CASUAL_RULES.add(new Rule("\\bjangan khawatir tentang itu\\b", "nggak usah dipikirin"));
        CASUAL_RULES.add(new Rule("\\baku baru saja\\b", "aku barusan"));
        CASUAL_RULES.add(new Rule("\\bbaru saja\\b", "barusan"));
        CASUAL_RULES.add(new Rule("\\bbenar-benar hebat\\b", "keren banget"));
        CASUAL_RULES.add(new Rule("\\bluar biasa sekali\\b", "keren banget"));
        CASUAL_RULES.add(new Rule("\\bsangat keren\\b", "keren banget"));
        CASUAL_RULES.add(new Rule("\\bsangat manis\\b", "manis banget"));
        CASUAL_RULES.add(new Rule("\\bsangat imut\\b", "imut banget"));
        CASUAL_RULES.add(new Rule("\\bsangat kuat\\b", "kuat banget"));
        CASUAL_RULES.add(new Rule("\\bsangat cepat\\b", "cepet banget"));
        CASUAL_RULES.add(new Rule("\\bsangat lambat\\b", "lambat banget"));
        CASUAL_RULES.add(new Rule("\\bsangat lezat\\b", "enak banget"));
        CASUAL_RULES.add(new Rule("\\bsangat enak\\b", "enak banget"));
        CASUAL_RULES.add(new Rule("\\bsangat menakutkan\\b", "serem banget"));
        CASUAL_RULES.add(new Rule("\\bsangat aneh\\b", "aneh banget"));
        CASUAL_RULES.add(new Rule("\\bsangat berisik\\b", "berisik banget"));
        CASUAL_RULES.add(new Rule("\\baku sangat senang\\b", "aku seneng banget"));
        CASUAL_RULES.add(new Rule("\\baku sangat lelah\\b", "aku capek banget"));
        CASUAL_RULES.add(new Rule("\\bsangat lelah\\b", "capek banget"));
        CASUAL_RULES.add(new Rule("\\baku sangat lapar\\b", "aku laper banget"));
        CASUAL_RULES.add(new Rule("\\bsangat lapar\\b", "laper banget"));
    }

    private static void initAigtxSmooth() {
        AIGTX_SMOOTH_RULES.add(new Rule("\\baku melihat\\b", "begitu rupanya"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\bkulihat\\b", "begitu rupanya"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\btidak ada jalan\\b", "tidak mungkin"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\btak ada jalan\\b", "tidak mungkin"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\btersesatlah\\b", "pergi sana"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\btak perlu khawatir\\b", "tenang saja"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\bgak usah dikira\\b", "tidak usah ditanya lagi"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\bcelah di tongkat\\b", "retakan di tongkat"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\btinta sulap\\b", "tinta sihir"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\bmelakukan casting\\b", "merapal sihir"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\bcasting seal\\b", "segel sihir"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\bberi aku istirahat\\b", "yang benar saja"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\bmelayanimu dengan benar\\b", "rasakan itu"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\bitu tidak bisa ditolong\\b", "mau bagaimana lagi"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\bsepotong kue\\b", "sangat mudah"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\bdi bawah cuaca\\b", "kurang enak badan"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\bbukan di jam tanganku\\b", "tidak akan kubiarkan"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\bambil dengan mudah\\b", "santai saja"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\bdi atas rumah\\b", "gratis dari kedai"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\bdi atas tubuh matiku\\b", "langkahi dulu mayatku"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\bjangan biarkan penjagamu turun\\b", "jangan lengah"));
        AIGTX_SMOOTH_RULES.add(new Rule("\\bperhatikan punggungmu\\b", "waspadalah di belakangmu"));
    }

    private static void initCasualPronouns() {
        CASUAL_PRONOUN_RULES.add(new Rule("\\bmenurut saya\\b", m -> Character.isLowerCase(m.group(0).charAt(0)) ? "menurutku" : "Menurutku"));
        CASUAL_PRONOUN_RULES.add(new Rule("\\bmilik saya\\b", "milikku"));
        CASUAL_PRONOUN_RULES.add(new Rule("\\buntuk saya\\b", "untukku"));
        CASUAL_PRONOUN_RULES.add(new Rule("\\bkepada saya\\b", "kepadaku"));
        CASUAL_PRONOUN_RULES.add(new Rule("\\bapakah saya\\b", m -> Character.isLowerCase(m.group(0).charAt(0)) ? "apa aku" : "Apa aku"));
        CASUAL_PRONOUN_RULES.add(new Rule("\\bapakah anda\\b", m -> Character.isLowerCase(m.group(0).charAt(0)) ? "apa kamu" : "Apa kamu"));
        CASUAL_PRONOUN_RULES.add(new Rule("\\bbisakah saya\\b", m -> Character.isLowerCase(m.group(0).charAt(0)) ? "bisa aku" : "Bisa aku"));
        CASUAL_PRONOUN_RULES.add(new Rule("\\bbisakah anda\\b", m -> Character.isLowerCase(m.group(0).charAt(0)) ? "bisa kamu" : "Bisa kamu"));
        CASUAL_PRONOUN_RULES.add(new Rule("\\bsaya tidak\\b", m -> Character.isLowerCase(m.group(0).charAt(0)) ? "aku nggak" : "Aku nggak"));
        CASUAL_PRONOUN_RULES.add(new Rule("\\bsaya nggak\\b", m -> Character.isLowerCase(m.group(0).charAt(0)) ? "aku nggak" : "Aku nggak"));
        CASUAL_PRONOUN_RULES.add(new Rule("\\bsaya mau\\b", m -> Character.isLowerCase(m.group(0).charAt(0)) ? "aku mau" : "Aku mau"));
        CASUAL_PRONOUN_RULES.add(new Rule("\\bsaya harus\\b", m -> Character.isLowerCase(m.group(0).charAt(0)) ? "aku harus" : "Aku harus"));
        CASUAL_PRONOUN_RULES.add(new Rule("\\bsaya bisa\\b", m -> Character.isLowerCase(m.group(0).charAt(0)) ? "aku bisa" : "Aku bisa"));
        CASUAL_PRONOUN_RULES.add(new Rule("\\bsaya akan\\b", m -> Character.isLowerCase(m.group(0).charAt(0)) ? "aku akan" : "Aku akan"));
        CASUAL_PRONOUN_RULES.add(new Rule("\\bsaya rasa\\b", m -> Character.isLowerCase(m.group(0).charAt(0)) ? "aku rasa" : "Aku rasa"));
        CASUAL_PRONOUN_RULES.add(new Rule("\\bsaya pikir\\b", m -> Character.isLowerCase(m.group(0).charAt(0)) ? "kupikir" : "Kupikir"));
        CASUAL_PRONOUN_RULES.add(new Rule("\\bberi saya\\b", m -> Character.isLowerCase(m.group(0).charAt(0)) ? "beri aku" : "Beri aku"));
        CASUAL_PRONOUN_RULES.add(new Rule("\\bsaya\\b", m -> Character.isUpperCase(m.group(0).charAt(0)) ? "Aku" : "aku"));
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
        // 1. Terapkan istilah universal (anime terms, game titles, Overgeared Satisfy)
        String result = applyUniversalLexicon(text);

        // 2. Scene detector: formal vs casual
        boolean isFormal = forceFormal || FORMAL_INDICATORS_RE.matcher(result).find();
        if (!isFormal) {
            for (Rule rule : CASUAL_RULES) {
                result = applyRulePreservingCase(rule, result);
            }
            for (Rule rule : AIGTX_SMOOTH_RULES) {
                result = applyRulePreservingCase(rule, result);
            }
        }
        return result;
    }

    public static List<SubtitleCue> sanitizeCuesWithContext(List<SubtitleCue> cues, String mode, int window, double maxGapSec) {
        if (cues == null || cues.isEmpty()) return cues != null ? cues : new ArrayList<>();
        int n = cues.size();
        boolean[] directFormal = new boolean[n];
        for (int i = 0; i < n; i++) {
            String txt = cues.get(i).text != null ? cues.get(i).text : "";
            directFormal[i] = FORMAL_INDICATORS_RE.matcher(txt).find();
        }

        boolean[] contextualFormal = new boolean[n];
        System.arraycopy(directFormal, 0, contextualFormal, 0, n);
        double maxGapMs = maxGapSec * 1000.0;

        for (int i = 0; i < n; i++) {
            if (directFormal[i]) continue;
            long cStart = cues.get(i).startMs;
            long cEnd = cues.get(i).endMs;
            for (int j = Math.max(0, i - window); j <= Math.min(n - 1, i + window); j++) {
                if (j == i || !directFormal[j]) continue;
                long nStart = cues.get(j).startMs;
                long nEnd = cues.get(j).endMs;
                double gap = j < i ? Math.max(0, cStart - nEnd) : Math.max(0, nStart - cEnd);
                if (gap <= maxGapMs) {
                    contextualFormal[i] = true;
                    break;
                }
            }
        }

        List<SubtitleCue> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            SubtitleCue c = cues.get(i);
            String sanitized = sanitizeGtxPlusId(c.text, contextualFormal[i]);
            out.add(new SubtitleCue(c.startMs, c.endMs, sanitized));
        }
        return out;
    }

    public static List<SubtitleCue> automateOpenSubtitlesPronouns(List<SubtitleCue> cues, String targetLang, int window, double maxGapSec) {
        if (cues == null || cues.isEmpty()) return cues != null ? cues : new ArrayList<>();
        String lang = (targetLang != null ? targetLang : "id").toLowerCase();
        if (!lang.equals("id") && !lang.equals("ind") && !lang.equals("indonesian")) {
            return cues;
        }

        int n = cues.size();
        boolean[] directFormal = new boolean[n];
        boolean[] directRage = new boolean[n];
        boolean[] directMonologue = new boolean[n];

        for (int i = 0; i < n; i++) {
            String txt = cues.get(i).text.trim();
            directFormal[i] = FORMAL_INDICATORS_RE.matcher(txt).find();
            directRage[i] = RAGE_OUTBURST_RE.matcher(txt).find();
            directMonologue[i] = MONOLOGUE_RE.matcher(txt).find();
        }

        boolean[] contextualFormal = new boolean[n];
        System.arraycopy(directFormal, 0, contextualFormal, 0, n);
        double maxGapMs = maxGapSec * 1000.0;

        for (int i = 0; i < n; i++) {
            if (directFormal[i] || directRage[i] || directMonologue[i]) continue;
            long cStart = cues.get(i).startMs;
            long cEnd = cues.get(i).endMs;
            for (int j = Math.max(0, i - window); j <= Math.min(n - 1, i + window); j++) {
                if (j == i || !directFormal[j]) continue;
                long nStart = cues.get(j).startMs;
                long nEnd = cues.get(j).endMs;
                double gap = j < i ? Math.max(0, cStart - nEnd) : Math.max(0, nStart - cEnd);
                if (gap <= maxGapMs) {
                    contextualFormal[i] = true;
                    break;
                }
            }
        }

        List<SubtitleCue> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            SubtitleCue c = cues.get(i);
            String txt = c.text.trim();
            if (txt.isEmpty()) {
                out.add(c);
                continue;
            }

            boolean isF = contextualFormal[i];
            boolean isR = directRage[i];
            boolean isM = directMonologue[i];

            String newTxt = txt;
            if (!isF || (isR || isM)) {
                for (Rule r : CASUAL_PRONOUN_RULES) {
                    newTxt = applyRulePreservingCase(r, newTxt);
                }
                if (isR) {
                    newTxt = Pattern.compile("\\bAnda\\b", Pattern.CASE_INSENSITIVE).matcher(newTxt)
                            .replaceAll(matchResult -> Character.isUpperCase(matchResult.group().charAt(0)) ? "Kau" : "kau");
                } else {
                    newTxt = Pattern.compile("\\bAnda\\b", Pattern.CASE_INSENSITIVE).matcher(newTxt)
                            .replaceAll(matchResult -> Character.isUpperCase(matchResult.group().charAt(0)) ? "Kamu" : "kamu");
                }
            }
            out.add(new SubtitleCue(c.startMs, c.endMs, newTxt));
        }
        return out;
    }

    private static String applyRulePreservingCase(Rule rule, String input) {
        Matcher matcher = rule.pattern.matcher(input);
        if (!matcher.find()) return input;

        StringBuffer sb = new StringBuffer(input.length() + 32);
        do {
            if (rule.replacer != null) {
                String repl = rule.replacer.replace(matcher);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(repl));
            } else {
                String repStr = rule.replacement;
                String orig = matcher.group(0);
                if (repStr.contains("$")) {
                    matcher.appendReplacement(sb, repStr);
                } else {
                    String repl;
                    if (orig != null && !orig.isEmpty() && Character.isUpperCase(orig.charAt(0))) {
                        repl = Character.toUpperCase(repStr.charAt(0)) + (repStr.length() > 1 ? repStr.substring(1) : "");
                    } else {
                        repl = repStr;
                    }
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(repl));
                }
            }
        } while (matcher.find());
        matcher.appendTail(sb);
        return sb.toString();
    }
}
