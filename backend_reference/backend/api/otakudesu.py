"""Scraper dan resolver stream untuk Otakudesu (Secondary Source Tatap).
Mengambil episode dan mendekripsi stream HLS dari player embed putarin.xyz (AES-256-GCM).
"""

import re
import json
import base64
import html as htmlmod
import urllib.parse
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from config import OTAKU_BASE, HI_UA

HEADERS = {
    "User-Agent": HI_UA,
    "Referer": OTAKU_BASE + "/",
}

_otaku_client = None

def _get_otaku_client():
    global _otaku_client
    if _otaku_client is None:
        import httpx
        limits = httpx.Limits(max_connections=20, max_keepalive_connections=10, keepalive_expiry=60.0)
        _otaku_client = httpx.Client(
            follow_redirects=True,
            timeout=httpx.Timeout(12.0, connect=5.0),
            limits=limits,
            headers=HEADERS
        )
    return _otaku_client

def close_otaku_client():
    """Tutup persistent client Otakudesu saat aplikasi shutdown."""
    global _otaku_client
    if _otaku_client is not None:
        try:
            _otaku_client.close()
        except Exception:
            pass
        _otaku_client = None

def _fetch(url: str, referer: str = None, timeout: int = 12) -> str:
    """Fetch helper menggunakan curl_cffi dengan fallback httpx pool dan TLS desync."""
    h = dict(HEADERS)
    if referer:
        h["Referer"] = referer
    try:
        from curl_cffi import requests as creq
        r = creq.get(url, headers=h, impersonate="chrome124", timeout=timeout, allow_redirects=True)
        if r.status_code == 200:
            return r.text
    except Exception:
        pass

    try:
        client = _get_otaku_client()
        r = client.get(url, headers=h)
        if r.status_code == 200:
            return r.text
    except Exception:
        pass

    # Fallback TLS Desync jika diblokir ISP
    try:
        from api.desync import tls_desync_request
        raw = tls_desync_request(url, headers=h, timeout=timeout)
        txt = raw.decode("utf-8", errors="replace")
        if txt and ("<html" in txt.lower() or "{" in txt):
            return txt
    except Exception:
        pass

    raise RuntimeError(f"Gagal memuat URL dari Otakudesu: {url}")


def search(query: str, limit: int = 10) -> list:
    """Cari anime di Otakudesu. Return [{title, url, id, poster}]."""
    clean_q = re.sub(r"[^\w\s-]", "", query).strip()
    clean_q = re.sub(r"\bund\s+panzer\b", "panzer", clean_q, flags=re.I)
    clean_q = re.sub(r"\band\s+panzer\b", "panzer", clean_q, flags=re.I)
    url = f"{OTAKU_BASE}/?s={urllib.parse.quote_plus(clean_q)}&post_type=anime"
    html = _fetch(url)
    
    # Format kartu di otakudesu: <a class="ot-k" href="..."> ... <h2 class="ot-k-j">Title</h2>
    cards = re.findall(r'<a class="ot-k"\s+href="([^"]+)">.*?<img[^>]*src="([^"]+)".*?<h2 class="ot-k-j">([^<]+)</h2>', html, re.S)
    results = []
    for href, poster, title in cards[:limit]:
        title = htmlmod.unescape(title.strip())
        slug_m = re.search(r'/anime/([^/]+)/?', href)
        slug = slug_m.group(1) if slug_m else href
        results.append({
            "title": title,
            "url": href,
            "id": slug,
            "poster": poster,
        })
    return results


def get_episodes(anime_url_or_slug: str) -> list:
    """Ambil daftar episode untuk anime Otakudesu.
    Return [{ep: 1, title: 'Episode 1', url: 'https://...'}] urut dari ep 1 ke atas.
    """
    if anime_url_or_slug.startswith("http"):
        url = anime_url_or_slug
    else:
        url = f"{OTAKU_BASE}/anime/{anime_url_or_slug.strip('/')}/"

    html = _fetch(url)
    # Match: <a class="ot-ep" href="..."><span>Naruto Episode 1</span></a>
    eps_raw = re.findall(r'<a class="ot-ep"\s+href="([^"]+)">\s*<span>([^<]+)</span>', html)
    out = []
    seen = set()
    for href, title in eps_raw:
        if href in seen:
            continue
        seen.add(href)
        # Parse nomor episode dari title
        ep_num_m = re.search(r'(?:episode|eps|ep)\s*#?\s*(\d+)', title, re.I)
        ep_no = int(ep_num_m.group(1)) if ep_num_m else len(out) + 1
        out.append({
            "ep": ep_no,
            "title": title.strip(),
            "url": href
        })

    # Urutkan episode dari yang terkecil
    out.sort(key=lambda x: x["ep"])
    return out


