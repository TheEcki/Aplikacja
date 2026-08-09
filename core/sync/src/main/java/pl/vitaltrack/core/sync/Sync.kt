package pl.vitaltrack.core.sync

import pl.vitaltrack.core.common.Obs
import pl.vitaltrack.core.common.SyncPhase
import pl.vitaltrack.core.common.Time
import pl.vitaltrack.core.healthconnect.HcExport
import pl.vitaltrack.core.healthconnect.HcResult
import pl.vitaltrack.core.model.AuthState
import pl.vitaltrack.core.model.ConnectionError
import pl.vitaltrack.core.model.M
import pl.vitaltrack.device.api.SyncResult
import pl.vitaltrack.core.model.WatchState
import pl.vitaltrack.device.api.WatchAdapter

/** Pipeline synchronizacji — czysty JVM, testowany. */
class Pipeline(
    private val store: Store,
    private val settings: Settings,
    private val hc: HcExport,
) {
    object Keys {
        const val MODEL = "last_model"; const val FW = "last_fw"
        const val BATTERY = "last_battery"; const val LAST_SYNC = "last_sync_ts"
    }
    object State {
        const val LAST_SYNCED = "last_synced_ts"
        const val STATUS = "last_sync_status"
    }

    @Volatile private var cancelled = false
    fun cancel() { cancelled = true }

    fun run(a: WatchAdapter, onP: (SyncProgress) -> Unit, onR: (SyncResult) -> Unit) {
        val started = System.currentTimeMillis()
        var stored = 0
        var raw = 0
        try {
            onP(SyncProgress(SyncPhase.CONNECT, "łączenie…", 5))
            if (a.state.value !is WatchState.Connected) {
                if (!wait(a)) { return fail(onP, onR, ConnectionError.DeviceUnavailable, "Zegarek niedostępny") }
            }
            onP(SyncProgress(SyncPhase.AUTH, "autoryzacja…", 15))
            if (a.auth.value == AuthState.FAIL) {
                return fail(onP, onR, ConnectionError.AuthenticationFailed, "Sprawdź klucz parowania")
            }
            onP(SyncProgress(SyncPhase.INFO, "informacje…", 25))
            val info = a.infoBlocking()
            for ((k, v) in info) when (k) {
                "model" -> settings.putString(Keys.MODEL, v)
                "wersja FW" -> settings.putString(Keys.FW, v)
            }
            store.log("OK", "Info: ${info.entries.joinToString(" ") { "${it.key}=${it.value}" }}")

            onP(SyncProgress(SyncPhase.BATTERY, "bateria…", 35))
            val bat = a.batteryBlocking()
            if (bat != null) {
                settings.putInt(Keys.BATTERY, bat)
                store.battery(System.currentTimeMillis(), bat)
                val day = store.day(Time.today()) ?: M.Day(Time.today())
                day.battery = bat; store.upsert(day); stored++
            }

            onP(SyncProgress(SyncPhase.RANGE, "zakres…", 45))
            val last = store.getState(State.LAST_SYNCED)?.toLongOrNull() ?: 0L
            val range = Ranges.next(last, System.currentTimeMillis())
            store.log("INFO", "Zakres: ${range.from}–${range.to}" +
                (if (last > 0) " (przyrostowo)" else " (pierwsza)"))

            onP(SyncProgress(SyncPhase.DOWNLOAD, "dane…", 55))
            raw = try { a.activityRawBlocking().size } catch (e: Exception) { store.log("WARN", "raw: ${e.message}"); 0 }

            onP(SyncProgress(SyncPhase.PARSE, "parsowanie…", 65))
            if (raw > 0) store.log("WARN",
                "$raw surowych pakietów — parser Zepp OS 5 niepotwierdzony (docs/BLE_PROTOCOL.md)")

            onP(SyncProgress(SyncPhase.VALIDATE, "walidacja…", 75))
            onP(SyncProgress(SyncPhase.DEDUPE, "deduplikacja…", 80))
            onP(SyncProgress(SyncPhase.STORE, "zapis…", 85))
            onP(SyncProgress(SyncPhase.AGGREGATE, "agregacje…", 90))
            stored += aggregates()

            onP(SyncProgress(SyncPhase.HC, "Health Connect…", 95))
            if (hc.isAvailable) {
                when (val r = hc.exportAll(store.days(14), store.hrRange(
                    System.currentTimeMillis() - 7 * Time.DAY, Long.MAX_VALUE), store.ws(50))) {
                    is HcResult.Ok -> store.log("OK", "HC: ${r.n} rekordów")
                    is HcResult.Failed -> store.log("WARN", "HC: ${r.detail}")
                }
            } else store.log("INFO", "HC niedostępny — pominięto")

            onP(SyncProgress(SyncPhase.DONE, "ukończono", 100))
            store.putState(State.LAST_SYNCED, System.currentTimeMillis().toString())
            store.putState(State.STATUS, "ok")
            settings.putLong(Keys.LAST_SYNC, System.currentTimeMillis())
            val summary = "Synchronizacja w ${(System.currentTimeMillis() - started) / 1000}s" +
                (if (stored > 0) ", $stored rekordów" else "") + (if (raw > 0) " (raw $raw)" else "")
            store.log("OK", summary)
            onR(SyncResult.Success(summary, stored))
        } catch (e: Exception) {
            fail(onP, onR, ConnectionError.Unknown(e), e.message ?: "Błąd")
        }
    }

    private fun fail(onP: (SyncProgress) -> Unit, onR: (SyncResult) -> Unit,
                     e: ConnectionError, d: String) {
        onP(SyncProgress(SyncPhase.DONE, d, 100, ok = false, error = e))
        store.log("ERROR", d); onR(SyncResult.Failure(e, d))
    }

    private fun wait(a: WatchAdapter): Boolean {
        val end = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < end && !cancelled) {
            when (val s = a.state.value) {
                is WatchState.Connected -> return true
                is WatchState.Error -> return false
                else -> {}
            }
            Thread.sleep(200)
        }
        return a.state.value is WatchState.Connected
    }

    private fun aggregates(): Int {
        var n = 0
        val from = Time.startOfDay(System.currentTimeMillis() - Time.DAY)
        val samples = store.hrRange(from, Long.MAX_VALUE)
        if (samples.isEmpty()) return 0
        val byDay = HashMap<String, MutableList<M.HrSample>>()
        for (s in samples) byDay.getOrPut(Time.localDate(s.ts)) { ArrayList() }.add(s)
        for ((date, list) in byDay) {
            val d = store.day(date) ?: M.Day(date)
            var sum = 0; var max = 0
            for (s in list) { sum += s.bpm; if (s.bpm > max) max = s.bpm }
            d.avgHr = Math.round(sum.toDouble() / list.size).toInt(); d.maxHr = max
            store.upsert(d); n++
        }
        return n
    }
}

