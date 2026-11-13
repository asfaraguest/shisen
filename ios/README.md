# Shisen for iOS (視線 iOS Edition)

Aplikasi streaming anime native iOS (100% Pure Swift & SwiftUI), bebas iklan, guest-only, dengan paritas fitur penuh Shisen Android & Tatap backend.

## Fitur Utama
- **Native SwiftUI (iOS 16+)**: Antarmuka modern, cepat, dan responsif dengan tema Deep Glacial Obsidian.
- **Katalog Lengkap AniList**: Trending, Paling Populer, Seasonal, dan Pencarian judul anime.
- **Multi-Source Streaming 3-Tier**: HiAnime (MegaCloud HLS) → AniChan → Otakudesu.
- **Smart Merge Subtitle ID**: Menggabungkan dialog bahasa Indonesia dengan terjemahan otomatis Signs/OP/ED.
- **AI Subtitle Terintegrasi**: Cerebras (`llama3.3-70b`), Google Gemini, Groq Cloud, NVIDIA NIM, OpenAI, dan OpenRouter Free Tier.
- **AniSkip Auto-Skip**: Otomatis mendeteksi dan melewati intro/outro lagu pembuka & penutup.
- **DPI & Anti-Blokir**: DNS-over-HTTPS (DoH Cloudflare/Google) terintegrasi untuk bypass pemblokiran ISP lokal.

## Build & Sideloading
1. **Buka di Xcode**:
   Buka file `ios/Shisen.xcodeproj` di Xcode (macOS).
2. **Build IPA untuk Sideloading**:
   ```bash
   xcodebuild -project ios/Shisen.xcodeproj -scheme Shisen -configuration Release -destination 'generic/platform=iOS' CODE_SIGN_IDENTITY="" CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO clean build
   ```
3. **Pemasangan di iPhone/iPad**:
   Pasang file `shisen-ios-release-*.ipa` menggunakan **SideStore**, **AltStore**, **TrollStore**, **Scarlet**, atau **LiveContainer**.
