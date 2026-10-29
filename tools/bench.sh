#!/usr/bin/env bash
# tools/bench.sh — Alat ukur performa Shisen di perangkat nyata (via adb).
#
# PROTOKOL PENGUKURAN (ikuti agar hasil dapat dibandingkan antar-build):
#   1. HP: Wi-Fi AMBIL SAYA on ("asal sama LAN"), kecerahan disetel tetap (Settings > Developer > Stay awake + brightness 50%),
#      mode baterai dinonaktifkan (saver off), lokasi mati.
#   2. Sesi pengukuran: buka satu episode yang SAMA dari awal sampai selesai 30 MENIT tanpa interaksi (pause minimal).
#   3. Jangan ganti bahasa/tema di tengah pengukuran; tutup semua aplikasi lain.
#   4. Jalankan skrip ini SEBELUM dan SESUDAH build yang mau dibandingkan, di HP yang sama.
#   5. Output otomatis berupa tabel markdown — salin hasilnya ke docs/PERF.md.
#
# Pakai: ./tools/bench.sh            (cold start + meminfo + meminfo playback + batterystats)
#        ./tools/bench.sh --skip-battery   (lewati reset batterystats, untuk sekadar cold start cepat)
set -euo pipefail

PKG="${PKG:-id.shisen.app}"
ACT="${ACT:-id.shisen.app.ui.home.MainActivity}"
COLD_START_RUNS=5
SESSION_MINUTES="${SESSION_MINUTES:-30}"

echo "== Shisen bench =="; echo "pkg=$PKG activity=$PKG/$ACT"
echo "- Pastikan HP terhubung: $(adb devices)"

send_key() { adb shell input keyevent "$1"; }

# === (a) Reset batterystats ===
if [ "${1:-}" != "--skip-battery" ]; then
  echo "== Reset baterystats =="; adb shell dumpsys batterystats --reset >/dev/null; echo "batterystats direset."
fi

# === (b) Cold start 5x, ambil TotalTime lalu median ===
echo "== Cold start x$COLD_START_RUNS =="
cold_times=()
for i in $(seq 1 $COLD_START_RUNS); do
  adb shell am force-stop "$PKG"
  sleep 2
  out=$(adb shell am start -W -n "$PKG/$ACT")
  t=$(echo "$out" | grep -o 'TotalTime: [0-9]*' | grep -o '[0-9]*' | head -1)
  ct=$(echo "$out" | grep -o 'WaitTime: [0-9]*' | grep -o '[0-9]*' | head -1)
  echo "run$i TotalTime=${t}ms WaitTime=${ct}ms"
  cold_times+=("$t")
  sleep 5
done
median_cold=$(printf '%s\n' "${cold_times[@]}" | sort -n | awk '{a[NR]=$1} END{ if (NR%2) print a[(NR+1)/2]; else print int((a[NR/2]+a[NR/2+1])/2) }')
echo "median Cold Start TotalTime: ${median_cold}ms"

# === (c) meminfo saat idle di katalog ===
echo "== Meminfo idle katalog =="
adb shell am force-stop "$PKG"; sleep 2
adb shell am start -W -n "$PKG/$ACT" >/dev/null
sleep 10
adb shell dumpsys meminfo "$PKG" | head -30 > /tmp/shisen_meminfo_idle.txt
cat /tmp/shisen_meminfo_idle.txt

# === (d) meminfo setelah playback; user saat ini harus membuka satu episode ===
echo ">> SIRA PERANGKAT: buka sebuah episode SEKARANG, biarkan memutar ${SESSION_MINUTES} menit tanpa menyentuh HP..."
read -r -p "Tekan ENTER saat episode sudah dimulai bermain" _

end=$((SECONDS + SESSION_MINUTES*60))
while [ $SECONDS -lt $end ]; do
  sleep 60
  if ! adb shell pidof "$PKG" >/dev/null 2>&1; then echo "!! Proses app mati — hentikan pengukuran playback"; break; fi
done

echo "== Meminfo setelah playback =="; adb shell dumpsys meminfo "$PKG" | head -30 > /tmp/shisen_meminfo_playback.txt
cat /tmp/shisen_meminfo_playback.txt

# === (e) batterystats sesi ===
echo "== batterystats sesi =="; adb shell dumpsys batterystats "$PKG" > /tmp/shisen_batterystats.txt
grep -E 'u0a[0-9]+ .*Wake start|Early wakeup|Background time|Foreground time|Uid .*: .*Bluetooth|Keep awake' /tmp/shisen_batterystats.txt | head -20 || true

echo
echo "== RINGKASAN MARKDOWN (salin ke docs/PERF.md) =="
cast(){ awk '/TOTAL PSS/{print $NF}' "$1" | head -1; }
cast_idle=$(cast /tmp/shisen_meminfo_idle.txt)
cast_play=$(cast /tmp/shisen_meminfo_playback.txt)
cat <<EOF
| Metrik | Nilai |
|---|---|
| Median Cold Start (TotalTime) | ${median_cold} ms |
| Meminfo idle katalog (TOTAL PSS) | ${cast_idle:-?} KB |
| Meminfo setelah playback (TOTAL PSS) | ${cast_play:-?} KB |
| Jumlah wake lock / sesi | lihat /tmp/shisen_batterystats.txt |
EOF
echo "Selesai."