data class SyncProgress(val phase: SyncPhase, val detail: String = "", val percent: Int = 0,
                        val ok: Boolean = true, val error: ConnectionError? = null)

interface Store {
    fun log(level: String, msg: String)
    fun getState(k: String): String?
    fun putState(k: String, v: String)
    fun day(date: String): M.Day?
    fun upsert(d: M.Day)
    fun hrRange(from: Long, to: Long): List<M.HrSample>
    fun battery(ts: Long, p: Int)
    fun days(n: Int): List<M.Day>
    fun ws(n: Int): List<M.Workout>
}

interface Settings {
    fun putString(k: String, v: String)
    fun putInt(k: String, v: Int)
    fun putLong(k: String, v: Long)
}

data class Range(val from: Long, val to: Long) { init { require(from <= to) } }

object Ranges {
    const val INITIAL = 7L * 24 * 3600 * 1000
    const val MAX_BACK = 90L * 24 * 3600 * 1000
    fun next(last: Long, now: Long): Range {
        val from = if (last > 0) last else maxOf(now - MAX_BACK, now - INITIAL)
        return Range(from, now)
    }
}

interface DedupeStore { fun dup(h: String): Boolean; fun remember(h: String) }

class Dedupe(private val s: DedupeStore) {
    fun isNew(device: String, ts: Long, type: String, v: Long, sec: Long = 0): Boolean {
        val h = pl.vitaltrack.device.amazfit.Codec.stableHash(device, ts, type, v, sec)
        return if (s.dup(h)) false else { s.remember(h); true }
    }
    fun filterNew(device: String, type: String, recs: List<Pair<Long, Long>>): List<Pair<Long, Long>> =
        recs.filter { isNew(device, it.first, type, it.second) }
}

class MemDedupe : DedupeStore {
    private val seen = HashSet<String>()
    override fun dup(h: String) = h in seen
    override fun remember(h: String) { seen.add(h) }
}