def _decrypt_putarin_config(embed_url: str) -> dict:
    """Buka iframe embed putarin, ambil nonce & key, lalu dekripsi config AES-256-GCM."""
    html = _fetch(embed_url, referer=OTAKU_BASE + "/")
    px_m = re.search(r'window\.__PX\s*=\s*({[^<]+});', html)
    if not px_m:
        raise RuntimeError("Embed player token (__PX) tidak ditemukan")

    px = json.loads(px_m.group(1))
    nonce = px["n"]
    ciphertext_b64 = px["d"]

    # Ambil origin host dari embed_url (misal https://putarin.biz atau https://putarin.xyz)
    parsed_embed = urllib.parse.urlparse(embed_url)
    origin = f"{parsed_embed.scheme}://{parsed_embed.netloc}"

    # Ambil kunci AES dari /api/pk?n={nonce}
    key_url = f"{origin}/api/pk?n={urllib.parse.quote(nonce)}"
    key_hex = _fetch(key_url, referer=embed_url).strip()
    if len(key_hex) < 64:
        raise RuntimeError("Kunci dekripsi stream tidak valid")

    key_bytes = bytes.fromhex(key_hex)
    raw_data = base64.b64decode(ciphertext_b64)
    iv = raw_data[:12]
    ct = raw_data[12:]

    aesgcm = AESGCM(key_bytes)
    decrypted_bytes = aesgcm.decrypt(iv, ct, None)
    config = json.loads(decrypted_bytes.decode("utf-8"))
    config["_origin"] = origin
    return config


def _extract_season_num(t: str) -> int:
    t = (t or "").lower()
    m = re.search(r"\b(?:season\s*(\d+)|(\d+)(?:nd|rd|th|st)\s*season|part\s*(\d+)|s(\d+)|saishuushou\s*(\d+)|finale\s*(\d+))\b|(?:\b|\s)(\d+)$", t)
    if m:
        for g in m.groups():
            if g:
                try:
                    return int(g)
                except ValueError:
                    pass
    return 1


def _clean_title_tokens(t: str) -> set:
    t = (t or "").replace("&", " and ")
    t = re.sub(r"\b(?:season\s*\d+|\d+(?:nd|rd|th|st)\s*season|part\s*\d+|s\d+|saishuushou\s*\d+|finale\s*\d+)\b|(?:\b|\s)\d+$", "", t, flags=re.I)
    t = re.sub(r"[^a-zA-Z0-9\s]", " ", t)
    tokens = set()
    for tok in t.lower().split():
        if tok == "ovas": tok = "ova"
        elif tok == "specials": tok = "special"
        elif tok == "movies": tok = "movie"
        elif tok == "finale": tok = "saishuushou"
        tokens.add(tok)
    return tokens


def _is_title_match(target_title: str, candidate_title: str) -> bool:
    if not target_title or not candidate_title:
        return False
    # 1. Exact normalized match (tanpa spasi & simbol)
    def _norm(s):
        s = (s or "").lower().replace("&", "and")
        s = re.sub(r"\bund\b", "and", s)
        return re.sub(r"[^a-zA-Z0-9]", "", s)
    if _norm(target_title) == _norm(candidate_title):
        return True

    # 2. Validasi nomor season / part (mencegah Season 2 match ke Season 1)
    season_a = _extract_season_num(target_title)
    season_b = _extract_season_num(candidate_title)
    if (season_a > 1 or season_b > 1) and season_a != season_b:
        return False

    # 3. Validasi token overlap
    tokens_a = _clean_title_tokens(target_title)
    tokens_b = _clean_title_tokens(candidate_title)
    if not tokens_a or not tokens_b:
        return False

    # 4. Isolasi Movie/Film vs Serial
    movie_keywords = {"movie", "film", "gekijouban"}
    if bool(tokens_a.intersection(movie_keywords)) != bool(tokens_b.intersection(movie_keywords)):
        return False

    # 5. Isolasi part / saishuushou (mencegah TV series match ke Saishuushou / part lanjutan)
    part_keywords = {"part", "saishuushou"}
    has_part_a = bool(tokens_a.intersection(part_keywords))
    has_part_b = bool(tokens_b.intersection(part_keywords))
    if has_part_a != has_part_b:
        return False

    stopwords = {"the", "a", "an", "of", "in", "to", "and", "und", "der", "die", "das", "no", "wa", "ga", "ni", "de", "sub", "indo"}
    sig_common = tokens_a.intersection(tokens_b) - stopwords
    sig_a = tokens_a - stopwords
    sig_b = tokens_b - stopwords

    if not sig_a or not sig_b:
        return (len(tokens_a.intersection(tokens_b)) / max(len(tokens_a), len(tokens_b))) >= 0.75

    overlap = len(sig_common) / max(len(sig_a), len(sig_b))
    return overlap >= 0.55


