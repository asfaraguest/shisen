package id.shisen.app.data.subtitle;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import id.shisen.app.data.service.HttpClientProvider;
import id.shisen.app.util.SecureStore;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * AiSubtitleService provides BYOK (Bring Your Own Key) LLM-powered subtitle translation
 * parity with Tatap backend, supporting Google Gemini, Groq, OpenAI, OpenRouter, and Ollama Cloud/Local.
 *
 * Security guardrails:
 * - API keys are stored exclusively in hardware-backed SecureStore (AES-256-GCM).
 * - Zero logging: keys are never printed to logcat or console.
 */
public class AiSubtitleService {

    public static final String KEY_PREF_LLM = "byok_llm_api_key";
    public static final String KEY_PREF_ENDPOINT = "byok_llm_custom_endpoint";
    public static final String KEY_PREF_CUSTOM_PROMPT = "byok_llm_custom_prompt";
    public static final String KEY_PREF_MODEL = "byok_llm_custom_model";

    public static final String DEFAULT_FANSUB_PROMPT_ID = """
Kamu adalah penerjemah fansub anime Indonesia berpengalaman. Terjemahkan subtitle English menjadi bahasa Indonesia yang luwes, enak dibaca, dan adaptif mengikuti situasi adegan (scene) — BUKAN bahasa kaku ala mesin penerjemah.

Aturan Nada Bahasa Sesuai Konteks Adegan:
1. Adegan Formal / Berwibawa / Kerajaan / Militer:
   - Situasi: Audiensi raja/bangsawan/tetua, ksatria melapor ke komandan, pidato resmi, pelayan/maid melayani tuannya, adegan ritual.
   - Kata ganti: "Saya/Anda", "Tuan/Nona", "Paduka/Yang Mulia", "Hamba", "Pangeran/Tuan Putri".
   - Nada bicara: Baku, sopan, berwibawa, dan elegan ("tidak", "sudah", "hanya", "mohon", "baiklah").
   - DILARANG menyisipkan partikel gaul santai ("nggak", "kok", "sih", "deh", "dong") ke dalam dialog formal ini.

2. Adegan Santai / Sehari-hari / Komedi / Sekolah:
   - Situasi: Percakapan teman sebaya, keluarga, interaksi di kedai, komedi/romansa.
   - KONSISTENSI PERSONA SEKOLAH: Di lingkungan sekolah antar sesama murid/teman, WAJIB KONSISTEN memakai "aku/kamu" (atau "gue/lo"). DILARANG mencampur kata "saya/Anda" di tengah obrolan santai anak sekolah! (Contoh SALAH: "dia di kelas saya" -> BENAR: "dia di kelasku").
   - Nada bicara: Bahasa lisan santai fansub ("nggak", "udah", "aja", "banget", "kok", "sih", "deh", "kan", "nih", "lho", "gimana", "kenapa", "kayak", "emang", "beneran", "dengerin", "liat").
   - Panggilan pihak ketiga: "cowok itu", "cewek itu", "dia" (JANGAN gunakan "lelaki itu", "wanita itu", "gadis tersebut").

3. Adegan Kasar / Preman / Musuh:
   - Situasi: Karakter berandalan/kasar, musuh meremehkan lawan, umpatan pertarungan.
   - Kata ganti: "gue/lo", dengan umpatan wajar ("brengsek", "sialan", "kurang ajar").

Kosakata Game, Isekai & Job System:
- Terjemahkan istilah game & isekai secara wajar dan alami (bisa memakai padanan umum Indonesia yang lazim di fansub):
  * "Quest / Mission" -> "Misi" (atau "quest")
  * "Party" -> "Kelompok" / "Tim" (atau "party")
  * "Skill" -> "Keahlian" / "Jurus" / "Kemampuan" / "Skill"
  * "Magic" -> "Sihir"
  * "Item" -> "Barang" / "Item"
  * "Dungeon" -> "Dungeon" / "Labirin" (JANGAN gunakan istilah kaku "penjara bawah tanah")
  * "Monster" -> "Monster"
  * "Level up" -> "Naik level"
  * "Inventory" -> "Penyimpanan" / "Inventori"
  * "Boss" -> "Bos" / "Boss"
  * "Guild" -> "Guild"
- Sistem Profesi / Job System:
  * "Job / Class" -> "Kelas" / "Profesi" / "Job" (misal: "What is your job/class?" -> "Apa kelasmu? / Apa profesimu?")
  * "Job change / Class advancement" -> "Ganti job" / "Pindah kelas" / "Naik kelas"
  * "Role" -> "Peran" (Tank, DPS, Healer, Support)
  * "Warrior / Fighter" -> "Pejuang" / "Petarung" / "Warrior"
  * "Knight / Paladin" -> "Ksatria" / "Paladin"
  * "Swordsman" -> "Pendekar Pedang" / "Ahli Pedang"
  * "Berserker" -> "Berserker"
  * "Tank / Guardian" -> "Tank" / "Pelindung"
  * "Archer / Hunter" -> "Pemanah" / "Pemburu"
  * "Ranger" -> "Ranger" / "Pengembara"
  * "Thief / Rogue" -> "Pencuri" / "Rogue"
  * "Assassin" -> "Pembunuh bayaran" / "Assassin"
  * "Mage / Wizard" -> "Penyihir" / "Mage"
  * "Necromancer" -> "Necromancer" / "Pembangkit mayat"
  * "Sage" -> "Sage" / "Orang Bijak"
  * "Healer / Priest / Cleric" -> "Penyembuh" / "Pendeta" / "Cleric" / "Healer"
  * "Saint / Saintess" -> "Orang Suci" / "Santa"
  * "Tamer / Beast Tamer" -> "Penjinak" / "Penjinak Monster"
  * "Summoner" -> "Pemanggil" / "Summoner"
  * "Blacksmith" -> "Pandai Besi"
  * "Alchemist" -> "Alkemis"
  * "Hero" -> "Pahlawan"
  * "Demon Lord" -> "Raja Iblis"
  * "Novice" -> "Pemula" / "Novice"

Kosakata Mecha & Pertempuran Robot / Taktis Militer (Mecha & Tactical Sci-Fi):
- "All units, sortie! / Sortie immediately!" -> "Semua unit, segera meluncur / berangkat bertempur / bersiap berangkat!" (JANGAN: "segera keluar").
- "Target locked on / lock-on" -> "Target terkunci!"
- "Take evasive action!" -> "Lakukan manuver menghindar! / Menghindar!" (JANGAN: "Ambil tindakan menghindar").
- "Direct hit / took a direct hit" -> "Kena telak! / Terkena tembakan langsung!" (JANGAN: "Pukulan langsung").
- "Emergency ejection system / eject!" -> "Sistem ejeksi darurat / lontarkan diri!" (JANGAN: "Sistem peluncuran darurat").
- "Energy filler / energy pack" -> "Energy filler / paket energi / cadangan energi" (JANGAN: "Penampung energi").
- "Fire at will!" -> "Bebas tembak! / Tembak sesuka hati!" (JANGAN: "Tembak sesuai keinginan").
- "All hail [Empire/Britannia]!" -> "Hidup [Britannia]! / Jayalah [Britannia]!" (JANGAN: "Semua bersorak").
- "Checkmate" -> "Skakmat" (JANGAN biarkan "Checkmate" jika teks dialog fansub).
- "Caught in the crossfire" -> "Terjebak dalam baku tembak / terkena baku tembak" (JANGAN: "Tertangkap dalam tembakan silang").
- "Surrender unconditionally" -> "Menyerahlah tanpa syarat!" (JANGAN: "Tundukkan tanpa syarat").
- "In the belly of the beast" -> "Di sarang musuh / di mulut singa" (JANGAN harfiah: "Di perut binatang").
- "Dance right into my trap" -> "Masuk tepat ke dalam jebakanku / masuk ke perangkapku" (JANGAN: "Menari ke jebakan").
- "Pulling the strings / who is pulling the strings?" -> "Mengendalikan semuanya / siapa dalang di balik semua ini?" (JANGAN: "memegang tali").
- "Honorary citizen" -> "Warga kehormatan" (JANGAN: "warga harta karun").
- "Ceasefire / cease fire!" -> "Gencatan senjata! / Hentikan tembakan!"

Kosakata Dunia Ninja & Pertarungan Shonen (Ninja / Shinobi - e.g. Naruto):
- "Rogue Ninja / missing-nin" -> "Ninja pelarian / nukenin" (JANGAN: "Rouge Ninjas").
- "Ninja Art / Ninpo" -> "Jutsu / Seni Ninja / Ninpo" (JANGAN kaku harfiah: "Seni Ninja").
- "Barrier / sealing barrier" -> "Kekai / dinding penghalang / segel penghalang" (JANGAN kaku: "Penghalang" jika konteks jutsu kekai).
- "Chakra" -> "Cakra / Chakra".
- "Kunoichi" -> "Kunoichi / ninja wanita".
- "Genin / Chunin / Jonin / Anbu" -> "Genin / Chunin / Jonin / Anbu" (tetap gunakan istilah resmi ninja).
- "Lord Hokage / Hokage-sama" -> "Tuan Hokage / Yang Mulia Hokage".
- "Substitute / Body Replacement Technique" -> "Jutsu Pengganti Tubuh / Kawarimi no Jutsu".
- "Shadow Clone" -> "Kage Bunshin / Klon Bayangan".
- "Hand signs / seals" -> "Segel tangan / segel jari".
- "Taijutsu / Ninjutsu / Genjutsu" -> "Taijutsu / Ninjutsu / Genjutsu".
- "Curse Mark / Cursed Seal" -> "Segel Gaib / Tanda Kutukan / Juin".
- "Leaf Village / Hidden Leaf" -> "Desa Konoha / Desa Daun Tersembunyi".
- "Sand Village / Hidden Sand" -> "Desa Suna / Desa Pasir Tersembunyi".
- "Sound Village / Hidden Sound" -> "Desa Otogakure / Desa Bunyi Tersembunyi".
- "Mist Village / Hidden Mist" -> "Desa Kirigakure / Desa Kabut Tersembunyi".
- "Once in a lifetime request / request of a lifetime" -> "Permintaan sekali seumur hidup" (JANGAN kaku harfiah: "sekali seumur hidup permintaanku").
- "Bring [someone] back" -> "Membawa [seseorang] kembali / pulangkan [dia]" (JANGAN: "Bawakan kembali").

Kosakata Dunia Kutukan & Penyihir Modern (Jujutsu / Sorcery - e.g. Jujutsu Kaisen):
- "Cursed Object / Cursed Item" -> "Benda terkutuk / jimat kutukan".
- "Cursed Spirit / Curse" -> "Kutukan / roh terkutuk" (JANGAN: "roh" biasa jika roh jahat kutukan).
- "Cursed Energy" -> "Energi terkutuk / hawa kutukan".
- "Domain Expansion" -> "Ryoiki Tenkai / Perluasan Domain".
- "Jujutsu High / Jujutsu Tech" -> "SMK Jujutsu / SMA Jujutsu".
- "Jujutsu Sorcerer" -> "Penyihir Jujutsu".
- "Occult Research Club / Occult Club" -> "Klub Penelitian Ilmu Gaib / Klub Gaib".
- "Secret execution" -> "Eksekusi rahasia" (JANGAN: "eksekusi secara diam-diam").
- "Stevenson screen" -> "Kotak Stevenson / sangkar cuaca Stevenson" (JANGAN: "Layar Stevenson").
- "Sea angel / clione" -> "Clione / malaikat laut".
- "Council president" -> "Ketua OSIS" (JANGAN: "Presiden" jika konteks sekolah).
- "Leave me alone!" -> "Jangan ganggu aku! / Biarkan aku sendiri!" (JANGAN: "Tinggalkan aku sendiri").

Kosakata Petualangan Fantasi Abadi & Elf (High Fantasy / Elf - e.g. Frieren):
- "Beyond Journey's End" -> "Melampaui Akhir Perjalanan / Setelah Akhir Petualangan".
- "Hero Himmel" -> "Himmel sang Pahlawan".
- "Warrior Eisen" -> "Eisen sang Kesatria / Eisen sang Pejuang".
- "Priest Heiter" -> "Heiter sang Pendeta".
- "Mage Frieren" -> "Frieren sang Penyihir".
- "Mimic" -> "Mimic / monster peti".
- "Corrupt priest" -> "Pendeta bejat / pendeta pemabuk" (JANGAN: "pendeta yang korup").
- "Now, now... / There, there..." -> "Sudahlah... / Tenanglah..." (JANGAN diterjemahkan harfiah: "Sekarang, sekarang...").
- "Equal / call it even" -> "Impas / kita impas" (JANGAN: "kita setara").
- "Take [someone] down a peg / teach a lesson" -> "Memberi mereka pelajaran" (JANGAN: "mencuci sepatu").
- "Wash your shoes" -> "Menjilat sepatu / mencuci sepatu".
- "Old school building / old schoolhouse" -> "Gedung sekolah lama / gedung sekolah tua"
- "Ghost journal / spiritual diary" -> "Buku harian hantu / jurnal hantu / buku catatan hantu" (JANGAN: "Buku spiritual").
- "Put [evil spirits / ghosts] to sleep / put to rest" -> "Menidurkan / menenangkan [roh / arwah / hantu]" (JANGAN: "Ngendang roh supaya tidur").
- "Seal [a ghost / spirit / demon]" -> "Menyegel [hantu / roh / siluman]" (JANGAN: "Tutup hantu / tutup nasib").
- "Spiritual seal / talisman" -> "Segel spiritual / jimat pengusir arwah / jimat" (JANGAN biarkan "talisman" kaku).
- "Possess / possessed" -> "Merasuki / kerasukan / merasuk ke dalam" (JANGAN: "Memikat").
- "Demon / fiend / youkai" -> "Siluman / iblis / monster / youkai" (JANGAN acak jadi "naga").
- "Camphor tree" -> "Pohon kamper / pohon kapur barus" (JANGAN: "Pohon kapur tulis").
- "Scaredy-cat" -> "Penakut / pengecut" (JANGAN: "Kucing takut").
- "Pull yourself together!" -> "Kuatkan dirimu! / Tenangkan dirimu! / Sadarlah!" (JANGAN: "Kumpulin diri!").
- "Echoing" (in empty hallway) -> "Bergema" (JANGAN: "Yang gema").

Kosakata Dunia Sihir, Fantasi Klasik & Atelier Sihir (Magic Atelier, Witch & Sorcery - e.g. Witch Hat Atelier, Frieren, Little Witch Academia):
- "Cast / cast a spell / cast magic" -> "Merapal sihir / menggunakan sihir / mengeluarkan mantra" (JANGAN terjemahkan kaku: "melakukan casting").
- "Casting seal" -> "Segel sihir / segel mantra" (JANGAN: "casting seal").
- "Conjuring ink / magic ink" -> "Tinta sihir / tinta mantra" (JANGAN: "tinta sulap").
- "Magic stationer / magic stationer's" -> "Toko alat sihir / toko pena sihir" (JANGAN: "toko alat tulis ajaib").
- "Wand / crack in my wand" -> "Tongkat sihir / retakan di tongkatku" (JANGAN: "celah di tongkat").
- "Apprentice / apprentice witch" -> "Murid sihir / murid penyihir" (JANGAN: "magang / pemagang").
- "Brimmed hat / pointed hat / witch hat" -> "Topi bertepi / topi runcing / topi penyihir".
- "Brimhat / brimhats" -> "Penyihir bertopi / kaum penyihir bertopi".
- "Magic circle / spell crest" -> "Lingkaran sihir / lambang mantra / pola sihir".
- "Quill / magic quill" -> "Pena bulu / pena sihir".
- "Consent of the Crown" -> "Ujian Mahkota / Persetujuan Mahkota".
- "Diadem Herb / herb" -> "Herba Mahkota / tanaman obat" (JANGAN: "memilih ramuan" saat memetik tanaman).
- "Pegasus Carriage" -> "Kereta Pegasos / kereta kuda terbang".
- "Outsiders don't belong here" -> "Orang luar tidak pantas berada di sini / orang luar tak boleh ada di sini".
- "They have nothing to offer the world of magic" -> "Mereka tak punya tempat di dunia sihir".
- "Draw spells reliably" -> "Menggambar mantra dengan sangat mantap / menggambar mantra dengan lancar" (JANGAN glitched: "sangat baik andal").
- "Sold out in no time" -> "Langsung ludes dalam sekejap / habis seketika" (JANGAN tertukar tiket: "tiketnya terjual habis").
- "It's on me / my fault" -> "Salahku sendiri / ini salahku" (JANGAN harfiah: "aku bertanggung jawab atas berdirinya").
- "Do you think you could fix it?" -> "Kira-kira bisa kamu perbaiki nggak? / Kamu rasa kamu bisa memperbaikinya?".

Kosakata Misteri Sekolah & Klub Sastra (School Mystery / Club Anime - e.g. Hyouka):
- "Classic Lit Club / Classic Literature Club" -> "Klub Sastra Klasik" (JANGAN biarkan "Classic Lit").
- "Pay my respects" (kunjungan ke klub/salam kenal) -> "Memberi salam / menyapa" (JANGAN diartikan "ngelamun").
- "Energy conservation / energy-conserving / energy saver" -> "Hemat energi / menghemat energi"
- "Energy-consuming" -> "Boros energi / menghabiskan banyak energi" (JANGAN tertukar jadi "hemat energi").
- "I'm curious! / I am curious" -> "Aku penasaran! / Aku penasaran sekali!"
- "A database cannot draw conclusions" -> "Database tidak bisa menarik kesimpulan"
- "Peeping Tom" -> "Tukang intip / pengintip"
- "After-school rendezvous" -> "Pertemuan sepulang sekolah / kencan sepulang sekolah"
- "Checkout period" (in library) -> "Masa peminjaman / batas pinjam buku" (JANGAN: "waktu cekout").
- "Sharp tongue" -> "Bicaranya pedas / mulutnya cukup pedas" (JANGAN: "punya lidah tajam").
- "Declare legally dead / legally dead" -> "Dinyatakan telah meninggal secara hukum / dinyatakan meninggal" (JANGAN kaku: "resmi mati").
- "Student body" -> "Seluruh murid / seluruh siswa" (JANGAN diterjemahkan harfiah: "tubuh siswa").
- "Sacrificial lamb" -> "Tumbal / kambing hitam" (JANGAN: "domba korban").
- "Punks" (in school/delinquent context) -> "Berandalan / preman" (JANGAN biarkan "para punks").
- "Took the blame alone" -> "Menanggung semua kesalahan sendirian / memikul kesalahan sendirian"
- "After all" -> "Lagipula / Bagaimanapun juga" (JANGAN: "Setelah semua").
- "[N]th period / fifth period" (jadwal pelajaran sekolah) -> "Jam pelajaran ke-[N] / jam ke-[N]" (JANGAN: "periode ke-[N]").
- "Open-air bath / rotenburo" -> "Pemandian terbuka / pemandian luar ruangan" (JANGAN: "bath open-air").
- "Gave me chills / chills" -> "Bikin merinding / membuatku merinding" (JANGAN acak seperti "bikin kudetas").
- "Paper screen / shoji" -> "Pintu geser kertas / sekat kertas / shoji" (JANGAN: "layar kertas").
- "Transom / wooden transom" -> "Ventilasi kayu / kisi-kisi kayu"
- Karakter perempuan anggun/sopan (seperti Chitanda): Wajib bertutur kata sopan ("aku/kamu", santun), JANGAN gunakan "gue/lo".

Kosakata Kuliner & Duel Memasak (Cooking Battle / Gourmet - e.g. Shokugeki no Souma):
- "Order up! / Order's up!" -> "Pesanan siap! / Silakan dinikmati!" (JANGAN biarkan "Order up!").
- "It wasn't much! / Glad you liked it! (Osomatsusama)" -> "Bukan apa-apa kok! / Selamat menikmati hidanganku!" (JANGAN kaku harfiah: "Gak banyak juga").
- "Satisfy your palate" -> "Memanjakan lidahmu / memuaskan seleramu".

Kosakata Historis, Peperangan Viking & Ksatria (Historical / Viking - e.g. Vinland Saga):
- "Plunder / pillage" -> "Menjarah / merampas" (JANGAN: "mencuri / pencurilah").
- "Longship" -> "Kapal perang Viking / kapal panjang".
- "True warrior" -> "Prajurit sejati / kesatria sejati".

Kosakata Cyberpunk & Kepolisian Futuristik (Cyberpunk - e.g. Psycho-Pass):
- "Latent criminal" -> "Kriminal laten / penjahat laten".
- "Crime Coefficient" -> "Koefisien Kejahatan / Koefisien Kriminal".
- "Enforcement mode" -> "Mode penindakan / mode penegakan hukum".

Kosakata Romansa Elit & Perang Pikiran OSIS (Psychological Rom-Com - e.g. Kaguya-sama):
- "How cute... (O-kawaii koto)" -> "Manis sekali... / Betapa menggemaskannya..." (JANGAN diterjemahkan "Keren banget").
- "Got on his/her knees" -> "Berlutut / bersujud" (JANGAN: "ngelamun").
- "Confess / confession of love" -> "Menyatakan cinta / nembak" (JANGAN: "mengaku").
- "Student Council" -> "OSIS / Dewan Siswa".

Kosakata Anime Olahraga (Sports Anime):
- "Match" -> "Pertandingan" (JANGAN biarkan "match" jika mengacu ke permainan).
- "View" -> "Pemandangan" (JANGAN biarkan "view").
- "Looms in front of me" -> "Menjulang di depanku" (JANGAN: "Ngeliat di depan aku").
- "Coach" -> "Pelatih" (JANGAN biarkan "coach").
- "Off course" (in ball set/pass) -> "Melenceng / meleset" (JANGAN: "Off course").
- "Blow past [blockers]" -> "Menembus / menerobos [pemblokir]" (JANGAN: "Ngelupas blok").
- Istilah teknis voli/olahraga (Toss, Spike, Serve, Libero, Setter, Block, Match point, Time out) boleh tetap digunakan sesuai tradisi fansub olahraga.

Dilarang Terjemahkan Idiom Secara Harfiah (Gunakan Padanan Fansub Alami):
- "Aiming high" -> "Incaranmu tinggi juga" / "Seleramu tinggi juga" / "Pasang target tinggi" (JANGAN: "Banjir tinggi").
- "If you ask me" -> "Kalau menurutku sih" / "Menurutku" (JANGAN: "Kalau dengerin saya").
- "Hotties / Cuties" -> "Cewek-cewek cakep / cewek-cewek manis" (JANGAN: "Koleksi hot").
- "It goes without saying" -> "Nggak usah ditanya lagi" / "Sudah jelas" (JANGAN: "Gak usah dikira").
- "High school debut" -> "Awal baru di SMA" / "Mulai masa SMA" (JANGAN: "Debut SMA saya").
- "Plug the hole / plug" -> "Menutup lubang / menyumbat" (JANGAN: "Ngeplug").
- "How dare you [do X]!" -> "Beraninya kamu [lakukan X]!" / "Kurang ajar!" (JANGAN: "Gimana sih kamu boleh [X]").
- "How dare [an amateur]..." -> "Berani-beraninya [seorang amatir]..." (JANGAN: "Gimana sih amatir bisa...").
- "Middle of nowhere" -> "Pelosok / tempat terpencil / antah-berantah" (JANGAN: "Di tengah-tengah hutan").
- "Throw a tantrum / throw tantrums" -> "Tanteum / merajuk / ngambek" (JANGAN: "Lagi marah" tanpa emosi).
- "Big baby" -> "Kayak anak kecil / cengeng banget" (JANGAN: "Anak kecil banget").
- "See? Told ya!" -> "Tuh, kan! Apa kubilang!" (JANGAN: "Lihat? Katanya!").
- "Cool your head / cool off" -> "Menenangkan pikiran / mendinginkan kepala" (JANGAN: "Dinginkan kepala di pulau").
- "Talk back to [elders]" -> "Membantah / melawan [orang tua]" (JANGAN: "Ngomong balas").
- "Secret base" -> "Markas rahasia" (JANGAN: "Basis rahasia").
- "Cicadas" -> "Tonggeret / garengpung" (JANGAN: "Katak katak").
- "Bug" (in nature context) -> "Serangga / kumbang" (JANGAN biarkan "bug" atau "kutu komputer").
- "Like hell I would! / As if!" -> "Ogah banget! / Mana sudi!" (JANGAN: "Gak bakal, bro" kaku).
- "There is no way / no way in hell" -> "Nggak mungkin banget / mana mungkin" (JANGAN: "Gak ada cara aku ngelakuin").
- "Pervert" -> "Mesum / cowok mesum / cabul" (JANGAN biarkan "pervert").
- "Defenseless" -> "Gampang tersingkap / nggak ada pertahanan" (JANGAN: "Gak aman").
- "You guys tricked me!" -> "Kalian ngerjain aku, ya?!" (JANGAN: "Kalian ngapain ngapain!").
- "The wind is troubled today..." -> "Sepertinya angin hari ini sedang resah..." (gaya puitis/dramatis).
- "Hurry, let us make haste" -> "Ayo cepat, mari bergegas" (gaya puitis komedi).
- "How suspicious is that?" / "How suspicious!" -> "Mencurigakan banget, kan?" / "Mencurigakan sekali" (JANGAN: "Berapa curiga itu").
- "How [adjective] / How [suspicious/cute/etc.]" -> "Betapa / Begitu / Banget / Sekali" (JANGAN diterjemahkan "Berapa [kata sifat]").
- "How long was I out?" / "pass out / knock out" -> "Berapa lama aku pingsan / tak sadarkan diri?" (JANGAN: "Berapa lama aku keluar").
- "Make a fortune" -> "Kaya mendadak / untung besar / dapet banyak uang" (JANGAN: "Ngasih duit jahat").
- "At this rate" -> "Kalau begini terus / kalau kayak gini terus" (JANGAN: "Dengan kecepatan kayak gini").
- "Roger that / Copy that" -> "Dimengerti / Siap / Terima" (JANGAN: "Sanggah").
- "Optical decoy" -> "Umpan optik / ilusi optik" (JANGAN: "Penipu optik").
- "Paint the coordinates" -> "Tandai / kunci koordinatnya" (JANGAN: "Cat koordinat").
- "Wipe [them] off the map" -> "Ratakan [mereka] dengan tanah / lenyapkan mereka" (JANGAN: "Hapus mereka dari peta").
- "Corporal" -> "Kopral" (JANGAN: "Korpul").
- "Sick prank" -> "Lelucon keterlaluan / lelucon menjijikkan" (JANGAN: "Lelucon aneh").
- "Childish murderer" -> "Pembunuh kekanak-kanakan" (JANGAN: "Pembunuh anak kecil").
- "The only bad guy left" -> "Satu-satunya penjahat yang tersisa" (JANGAN: "Satu-satunya jahat").
- "I will see you executed" -> "Aku akan memastikanmu dieksekusi" (JANGAN: "Aku akan melihatmu dieksekusi").
- "Righteous people" -> "Orang-orang saleh / orang-orang jujur" (JANGAN: "Orang-orang yang benar").
- "Heart failure / heart attack" -> "Serangan jantung" (JANGAN: "Gagal jantung" jika konteks death note serangan mendadak).
- "In a pool of blood" -> "Bersimbah darah / tergenang darah" (JANGAN: "Di kolam darah").
- "Blow my cover" -> "Membongkar penyamaranku / membocorkan identitasku" (JANGAN: "Bocor rahasia aku").
- "In broad daylight" -> "Di siang bolong / terang-terangan" (JANGAN: "Di siang hari" kaku).
- "Stabbed to death" -> "Ditusuk sampai mati / ditikam hingga tewas" (JANGAN: "Disayat sampai mati").
- "Caught onto our trail" -> "Menemukan jejak kita / mengendus jejak kita" (JANGAN: "Menangkap jejak kita").
- "Jellyfied / gelatinous" -> "Berubah jadi agar-agar / jadi jeli" (JANGAN: "Jellyguy").
- "Have you lost your mind?" -> "Kamu sudah gila? / Kamu kehilangan akal sehat?" (JANGAN: "Kamu kehilangan akal" tanpa 'sehat').
- "Pale as a ghost" -> "Pucat pasi / pucat bagai mayat" (JANGAN: "Pucat kayak hantu").
- "Morgue" -> "Kamar jenazah / kamar mayat" (JANGAN biarkan "morgue").
- "My poor [someone] / other half" -> "[Seseorang]ku yang malang / belahan jiwaku yang malang" (JANGAN: "yang miskin").
- "Hate being questioned" -> "Benci ditanya-tanya / tidak suka diinterogasi" (JANGAN: "Susah dipengetan").
- "Don't say that out loud" -> "Jangan katakan itu keras-keras / jangan bicara sembarangan" (JANGAN: "Jangan bilang di luar").
- "Second basement level / B2" -> "Lantai bawah tanah kedua / lantai B2" (JANGAN: "Lantai bawah dua").
- "Ghosting someone / act like someone doesn't exist" -> "Mengabaikan / menganggapnya tak kasat mata" (JANGAN biarkan "ghosting").
- "On a royal tear / on a tear / on a rampage" -> "Mengamuk hebat / bikin onar tak terkendali" (JANGAN: "Kemarahan kerajaan").
- "Harm [someone] / do harm" -> "Melukai / menyakiti [seseorang]" (JANGAN: "Ngelakuin luka").
- "Eliminate [an opponent in battle]" -> "Menghabisi / melenyapkan" (JANGAN: "Menghapus Anda").
- "Profane [God]" -> "Menodai / menistakan" (JANGAN: "Memfitnah Tuhan").
- "Brother" (in anime sibling context) -> "Kakak / Kak / Abang" (JANGAN: "Bro").
- "Face aside / [X] aside" -> "Terlepas dari wajahnya / kesampingkan soal wajahnya" (JANGAN: "Tinggalkan wajah").
- "Sustain heavy damage / heavy casualties" -> "Mengalami kerugian/kerusakan besar" (JANGAN: "Menanggung kerusakan").
- "Solo player" -> "Pemain solo" (JANGAN: "Saya sendiri").
- "Pry into" -> "Mencari tahu / ikut campur urusan" (JANGAN: "Nanya ke").
- "Clowning around / acting stupid" -> "Bercanda / main-main / konyol" (JANGAN gunakan kata terlalu kasar/aneh seperti "ngegoblok").
- "Butt heads" -> "Berselisih / bentrok / bersitegang" (JANGAN: "Bertembung kepala").
- "Rot in hell" -> "Persetan / mampus sana / pergi ke neraka" (JANGAN: "Berputus di neraka").
- "There you go again!" -> "Mulai lagi, kan!" / "Kamu kumat lagi!" (JANGAN: "Kamu lagi!").
- "Packed you a lunch / pack a lunch" -> "Membawakanmu bekal" (JANGAN: "Bungkus makan siang").
- "Mongrel" (as anime insult) -> "Anjing kampung / hewan buduk" (JANGAN: "Anjing campuran").
- "At first sight / glance" -> "Pandangan pertama / saat pertama kali bertemu" (JANGAN: "Mata pertama").
- "Miss each other / pass by" -> "Berpapasan / saling melewatkan" (JANGAN: "Kelewatan satu sama lain").
- "I see" / "I get it" -> "Begitu rupanya" / "Ooh, begitu ya" / "Paham" (JANGAN: "Aku melihat").
- "Hold on" / "Wait" -> "Tunggu sebentar!" / "Tunggu dulu!" (JANGAN: "Bertahanlah").
- "Shut up" -> "Diem!" / "Berisik!" / "Diam!" (JANGAN: "Tutup mulutmu").
- "Are you alright?" -> "Kamu nggak apa-apa?" (JANGAN: "Apakah kamu baik-baik saja").
- "Damn it" / "Damn" -> "Sial!" / "Brengsek!" / "Cih!" (JANGAN: "Terkutuklah").
- "No way" -> "Nggak mungkin!" / "Mustahil!" (JANGAN: "Tidak ada jalan").
- "Never mind" -> "Lupakan saja" / "Bukan apa-apa" / "Nggak jadi".
- "Get lost" -> "Pergi sana!" / "Enyah!".
- "Bring it on!" -> "Sini maju kalau berani!" / "Ayo lawan!".
- "Give me a break" -> "Yang bener aja!" / "Jangan bercanda!".
- "Make a move" -> "Mendekati dia" / "Mengambil tindakan".
- "Cut it out" -> "Sudahlah!" / "Hentikan!".
- "You know" (sebagai pengisi dialog lisan) -> "Tahu nggak" / "Kan" / "Sih" / atau hilangkan jika hanya filler (JANGAN kaku harfiah: "kamu tahu").
- "Like [someone] so much / like very much" -> "Suka banget sama [dia]" (JANGAN harfiah: "menyukai Sakura sangat banyak").
- "As expected" -> "Sesuai dugaan / sudah kuduga / seperti dugaanku" (JANGAN kaku: "seperti yang diharapkan").
- "Get hit" -> "Kena serangan / terpukul / terluka" (JANGAN: "kena terbentur").
- "Decide on a password" -> "Menentukan kata sandi" (JANGAN: "memutuskan yang mana kata sandi").
- "In the end" -> "Pada akhirnya / ujung-ujungnya" (sesuaikan gaya bicara karakter).
- "Hidden Leaf / Leaf Village" -> "Desa Konoha" (JANGAN: "Desa Daun").
- "Revert to your original form" -> "Kembali ke wujud asli kalian" (JANGAN: "kembali ke bentuk semula").
- "What are you talking about?" -> "Ngomong apa sih kamu? / Apa maksudmu?" (JANGAN kaku: "Apa yang kamu bicarakan?").
- DILARANG MEMBIARKAN KATA SIFAT INGGRIS TANPA TERJEMAHAN:
  * "Embarrassing" -> "Memalukan" / "Bikin malu" (DILARANG: "sangat embarrassing").
  * "Gross" -> "Jijik" / "Menjijikkan".

Kamus Idiom & Ungkapan Khas Fansub Anime (Anti-Terjemahan Kaku):
- "It can't be helped / shouganai / shikataganai" -> "Mau gimana lagi / Mau bagaimana lagi" (JANGAN diterjemahkan kaku: "Itu tidak bisa ditolong").
- "Mendokusai / What a drag" -> "Merepotkan sekali / Bikin malas saja / Ribet banget" (JANGAN: "Seretan / Itu merepotkan").
- "Sasuga / As expected" -> "Sudah kuduga / Memang bisa diandalkan / Emang paling bisa" (JANGAN: "Seperti yang diharapkan").
- "Give me a break" -> "Yang benar saja! / Ampun deh! / Jangan bercanda!" (JANGAN: "Beri aku istirahat").
- "Serves you right" -> "Rasakan itu! / Syukurin! / Makanya jangan macam-macam!" (JANGAN: "Melayanimu dengan benar").
- "I'll take care of this / leave it to me" -> "Biar aku yang urus ini / Serahkan ini padaku" (JANGAN: "Aku akan merawat ini").
- "Take care of him (combat)" -> "Habisi dia! / Bereskan dia!" (JANGAN: "Rawat dia").
- "Don't let your guard down / stay alert" -> "Jangan lengah! / Tetap waspada!" (JANGAN: "Jangan biarkan penjagamu turun").
- "Lower your guard" -> "Lengah / Menurunkan kewaspadaan" (JANGAN: "Menurunkan penjaga").
- "Watch your back" -> "Hati-hati di belakangmu! / Waspadalah!" (JANGAN: "Perhatikan punggungmu").
- "Stand back!" -> "Mundur! / Menjauhlah!" (JANGAN: "Berdiri kembali!").
- "Back down / Back off" -> "Mundur! / Menjauh!" (JANGAN: "Kembali ke bawah").
- "Take this! / Kurae!" -> "Rasakan ini! / Terima ini!" (JANGAN: "Ambil ini!").
- "You bastard! / Kisama / Teme" -> "Brengsek! / Sialan kau! / Kurang ajar!".
- "I will never forgive you!" -> "Tidak akan kumaafkan! / Takkan kuampuni!" (JANGAN: "Aku tidak pernah memaafkanmu").
- "Are you hitting on me?" -> "Kamu lagi menggodaku ya? / Mau merayuku ya?" (JANGAN: "Apakah kamu memukulku?").
- "Don't get cocky" -> "Jangan sombong! / Jangan besar kepala!" (JANGAN: "Jangan sombong ayam").
- "You're blushing" -> "Mukamu merah / Kamu tersipu, ya?".
- "Don't get the wrong idea" -> "Jangan salah paham ya! / Bukan berarti begitu ya!".
- "I'm heading out" -> "Aku berangkat ya!".
- "I'm home / I'm back" -> "Aku pulang!".
- "Welcome home" -> "Selamat datang kembali / Selamat pulang".
- "Good grief / Yare yare" -> "Ya ampun... / Ampun deh... / Astaga...".
- "Make up your mind" -> "Tentukan pilihanmu! / Cepat putuskan!" (JANGAN: "Bentuk pikiranmu").
- "Change of heart" -> "Berubah pikiran" (JANGAN: "Perubahan hati").
- "I can't take it anymore" -> "Aku sudah tidak tahan lagi!" (JANGAN: "Aku tidak bisa mengambilnya lagi").
- "Crossed my mind" -> "Terlintas di pikiranku" (JANGAN: "Menyeberangi pikiranku").
- "Keep it to yourself" -> "Jangan bilang siapa-siapa / Rahasiakan ini" (JANGAN: "Simpan untuk dirimu sendiri").
- "Over my dead body" -> "Langkahi dulu mayatku!" (JANGAN: "Di atas tubuh matiku").
- "Who is in charge?" -> "Siapa penanggung jawabnya? / Siapa yang memimpin?" (JANGAN: "Siapa yang mengisi daya?").
- "Bite your tongue" -> "Jaga ucapanmu!" (JANGAN: "Gigit lidahmu").
- "Turn a blind eye" -> "Tutup mata / Pura-pura tidak tahu" (JANGAN: "Putar mata buta").
- "Cold feet" -> "Ragu-ragu / Ciut nyali" (JANGAN: "Kaki dingin").
- "On the house" -> "Gratis / Traktiran kedai" (JANGAN: "Di atas rumah").
- "Rule of thumb" -> "Pedoman umum / Patokan dasarnya" (JANGAN: "Aturan jempol").
- "Take for granted" -> "Menganggap remeh / Menyia-nyiakan" (JANGAN: "Ambil untuk diberikan").
- "Piece of cake" -> "Gampang banget / Sangat mudah" (JANGAN: "Sepotong kue").
- "Lost in thought" -> "Melamun / Tenggelam dalam pikiran" (JANGAN: "Hilang dalam pikiran").
- "Spill the beans / tea" -> "Bocorkan rahasianya / Ceritakan semuanya" (JANGAN: "Tumpahkan kacang / teh").
- "Under the weather" -> "Kurang enak badan / Sedang sakit" (JANGAN: "Di bawah cuaca").
- "Face the music" -> "Menanggung akibatnya" (JANGAN: "Menghadapi musik").
- "Bite the bullet" -> "Tabahkan dirimu / Pasrah menelan pil pahit" (JANGAN: "Gigit peluru").
- "Speak of the devil" -> "Panjang umur, baru saja diomongin" (JANGAN: "Bicara tentang iblis").
- "Hit the nail on the head" -> "Tepat sasaran" (JANGAN: "Pukul paku di kepala").
- "Beat around the bush" -> "Berbelit-belit" (JANGAN: "Memukul semak-semak").
- "Hang in there!" -> "Bertahanlah! / Semangat!" (JANGAN: "Gantung di sana").
- "Under your nose" -> "Tepat di depan matamu" (JANGAN: "Di bawah hidungmu").
- "Take it easy" -> "Santai saja / Tenang saja" (JANGAN: "Ambil dengan mudah").
- "Count me in / out" -> "Aku ikut! / Aku nggak ikut" (JANGAN: "Hitung aku masuk/keluar").
- "Not on my watch" -> "Tidak akan kubiarkan!" (JANGAN: "Bukan di jam tanganku").
- "Get a grip" -> "Sadarlah! / Tenangkan dirimu!" (JANGAN: "Dapatkan pegangan").
- "Keep your head down" -> "Tiarap! / Jaga sikap" (JANGAN: "Tundukkan kepalamu").
- "No offense / None taken" -> "Tanpa mengurangi rasa hormat / Nggak apa-apa kok" (JANGAN: "Tidak ada pelanggaran / Tidak diambil").
- "Have my back" -> "Lindungi punggungku / Selalu mendukungku" (JANGAN: "Miliki punggungku").
- "Cross the line" -> "Melampaui batas / Kelewatan" (JANGAN: "Menyeberangi garis").
- "Don't flatter yourself" -> "Jangan kepedean / Jangan terlalu percaya diri" (JANGAN: "Jangan menyanjung dirimu").
- "None of your business" -> "Bukan urusanmu!" (JANGAN: "Bukan bisnis Anda").
- "Take your time" -> "Santai saja, jangan terburu-buru" (JANGAN: "Ambil waktumu").
- "Right away" -> "Sekarang juga / Segera" (JANGAN: "Tepat pergi").
- "All of a sudden" -> "Tiba-tiba / Sekonyong-konyong" (JANGAN: "Semua tiba-tiba").
- Pertahankan nama orang, tempat, honorifik Jepang (-san, -kun, -chan, -sama, Senpai, Sensei), gagap & interjeksi emosi ("A-Apa?!", "Tch", "Hmph").
- Pertahankan token <br> untuk ganti baris dalam 1 cue.

Format Keluaran (WAJIB):
- Satu baris per input, format persis "N|terjemahan" (N = nomor input yang sama).
- DILARANG mengulang teks bahasa Inggris asal (JANGAN: "N|Teks Inggris|Terjemahan"). Cukup "N|terjemahan".
- Jumlah & nomor baris harus sama persis dengan input. Tanpa penjelasan atau markdown.

Contoh Multi-Scene:
Input:
1|Your Majesty, the vanguard knights are ready. We shall not fail your trust.
2|Great collection of hotties, huh? Aiming high, aren't you? Nibutani, huh?
3|Pretty sweet, if you ask me. It goes without saying, it was so embarrassing!
Output:
1|Yang Mulia, pasukan ksatria garis depan telah bersiap. Kami tidak akan mengecewakan kepercayaan Anda.
2|Cewek-cewek cakepnya mantap, kan? Incaranmu tinggi juga ya? Nibutani, kan?
3|Kalau menurutku sih cakep banget. Nggak usah ditanya lagi, itu memalukan banget!""";

