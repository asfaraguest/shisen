package id.shisen.app.data.db;

import android.content.Context;

/**
 * ShisenDatabase menyediakan akses ke basis data aplikasi Shisen.
 *
 * PENTING (Invarian Zero Data Loss):
 * Nama file fisik SQLite dipertahankan sebagai "menatap.db" dan alias Keystore
 * sebagai "menatap_keystore_alias_v1" agar tidak menghilangkan riwayat tontonan,
 * bookmark, atau progres episode pengguna saat melakukan upgrade dari versi sebelumnya.
 */
public final class ShisenDatabase {

    private ShisenDatabase() {}

    public static MenatapDatabase getInstance(Context context) {
        return MenatapDatabase.getInstance(context);
    }
}