def resolve_stream(title_or_slug: str, ep: int = 1) -> dict:
    """Selesaikan URL stream m3u8 Otakudesu untuk anime & episode tertentu.
    Return dict terstandar dengan master m3u8, variants, referer, server name, dsb.
    """
    if "hentai" in (title_or_slug or "").lower():
        raise RuntimeError("Konten Hentai tidak tersedia di Otakudesu")

    # 1. Cari anime jika bukan direct URL
    episodes = []
    if "otakudesu" in title_or_slug and "/anime/" in title_or_slug:
        episodes = get_episodes(title_or_slug)
    else:
        # Coba ambil langsung jika title_or_slug merupakan slug anime otakudesu yang valid
        if "-" in title_or_slug and not " " in title_or_slug:
            try:
                direct_url = f"{OTAKU_BASE}/anime/{title_or_slug.strip('/')}/"
                direct_eps = get_episodes(direct_url)
                if direct_eps:
                    episodes = direct_eps
            except Exception:
                pass

        if not episodes:
            # Bersihkan hash suffix (cth: "steel-ball-run-jojo-no-kimyou-na-bouken-jtgn9fq" -> "steel ball run...")
            # dan angka suffix ID hianime (cth: "attack-on-titan-season-3-866")
            clean_str = re.sub(r'-[a-z0-9]{5,10}$', '', title_or_slug)
            clean_title = re.sub(r'-\d+$', '', clean_str).replace("-", " ").strip()
            search_res = search(clean_title)
            if not search_res and clean_title != title_or_slug:
                raw_title = re.sub(r'-\d+$', '', title_or_slug).replace("-", " ").strip()
                search_res = search(raw_title)

            if not search_res:
                # Coba cari tanpa embel-embel nomor season/part jika pencarian awal kosong
                simplified = re.sub(r'\b(?:season\s*\d+|\d+(?:nd|rd|th|st)\s*season|part\s*\d+|s\d+)\b', '', clean_title, flags=re.I).strip()
                if simplified and simplified != clean_title:
                    search_res = search(simplified)

            if not search_res:
                raise RuntimeError(f"Anime '{title_or_slug}' tidak ditemukan di Otakudesu")

            # Validasi kecocokan judul: wajib lolos verifikasi, jangan pernah fallback buta ke res[0]
            matched_item = None
            for cand in search_res:
                if _is_title_match(clean_title, cand.get("title", "")):
                    matched_item = cand
                    break

            if not matched_item:
                raise RuntimeError(f"Anime '{title_or_slug}' tidak memiliki judul yang cocok di Otakudesu")

            # Ambil episode dari hasil pencarian yang terverifikasi cocok
            episodes = get_episodes(matched_item["url"])

    if not episodes:
        raise RuntimeError("Tidak ada daftar episode di Otakudesu")

    # Cari episode yang cocok
    target_ep = next((e for e in episodes if e["ep"] == int(ep)), None)
    if not target_ep:
        # Fallback ke index jika nomor episode off
        idx = int(ep) - 1
        if 0 <= idx < len(episodes):
            target_ep = episodes[idx]
        else:
            raise RuntimeError(f"Episode {ep} belum rilis di Otakudesu")

    # 2. Ambil halaman episode dan ekstrak embed putarin
    ep_html = _fetch(target_ep["url"], referer=OTAKU_BASE + "/")
    iframe_m = re.search(r'<iframe[^>]*src="([^"]*putarin\.[a-z]+/e/[^"]+)"', ep_html)
    if not iframe_m:
        # Fallback regex untuk iframe apapun di dalam .ot-player
        iframe_m = re.search(r'<div class="ot-player"[^>]*>.*?<iframe[^>]*src="([^"]+)"', ep_html, re.S)
    if not iframe_m:
        raise RuntimeError("Iframe embed pemutar Otakudesu tidak ditemukan")

    embed_url = iframe_m.group(1)
    if embed_url.startswith("//"):
        embed_url = "https:" + embed_url

    # 3. Dekripsi player config putarin
    cfg = _decrypt_putarin_config(embed_url)
    origin = cfg.get("_origin", "https://putarin.xyz")
    hls_rel = cfg.get("file") or (cfg.get("sources") or [{}])[0].get("file")
    if not hls_rel:
        raise RuntimeError("File stream tidak ditemukan di konfigurasi putarin")

    hls_master = urllib.parse.urljoin(origin, hls_rel)

    # 4. Validasi master playlist & ekstrak variants
    variants = [
        {"q": "720p", "url": hls_master},
        {"q": "auto", "url": hls_master}
    ]

    return {
        "master": hls_master,
        "variants": variants,
        "sub": None,
        "sub_lang": "Indonesian (Hardsub)",
        "referer": origin + "/",
        "server": "Otakudesu (Sub Indo)",
        "source": "otakudesu",
        "tracks": [],
        "subtitles": [],
    }