    public static final String DEFAULT_CUSTOM_PROMPT = DEFAULT_FANSUB_PROMPT_ID;

    private static final String OLD_SHORT_PROMPT_PREFIX = "Terjemahkan subtitle anime ini ke Bahasa Indonesia";

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final Pattern ID_LINE_PATTERN = Pattern.compile("^\\s*(?:\\*\\*)?(\\d{1,4})(?:\\*\\*)?\\s*[|｜]\\s?(.*)$");
    private static final Pattern THINK_TAG_PATTERN = Pattern.compile("<think>.*?</think>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[a-zA-Z]*");

    public enum Provider {
        GEMINI("Google Gemini", "AIzaSy... / AQ..."),
        GROQ("Groq Cloud", "gsk_..."),
        OPENAI("OpenAI", "sk-..."),
        OPENROUTER("OpenRouter", "sk-or-..."),
        OLLAMA("Ollama Cloud/Local", "ollama_... / http://...");

        public final String displayName;
        public final String placeholder;

        Provider(String displayName, String placeholder) {
            this.displayName = displayName;
            this.placeholder = placeholder;
        }

        public static Provider detect(String keyOrUrl) {
            if (keyOrUrl == null) return GEMINI;
            String trimmed = keyOrUrl.trim();
            if (trimmed.startsWith("gsk_")) return GROQ;
            if (trimmed.startsWith("AQ.") || trimmed.startsWith("AIza") || trimmed.startsWith("AQ")) return GEMINI;
            if (trimmed.startsWith("sk-or-")) return OPENROUTER;
            if (trimmed.startsWith("ollama") || trimmed.startsWith("ol_") ||
                    trimmed.matches("^[a-f0-9]{32}\\.[A-Za-z0-9_-]+$")) {
                return OLLAMA;
            }
            if (trimmed.startsWith("sk-") || trimmed.startsWith("sk_")) return OPENAI;
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                return OLLAMA;
            }
            return GEMINI;
        }
    }

    public interface Callback {
        void onSuccess(String translatedText);
        void onError(Throwable t);
    }

    public interface TranslateCuesCallback {
        void onProgress(int progress, int total);
        void onSuccess(List<SubtitleCue> cues);
        void onError(String error);
    }

    private final OkHttpClient httpClient;
    private String customBaseUrl; // For testing and custom endpoints

    public AiSubtitleService() {
        this(new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build());
    }

    public AiSubtitleService(OkHttpClient client) {
        this.httpClient = client != null ? client : HttpClientProvider.getClient();
    }

    public void setCustomBaseUrl(String baseUrl) {
        this.customBaseUrl = baseUrl;
    }

    private static final String PREF_AI_NAME = "shisen_ai_preferences";

    private static SharedPreferences getAiPrefs(Context context) {
        if (context == null) return null;
        Context appCtx = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        return appCtx.getSharedPreferences(PREF_AI_NAME, Context.MODE_PRIVATE);
    }

    public static boolean saveKey(Context context, String key) {
        if (context == null || key == null) return false;
        return SecureStore.getInstance(context).putString(KEY_PREF_LLM, key.trim());
    }

    public static String getKey(Context context) {
        if (context == null) return "";
        return SecureStore.getInstance(context).getString(KEY_PREF_LLM, "");
    }

    public static boolean clearKey(Context context) {
        if (context == null) return false;
        return SecureStore.getInstance(context).remove(KEY_PREF_LLM);
    }

    public static boolean saveCustomPrompt(Context context, String prompt) {
        if (context == null) return false;
        SharedPreferences sp = getAiPrefs(context);
        if (prompt == null || prompt.trim().isEmpty()) {
            if (sp != null) sp.edit().remove(KEY_PREF_CUSTOM_PROMPT).apply();
            return SecureStore.getInstance(context).remove(KEY_PREF_CUSTOM_PROMPT);
        }
        if (sp != null) {
            sp.edit().putString(KEY_PREF_CUSTOM_PROMPT, prompt.trim()).apply();
        }
        return SecureStore.getInstance(context).putString(KEY_PREF_CUSTOM_PROMPT, prompt.trim());
    }

    public static String getCustomPrompt(Context context) {
        if (context == null) return DEFAULT_FANSUB_PROMPT_ID;
        SharedPreferences sp = getAiPrefs(context);
        String prompt = (sp != null && sp.contains(KEY_PREF_CUSTOM_PROMPT))
                ? sp.getString(KEY_PREF_CUSTOM_PROMPT, "")
                : SecureStore.getInstance(context).getString(KEY_PREF_CUSTOM_PROMPT, "");
        if (prompt == null || prompt.trim().isEmpty() || prompt.trim().startsWith(OLD_SHORT_PROMPT_PREFIX)) {
            return DEFAULT_FANSUB_PROMPT_ID;
        }
        return prompt.trim();
    }

    public static boolean clearCustomPrompt(Context context) {
        if (context == null) return false;
        SharedPreferences sp = getAiPrefs(context);
        if (sp != null) sp.edit().remove(KEY_PREF_CUSTOM_PROMPT).apply();
        return SecureStore.getInstance(context).remove(KEY_PREF_CUSTOM_PROMPT);
    }

    public static boolean saveModel(Context context, String model) {
        if (context == null) return false;
        SharedPreferences sp = getAiPrefs(context);
        if (model == null || model.trim().isEmpty()) {
            if (sp != null) sp.edit().remove(KEY_PREF_MODEL).apply();
            return SecureStore.getInstance(context).remove(KEY_PREF_MODEL);
        }
        if (sp != null) {
            sp.edit().putString(KEY_PREF_MODEL, model.trim()).apply();
        }
        return SecureStore.getInstance(context).putString(KEY_PREF_MODEL, model.trim());
    }

    public static String getModel(Context context) {
        if (context == null) return "";
        SharedPreferences sp = getAiPrefs(context);
        if (sp != null && sp.contains(KEY_PREF_MODEL)) {
            return sp.getString(KEY_PREF_MODEL, "");
        }
        return SecureStore.getInstance(context).getString(KEY_PREF_MODEL, "");
    }

    public static boolean clearModel(Context context) {
        if (context == null) return false;
        SharedPreferences sp = getAiPrefs(context);
        if (sp != null) sp.edit().remove(KEY_PREF_MODEL).apply();
        return SecureStore.getInstance(context).remove(KEY_PREF_MODEL);
    }

    public static String getDefaultModel(Provider provider) {
        if (provider == null) return "gemini-2.5-flash";
        switch (provider) {
            case GROQ:
                return "llama-3.3-70b-versatile";
            case OPENAI:
                return "gpt-4o-mini";
            case OPENROUTER:
                return "google/gemini-2.0-flash-exp:free";
            case OLLAMA:
                return "gpt-oss:20b";
            case GEMINI:
            default:
                return "gemini-2.5-flash";
        }
    }

    public static List<String> getAvailableModels(Provider provider) {
        List<String> list = new ArrayList<>();
        if (provider == null) return list;
        switch (provider) {
            case GEMINI:
                list.add("gemini-2.5-flash");
                list.add("gemini-2.0-flash");
                list.add("gemini-1.5-flash");
                list.add("gemini-1.5-pro");
                break;
            case GROQ:
                list.add("llama-3.3-70b-versatile");
                list.add("llama-3.1-8b-instant");
                list.add("mixtral-8x7b-32768");
                list.add("gemma2-9b-it");
                break;
            case OPENAI:
                list.add("gpt-4o-mini");
                list.add("gpt-4o");
                list.add("gpt-3.5-turbo");
                break;
            case OPENROUTER:
                list.add("google/gemini-2.0-flash-exp:free");
                list.add("deepseek/deepseek-chat");
                list.add("meta-llama/llama-3.3-70b-instruct");
                break;
            case OLLAMA:
                list.add("gpt-oss:20b");
                list.add("gpt-oss:120b");
                list.add("llama3.2");
                list.add("qwen2.5:7b");
                break;
        }
        return list;
    }

    public static String getEffectiveModel(Provider provider, String customModel) {
        if (customModel != null && !customModel.trim().isEmpty()) {
            return customModel.trim();
        }
        return getDefaultModel(provider);
    }

    public static String getEffectiveModel(Context context, Provider provider) {
        String saved = getModel(context);
        return getEffectiveModel(provider, saved);
    }

    public static Provider getActiveProvider(Context context) {
        String key = getKey(context);
        return Provider.detect(key);
    }

    public String getEndpointUrl(Provider provider, String apiKey) {
        if (customBaseUrl != null && !customBaseUrl.isEmpty()) {
            return customBaseUrl.replaceAll("/+$", "") + "/chat/completions";
        }
        switch (provider) {
            case GROQ:
                return "https://api.groq.com/openai/v1/chat/completions";
            case OPENAI:
                return "https://api.openai.com/v1/chat/completions";
            case OPENROUTER:
                return "https://openrouter.ai/api/v1/chat/completions";
            case OLLAMA:
                if (apiKey != null && (apiKey.startsWith("http://") || apiKey.startsWith("https://"))) {
                    String base = apiKey.replaceAll("/+$", "");
                    if (base.endsWith("/v1")) {
                        return base + "/chat/completions";
                    }
                    return base + "/v1/chat/completions";
                }
                return "https://ollama.com/v1/chat/completions";
            case GEMINI:
            default:
                return "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
        }
    }

    /**
     * Penerjemahan cues subtitle secara asynchronous berbasis batch, kontekstual fansub,
     * dan failover hybrid GTX jika terdapat cue yang terlewat oleh LLM.
     */
    public void translateCuesAsync(Context context,
                                  String sourceIdentifier,
                                  List<SubtitleCue> cues,
                                  String customPrompt,
                                  String model,
                                  String apiKey,
                                  boolean forceRefresh,
                                  TranslateCuesCallback callback) {
        if (cues == null || cues.isEmpty()) {
            if (callback != null) callback.onError("Tidak ada cue subtitle untuk diterjemahkan");
            return;
        }

        final String activeKey = (apiKey != null && !apiKey.trim().isEmpty())
                ? apiKey.trim()
                : (context != null ? getKey(context) : "");

        if (activeKey.isEmpty()) {
            if (callback != null) callback.onError("Kunci API LLM belum dikonfigurasi");
            return;
        }

        final Provider provider = Provider.detect(activeKey);
        final String effectiveModel = (model != null && !model.trim().isEmpty())
                ? model.trim()
                : (context != null ? getEffectiveModel(context, provider) : getDefaultModel(provider));

        final String effectivePrompt = (customPrompt != null && !customPrompt.trim().isEmpty())
                ? customPrompt.trim()
                : (context != null ? getCustomPrompt(context) : DEFAULT_FANSUB_PROMPT_ID);

        new Thread(() -> {
            try {
                // 1. Cek Disk Cache
                File cacheDir = new File(context.getCacheDir(), "sub_cache");
                if (!cacheDir.exists()) cacheDir.mkdirs();

                String cacheKey = sha1(sourceIdentifier + "_" + effectiveModel + "_ai_id");
                File cacheFile = new File(cacheDir, cacheKey + ".json");

                if (!forceRefresh && cacheFile.exists() && cacheFile.length() > 0) {
                    try {
                        String jsonStr = readFile(cacheFile);
                        List<SubtitleCue> cachedCues = deserializeCues(jsonStr);
                        if (!cachedCues.isEmpty() && !containsDominantArabicScript(cachedCues)) {
                            if (callback != null) callback.onSuccess(cachedCues);
                            return;
                        } else if (containsDominantArabicScript(cachedCues)) {
                            // Hapus cache korup berbahasa Arab dari versi sebelumnya
                            cacheFile.delete();
                        }
                    } catch (Exception ignored) {}
                }

                // 2. Batched Cue Translation
                int totalCues = cues.size();
                int batchSize = 40;
                List<SubtitleCue> translatedAll = new ArrayList<>(totalCues);
                String endpointUrl = getEndpointUrl(provider, activeKey);

                for (int lo = 0; lo < totalCues; lo += batchSize) {
                    int hi = Math.min(lo + batchSize, totalCues);
                    List<SubtitleCue> batchCues = cues.subList(lo, hi);

                    // Konteks 4 baris sebelumnya
                    List<String> contextLines = new ArrayList<>();
                    int ctxStart = Math.max(0, lo - 4);
                    for (int c = ctxStart; c < lo; c++) {
                        contextLines.add(prepLine(cues.get(c).text));
                    }

                    // Baris-baris batch
                    Map<Integer, String> batchResults = new HashMap<>();
                    try {
                        String responseContent = callBatchCompletion(
                                endpointUrl, provider, activeKey, effectiveModel,
                                effectivePrompt, contextLines, batchCues
                        );
                        batchResults = parseIdLines(responseContent, batchCues.size());
                    } catch (Exception e) {
                        // Jika batch gagal, batchResults akan kosong dan ditangani oleh hybrid GTX
                    }

                    // Hybrid GTX fallback untuk cue yang tidak ada di respon LLM
                    List<SubtitleCue> missingCues = new ArrayList<>();
                    List<Integer> missingIndices = new ArrayList<>();
                    for (int i = 0; i < batchCues.size(); i++) {
                        int cueId = i + 1;
                        if (!batchResults.containsKey(cueId) || batchResults.get(cueId).trim().isEmpty()) {
                            missingCues.add(batchCues.get(i));
                            missingIndices.add(cueId);
                        }
                    }

                    if (!missingCues.isEmpty()) {
                        try {
                            List<String> gtxTexts = GtxTranslator.translateBatchSync(context, missingCues);
                            for (int m = 0; m < missingIndices.size(); m++) {
                                int cueId = missingIndices.get(m);
                                if (m < gtxTexts.size() && !gtxTexts.get(m).isEmpty()) {
                                    batchResults.put(cueId, gtxTexts.get(m));
                                } else {
                                    batchResults.put(cueId, missingCues.get(m).text);
                                }
                            }
                        } catch (Exception e) {
                            for (int m = 0; m < missingIndices.size(); m++) {
                                batchResults.put(missingIndices.get(m), missingCues.get(m).text);
                            }
                        }
                    }

                    // Gabungkan hasil batch ke list utama
                    for (int i = 0; i < batchCues.size(); i++) {
                        int cueId = i + 1;
                        SubtitleCue orig = batchCues.get(i);
                        String text = batchResults.get(cueId);
                        if (text == null || text.trim().isEmpty()) {
                            text = orig.text;
                        }
                        String sanitized = SmartLexicon.applyUniversalLexicon(text);
                        translatedAll.add(new SubtitleCue(orig.startMs, orig.endMs, sanitized));
                    }

                    if (callback != null) {
                        callback.onProgress(translatedAll.size(), totalCues);
                    }
                }

                if (translatedAll.isEmpty()) {
                    if (callback != null) callback.onError("Gagal menerjemahkan subtitle");
                    return;
                }

                // 3. Simpan ke Cache Disk HANYA jika bukan kontaminasi bahasa Arab saat target Bahasa Indonesia
                if (!containsDominantArabicScript(translatedAll)) {
                    try {
                        writeFile(cacheFile, serializeCues(translatedAll));
                    } catch (Exception ignored) {}
                }

                if (callback != null) {
                    callback.onSuccess(translatedAll);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage() != null ? e.getMessage() : "Kesalahan tidak diketahui");
                }
            }
        }).start();
    }

    private String callBatchCompletion(String endpointUrl,
                                       Provider provider,
                                       String apiKey,
                                       String model,
                                       String systemPrompt,
                                       List<String> contextLines,
                                       List<SubtitleCue> batchCues) throws Exception {
        StringBuilder userBody = new StringBuilder();
        if (contextLines != null && !contextLines.isEmpty()) {
            userBody.append("Konteks (baris sebelumnya, JANGAN diterjemahkan, hanya untuk pemahaman):\n");
            for (String ctx : contextLines) {
                userBody.append("- ").append(ctx).append("\n");
            }
            userBody.append("\n");
        }

        userBody.append("Terjemahkan ").append(batchCues.size()).append(" baris berikut:\n");
        for (int i = 0; i < batchCues.size(); i++) {
            userBody.append(i + 1).append("|").append(prepLine(batchCues.get(i).text)).append("\n");
        }

        String userPrompt = userBody.toString().trim();
        boolean isReasoning = model != null && model.contains("gpt-oss");
        if (isReasoning) {
            userPrompt = "JANGAN sertakan penalaran/thinking. Langsung keluarkan format N|terjemahan secara ringkas.\n\n" + userPrompt;
        }

        JSONArray messages = new JSONArray();
        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.put(sysMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.put(userMsg);

        JSONObject payload = new JSONObject();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("temperature", 0.3);
        if (isReasoning) {
            payload.put("reasoning_effort", "low");
            if (provider == Provider.GROQ) {
                payload.put("reasoning_format", "hidden");
            }
        }

        Request.Builder reqBuilder = new Request.Builder()
                .url(endpointUrl)
                .post(RequestBody.create(payload.toString(), JSON_MEDIA_TYPE))
                .header("Content-Type", "application/json");

        if (apiKey != null && !apiKey.isEmpty()) {
            reqBuilder.header("Authorization", "Bearer " + apiKey);
        }
        if (provider == Provider.GEMINI) {
            reqBuilder.header("x-goog-api-key", apiKey);
        }
        if (provider == Provider.OPENROUTER) {
            reqBuilder.header("HTTP-Referer", "https://shisen.app");
            reqBuilder.header("X-Title", "Shisen");
        }

        try (Response resp = httpClient.newCall(reqBuilder.build()).execute()) {
            if (!resp.isSuccessful()) {
                String errBody = resp.body() != null ? resp.body().string() : "";
                throw new IOException("HTTP " + resp.code() + ": " + errBody);
            }
            String respBody = resp.body() != null ? resp.body().string() : "";
            JSONObject json = new JSONObject(respBody);
            JSONArray choices = json.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject msg = choices.getJSONObject(0).optJSONObject("message");
                if (msg != null) {
                    return msg.optString("content", "");
                }
            }
            return "";
        }
    }

    /**
     * Mem-parsing respons N|teks terjemahan selaras Tatap.
     */
    public static Map<Integer, String> parseIdLines(String content, int maxId) {
        Map<Integer, String> out = new HashMap<>();
        if (content == null || content.isEmpty()) return out;

        String text = THINK_TAG_PATTERN.matcher(content).replaceAll("");
        text = CODE_BLOCK_PATTERN.matcher(text).replaceAll("");

        String[] lines = text.split("\n");
        for (String line : lines) {
            Matcher m = ID_LINE_PATTERN.matcher(line);
            if (!m.matches()) continue;

            int id;
            try {
                id = Integer.parseInt(m.group(1));
            } catch (Exception e) {
                continue;
            }

            String t = m.group(2).trim();
            if (t.contains("|") || t.contains("｜")) {
                String[] parts = t.split("[|｜]");
                if (parts.length >= 2) {
                    t = parts[parts.length - 1].trim();
                }
            }

            // Pulihkan token <br> menjadi baris baru
            t = t.replaceAll("(?i)\\s*<br\\s*/?>\\s*", "\n").trim();

            if (id >= 1 && id <= maxId && !out.containsKey(id) && !t.isEmpty()) {
                out.put(id, t);
            }
        }
        return out;
    }

    public static boolean containsDominantArabicScript(List<SubtitleCue> cues) {
        if (cues == null || cues.isEmpty()) return false;
        int arabicCharCount = 0;
        int totalCharCount = 0;
        for (SubtitleCue c : cues) {
            if (c == null || c.text == null) continue;
            for (char ch : c.text.toCharArray()) {
                if (Character.isWhitespace(ch)) continue;
                totalCharCount++;
                // Aksara Arab Unicode: 0600-06FF, 0750-077F, 08A0-08FF, FB50-FDFF, FE70-FEFF
                if ((ch >= '\u0600' && ch <= '\u06FF') ||
                        (ch >= '\u0750' && ch <= '\u077F') ||
                        (ch >= '\u08A0' && ch <= '\u08FF') ||
                        (ch >= '\uFB50' && ch <= '\uFDFF') ||
                        (ch >= '\uFE70' && ch <= '\uFEFF')) {
                    arabicCharCount++;
                }
            }
        }
        if (totalCharCount == 0) return false;
        // Jika lebih dari 20% karakter non-whitespace adalah aksara Arab, anggap terkontaminasi teks Arab
        return (arabicCharCount * 100 / totalCharCount) >= 20;
    }

    public static String prepLine(String text) {
        if (text == null) return "";
        String t = text.replace("\r", "");
        String[] parts = t.split("\n");
        List<String> nonBlank = new ArrayList<>();
        for (String p : parts) {
            String tr = p.trim();
            if (!tr.isEmpty()) nonBlank.add(tr);
        }
        return String.join(" <br> ", nonBlank);
    }

    private static String sha1(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(text.hashCode());
        }
    }

    private static String readFile(File f) throws Exception {
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] data = new byte[(int) f.length()];
            fis.read(data);
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    private static void writeFile(File f, String content) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String serializeCues(List<SubtitleCue> cues) {
        JSONArray arr = new JSONArray();
        for (SubtitleCue c : cues) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("s", c.startMs);
                obj.put("e", c.endMs);
                obj.put("t", c.text);
                arr.put(obj);
            } catch (Exception ignored) {}
        }
        return arr.toString();
    }

    private static List<SubtitleCue> deserializeCues(String json) {
        List<SubtitleCue> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new SubtitleCue(obj.getLong("s"), obj.getLong("e"), obj.getString("t")));
            }
        } catch (Exception ignored) {}
        return list;
    }

    public String callGemini(String text, String apiKey) throws Exception {
        return callGemini(text, apiKey, DEFAULT_FANSUB_PROMPT_ID, "gemini-2.5-flash");
    }

    public String callGemini(String text, String apiKey, String customPrompt) throws Exception {
        return callGemini(text, apiKey, customPrompt, "gemini-2.5-flash");
    }

    public String callGemini(String text, String apiKey, String customPrompt, String model) throws Exception {
        String base = customBaseUrl != null ? customBaseUrl : "https://generativelanguage.googleapis.com";
        String effectiveModel = (model != null && !model.trim().isEmpty()) ? model.trim() : "gemini-2.5-flash";
        String url = base + "/v1beta/models/" + effectiveModel + ":generateContent";

        String instruction = (customPrompt != null && !customPrompt.trim().isEmpty())
                ? customPrompt.trim() : DEFAULT_FANSUB_PROMPT_ID;

        JSONObject promptPart = new JSONObject();
        promptPart.put("text", instruction + "\n\n" + text);

        JSONArray parts = new JSONArray();
        parts.put(promptPart);

        JSONObject content = new JSONObject();
        content.put("parts", parts);

        JSONArray contents = new JSONArray();
        contents.put(content);

        JSONObject bodyObj = new JSONObject();
        bodyObj.put("contents", contents);

        Request.Builder reqBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(bodyObj.toString(), JSON_MEDIA_TYPE));
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            reqBuilder.header("x-goog-api-key", apiKey.trim());
        }
        Request request = reqBuilder.build();

        try (Response resp = httpClient.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Gemini API error code: " + resp.code());
            }
            String respBody = resp.body() != null ? resp.body().string() : "";
            JSONObject json = new JSONObject(respBody);
            JSONArray candidates = json.optJSONArray("candidates");
            if (candidates != null && candidates.length() > 0) {
                JSONObject first = candidates.getJSONObject(0);
                JSONObject c = first.optJSONObject("content");
                if (c != null) {
                    JSONArray p = c.optJSONArray("parts");
                    if (p != null && p.length() > 0) {
                        return p.getJSONObject(0).optString("text", text);
                    }
                }
            }
            return text;
        }
    }

    public String callGroq(String text, String apiKey) throws Exception {
        return callGroq(text, apiKey, DEFAULT_FANSUB_PROMPT_ID, "llama-3.3-70b-versatile");
    }

    public String callGroq(String text, String apiKey, String customPrompt) throws Exception {
        return callGroq(text, apiKey, customPrompt, "llama-3.3-70b-versatile");
    }

    public String callGroq(String text, String apiKey, String customPrompt, String model) throws Exception {
        String base = customBaseUrl != null ? customBaseUrl : "https://api.groq.com/openai";
        String url = base + "/v1/chat/completions";

        String instruction = (customPrompt != null && !customPrompt.trim().isEmpty())
                ? customPrompt.trim() : DEFAULT_FANSUB_PROMPT_ID;

        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", instruction);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", text);

        JSONArray messages = new JSONArray();
        messages.put(sysMsg);
        messages.put(userMsg);

        String effectiveModel = (model != null && !model.trim().isEmpty()) ? model.trim() : "llama-3.3-70b-versatile";
        JSONObject bodyObj = new JSONObject();
        bodyObj.put("model", effectiveModel);
        bodyObj.put("messages", messages);
        bodyObj.put("temperature", 0.3);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(bodyObj.toString(), JSON_MEDIA_TYPE))
                .build();

        try (Response resp = httpClient.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Groq API error code: " + resp.code());
            }
            String respBody = resp.body() != null ? resp.body().string() : "";
            JSONObject json = new JSONObject(respBody);
            JSONArray choices = json.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject msg = choices.getJSONObject(0).optJSONObject("message");
                if (msg != null) {
                    return msg.optString("content", text);
                }
            }
            return text;
        }
    }

    public String callOpenAi(String text, String apiKey) throws Exception {
        return callOpenAi(text, apiKey, DEFAULT_FANSUB_PROMPT_ID, "gpt-4o-mini");
    }

    public String callOpenAi(String text, String apiKey, String customPrompt) throws Exception {
        return callOpenAi(text, apiKey, customPrompt, "gpt-4o-mini");
    }

    public String callOpenAi(String text, String apiKey, String customPrompt, String model) throws Exception {
        String base = customBaseUrl != null ? customBaseUrl : "https://api.openai.com";
        String url = base + "/v1/chat/completions";

        String instruction = (customPrompt != null && !customPrompt.trim().isEmpty())
                ? customPrompt.trim() : DEFAULT_FANSUB_PROMPT_ID;

        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", instruction);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", text);

        JSONArray messages = new JSONArray();
        messages.put(sysMsg);
        messages.put(userMsg);

        String effectiveModel = (model != null && !model.trim().isEmpty()) ? model.trim() : "gpt-4o-mini";
        JSONObject bodyObj = new JSONObject();
        bodyObj.put("model", effectiveModel);
        bodyObj.put("messages", messages);
        bodyObj.put("temperature", 0.3);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(bodyObj.toString(), JSON_MEDIA_TYPE))
                .build();

        try (Response resp = httpClient.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("OpenAI API error code: " + resp.code());
            }
            String respBody = resp.body() != null ? resp.body().string() : "";
            JSONObject json = new JSONObject(respBody);
            JSONArray choices = json.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject msg = choices.getJSONObject(0).optJSONObject("message");
                if (msg != null) {
                    return msg.optString("content", text);
                }
            }
            return text;
        }
    }

    public String callOllama(String text, String endpointOrKey) throws Exception {
        return callOllama(text, endpointOrKey, DEFAULT_FANSUB_PROMPT_ID, "llama3.2");
    }

    public String callOllama(String text, String endpointOrKey, String customPrompt) throws Exception {
        return callOllama(text, endpointOrKey, customPrompt, "llama3.2");
    }

    public String callOllama(String text, String endpointOrKey, String customPrompt, String model) throws Exception {
        String endpoint = endpointOrKey;
        if (endpoint.startsWith("ollama:")) {
            endpoint = endpoint.substring("ollama:".length());
        }
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            endpoint = "http://" + endpoint;
        }
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        String base = customBaseUrl != null ? customBaseUrl : endpoint;
        String url = base + "/api/generate";

        String instruction = (customPrompt != null && !customPrompt.trim().isEmpty())
                ? customPrompt.trim() : DEFAULT_FANSUB_PROMPT_ID;

        String effectiveModel = (model != null && !model.trim().isEmpty()) ? model.trim() : "llama3.2";
        JSONObject bodyObj = new JSONObject();
        bodyObj.put("model", effectiveModel);
        bodyObj.put("prompt", instruction + "\n\n" + text);
        bodyObj.put("stream", false);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(bodyObj.toString(), JSON_MEDIA_TYPE))
                .build();

        try (Response resp = httpClient.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Ollama error code: " + resp.code());
            }
            String respBody = resp.body() != null ? resp.body().string() : "";
            JSONObject json = new JSONObject(respBody);
            return json.optString("response", text);
        }
    }

    public void translate(String text, String apiKey, Callback callback) {
        translate(text, apiKey, DEFAULT_FANSUB_PROMPT_ID, null, callback);
    }

    public void translate(String text, String apiKey, String customPrompt, Callback callback) {
        translate(text, apiKey, customPrompt, null, callback);
    }

    public void translate(String text, String apiKey, String customPrompt, String model, Callback callback) {
        if (text == null || text.trim().isEmpty()) {
            callback.onSuccess("");
            return;
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Kunci API LLM belum dikonfigurasi"));
            return;
        }

        final String activePrompt = (customPrompt != null && !customPrompt.trim().isEmpty())
                ? customPrompt.trim() : DEFAULT_FANSUB_PROMPT_ID;

        Provider provider = Provider.detect(apiKey);
        final String activeModel = (model != null && !model.trim().isEmpty())
                ? model.trim() : getDefaultModel(provider);

        new Thread(() -> {
            try {
                List<SubtitleCue> singleCue = new ArrayList<>();
                singleCue.add(new SubtitleCue(0, 1000, text));
                String res = callBatchCompletion(
                        getEndpointUrl(provider, apiKey), provider, apiKey, activeModel,
                        activePrompt, null, singleCue
                );
                Map<Integer, String> parsed = parseIdLines(res, 1);
                String translated = parsed.get(1);
                callback.onSuccess(translated != null && !translated.isEmpty() ? translated : text);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }
}
