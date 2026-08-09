package pl.vitaltrack

import android.app.Application
import android.content.Context
import pl.vitaltrack.core.common.HrHistory
import pl.vitaltrack.core.common.Hub
import pl.vitaltrack.core.common.SettingsPort
import pl.vitaltrack.core.database.Db
import pl.vitaltrack.core.healthconnect.HcGateway
import pl.vitaltrack.core.healthconnect.UnavailableHc
import pl.vitaltrack.core.model.AuthState
import pl.vitaltrack.core.security.Vault
import pl.vitaltrack.core.settings.SettingsRepo
import pl.vitaltrack.core.sync.Engine
import pl.vitaltrack.core.sync.Pipeline
import pl.vitaltrack.device.amazfit.Bip6Factory
import pl.vitaltrack.device.api.WatchRegistry

/**
 * VitalTrack — kompozycja (ręczny DI). MINIMALNY start: żadnych usług,
 * żadnego harmonogramu, żadnych czcionek z assetów; baza tworzona leniwie.
 * Przy błędzie startu — zapis do crash.txt i ekran błędu w MainActivity.
 */
class App : Application() {

    companion object {
        @Volatile
        var startupError: Throwable? = null

        fun writeCrash(c: Context, text: String) {
            try {
                android.util.Log.e("VitalTrack", text)
                try { java.io.File(c.filesDir, "crash.txt").writeText(text) } catch (ignored: Exception) {}
                try { c.getExternalFilesDir(null)?.let { java.io.File(it, "crash.txt").writeText(text) } } catch (ignored: Exception) {}
                try {
                    if (android.os.Build.VERSION.SDK_INT >= 29) {
                        val v = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "VitalTrack_crash.txt")
                            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                        }
                        val uri = c.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)
                        if (uri != null) c.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                    }
                } catch (ignored: Exception) {}
            } catch (ignored: Exception) {}
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, t ->
            writeCrash(this, "VitalTrack crash [${thread.name}] " +
                android.os.Build.MODEL + " Android " + android.os.Build.VERSION.RELEASE +
                "\n" + android.util.Log.getStackTraceString(t))
            prev?.uncaughtException(thread, t)
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            val settings = SettingsRepo(this)
            val db = Db(this)
            val vault = Vault(this)
            val hc: HcGateway = UnavailableHc()
            val pipeline = Pipeline(db, settings, hc)
            val engine = Engine(pipeline)
            val registry = WatchRegistry(listOf(Bip6Factory(this)))
            val repo = DeviceRepository(this, settings, vault, registry, engine)
            Hub.init(settings, db, vault, repo, engine, StringsImpl(this), hc)
            repo.addListener(persist)
            seedDemo(db)
        } catch (t: Throwable) {
            startupError = t
            writeCrash(this, "App.onCreate:\n" + android.util.Log.getStackTraceString(t))
        }
    }

    /** Demo data tylko w buildzie --demo (refleksja — bezpieczna w produkcji). */
    private fun seedDemo(db: Db) {
        try {
            val cls = Class.forName("pl.vitaltrack.demo.Demo")
            cls.getMethod("seed", Db::class.java).invoke(null, db)
        } catch (e: ClassNotFoundException) {
            // produkcja — oczekiwane
        } catch (ignored: Exception) {
        }
    }

    /** Zapis danych na żywo do bazy. */
    private val persist = object : pl.vitaltrack.core.common.WatchListener {
        private var lastWrite = 0L
        private var lastMinute = 0
        private var minuteBucket = -1L

        override fun onHeart(bpm: Int, ts: Long) {
            HrHistory.push(bpm)
            val now = System.currentTimeMillis()
            if (now - lastWrite > 4000) { lastWrite = now; Hub.data.insertHr(ts, bpm) }
            val b = ts / 60_000
            if (b != minuteBucket) {
                if (lastMinute > 0) cardio(lastMinute, ts - 60_000)
                minuteBucket = b; lastMinute = 0
            }
            if (bpm > lastMinute) lastMinute = bpm
        }

        override fun onBattery(p: Int) {
            Hub.settings.putInt(SettingsPort.Keys.BATTERY, p)
            Hub.settings.putLong(SettingsPort.Keys.BATTERY_TS, System.currentTimeMillis())
            Hub.data.insertBattery(System.currentTimeMillis(), p)
        }

        override fun onInfo(i: Map<String, String>) {
            i["model"]?.let { Hub.settings.putString(SettingsPort.Keys.MODEL, it) }
            i["wersja FW"]?.let { Hub.settings.putString(SettingsPort.Keys.FW, it) }
        }

        override fun onAuth(s: AuthState) {
            when (s) { AuthState.OK -> Hub.data.log("OK", "Autoryzacja OK.")
                AuthState.FAIL -> Hub.data.log("WARN", "Autoryzacja nieudana."); else -> {} }
        }

        private fun cardio(bpm: Int, ts: Long) {
            val s = Hub.settings
            val mh = pl.vitaltrack.core.model.Estimates.maxHr(
                s.getInt(SettingsPort.Keys.MAX_HR, 0), s.getInt(SettingsPort.Keys.AGE, 30))
            val pts = pl.vitaltrack.core.model.Estimates.cardioMinute(bpm, mh)
            if (pts == 0) return
            val date = pl.vitaltrack.core.common.Time.localDate(ts)
            val d = Hub.data.day(date) ?: pl.vitaltrack.core.model.M.Day(date)
            d.cardioPoints += pts; d.moveMinutes += 1
            Hub.data.upsertDay(d)
        }
    }
}
