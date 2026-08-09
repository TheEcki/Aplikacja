package pl.vitaltrack.core.log

import android.util.Log

/** Logowanie z regułami prywatności: bez klucza, MAC maskowany, raw tylko debug. */
object AppLog {
    const val TAG = "VitalTrack"
    @Volatile var verboseBle = false

    fun d(m: String, s: String) { Log.d(TAG, "[$m] $s") }
    fun i(m: String, s: String) { Log.i(TAG, "[$m] $s") }
    fun w(m: String, s: String) { Log.w(TAG, "[$m] $s") }
    fun e(m: String, s: String, t: Throwable? = null) { Log.e(TAG, "[$m] $s", t) }
    fun ble(m: String, dir: Char, chr: String, hex: String) {
        if (verboseBle) Log.d(TAG, "[$m] BLE $dir $chr = $hex")
    }
}
