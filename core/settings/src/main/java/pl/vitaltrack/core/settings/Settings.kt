package pl.vitaltrack.core.settings

import android.content.Context
import android.content.SharedPreferences
import pl.vitaltrack.core.common.SettingsPort
import pl.vitaltrack.core.sync.Settings

/** Ustawienia lokalne (DataStore-substytut). Klucz parowania NIE jest tu trzymany. */
class SettingsRepo(context: Context) : SettingsPort, Settings {
    private val p: SharedPreferences = context.getSharedPreferences("vitaltrack_prefs", Context.MODE_PRIVATE)

    override fun getString(k: String, def: String): String = p.getString(k, def) ?: def
    override fun putString(k: String, v: String) { p.edit().putString(k, v).apply() }
    override fun getInt(k: String, def: Int): Int = p.getInt(k, def)
    override fun putInt(k: String, v: Int) { p.edit().putInt(k, v).apply() }
    override fun getLong(k: String, def: Long): Long = p.getLong(k, def)
    override fun putLong(k: String, v: Long) { p.edit().putLong(k, v).apply() }
    override fun getBool(k: String, def: Boolean): Boolean = p.getBoolean(k, def)
    override fun putBool(k: String, v: Boolean) { p.edit().putBoolean(k, v).apply() }

    fun forget() {
        p.edit()
            .remove(SettingsPort.Keys.MAC).remove(SettingsPort.Keys.NAME)
            .remove(SettingsPort.Keys.HAS_KEY).remove(SettingsPort.Keys.BATTERY)
            .remove(SettingsPort.Keys.BATTERY_TS).remove(SettingsPort.Keys.FW)
            .remove(SettingsPort.Keys.MODEL).apply()
    }
}
