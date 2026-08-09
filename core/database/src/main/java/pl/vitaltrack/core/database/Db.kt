package pl.vitaltrack.core.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import pl.vitaltrack.core.common.DataPort
import pl.vitaltrack.core.model.M
import pl.vitaltrack.core.sync.DedupeStore
import pl.vitaltrack.core.sync.Store
import java.util.Locale

/** SQLite — lokalna baza jest źródłem prawdy dla UI. Migracje bez destrukcji. */
class Db(context: Context) : SQLiteOpenHelper(context, "vitaltrack.db", null, 1),
    DataPort, Store, DedupeStore {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE daily (date TEXT PRIMARY KEY,
            steps INTEGER DEFAULT 0, calories INTEGER DEFAULT 0, move_minutes INTEGER DEFAULT 0,
            cardio_points INTEGER DEFAULT 0, distance_m INTEGER DEFAULT 0,
            sleep_min INTEGER DEFAULT 0, deep_min INTEGER DEFAULT 0, light_min INTEGER DEFAULT 0,
            rem_min INTEGER DEFAULT 0, awake_min INTEGER DEFAULT 0,
            resting_hr INTEGER DEFAULT 0, avg_hr INTEGER DEFAULT 0, max_hr INTEGER DEFAULT 0,
            battery INTEGER DEFAULT -1)""")
        db.execSQL("CREATE TABLE hr_samples (ts INTEGER PRIMARY KEY, bpm INTEGER NOT NULL)")
        db.execSQL("""CREATE TABLE workouts (id INTEGER PRIMARY KEY AUTOINCREMENT,
            type TEXT, start_ts INTEGER, end_ts INTEGER, duration_sec INTEGER,
            distance_m INTEGER, avg_hr INTEGER, max_hr INTEGER, calories INTEGER,
            avg_pace INTEGER, source TEXT, route TEXT)""")
        db.execSQL("CREATE TABLE sync_log (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, level TEXT, message TEXT)")
        db.execSQL("CREATE TABLE battery_samples (ts INTEGER PRIMARY KEY, percent INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE sync_state (key TEXT PRIMARY KEY, value TEXT)")
        db.execSQL("CREATE TABLE dedupe (hash TEXT PRIMARY KEY, ts INTEGER)")
        db.execSQL("CREATE INDEX idx_hr ON hr_samples(ts)")
    }

    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) { /* przyszłe migracje — bez destrukcji */ }

    // ---- daily
    private fun vals(d: M.Day) = ContentValues().apply {
        put("date", d.date); put("steps", d.steps); put("calories", d.calories)
        put("move_minutes", d.moveMinutes); put("cardio_points", d.cardioPoints)
        put("distance_m", d.distanceM); put("sleep_min", d.sleepMin)
        put("deep_min", d.deepMin); put("light_min", d.lightMin); put("rem_min", d.remMin)
        put("awake_min", d.awakeMin); put("resting_hr", d.restingHr)
        put("avg_hr", d.avgHr); put("max_hr", d.maxHr); put("battery", d.battery)
    }

    private fun read(c: Cursor): M.Day = M.Day(
        date = c.getString(c.getColumnIndexOrThrow("date")),
        steps = c.getInt(c.getColumnIndexOrThrow("steps")),
        calories = c.getInt(c.getColumnIndexOrThrow("calories")),
        moveMinutes = c.getInt(c.getColumnIndexOrThrow("move_minutes")),
        cardioPoints = c.getInt(c.getColumnIndexOrThrow("cardio_points")),
        distanceM = c.getInt(c.getColumnIndexOrThrow("distance_m")),
        sleepMin = c.getInt(c.getColumnIndexOrThrow("sleep_min")),
        deepMin = c.getInt(c.getColumnIndexOrThrow("deep_min")),
        lightMin = c.getInt(c.getColumnIndexOrThrow("light_min")),
        remMin = c.getInt(c.getColumnIndexOrThrow("rem_min")),
        awakeMin = c.getInt(c.getColumnIndexOrThrow("awake_min")),
        restingHr = c.getInt(c.getColumnIndexOrThrow("resting_hr")),
        avgHr = c.getInt(c.getColumnIndexOrThrow("avg_hr")),
        maxHr = c.getInt(c.getColumnIndexOrThrow("max_hr")),
        battery = c.getInt(c.getColumnIndexOrThrow("battery")),
    )

    private fun field(d: M.Day, k: String): Long = when (k) {
        "steps" -> d.steps.toLong(); "calories" -> d.calories.toLong()
        "move_minutes" -> d.moveMinutes.toLong(); "cardio_points" -> d.cardioPoints.toLong()
        "distance_m" -> d.distanceM.toLong(); "sleep_min" -> d.sleepMin.toLong()
        "deep_min" -> d.deepMin.toLong(); "light_min" -> d.lightMin.toLong()
        "rem_min" -> d.remMin.toLong(); "awake_min" -> d.awakeMin.toLong()
        "resting_hr" -> d.restingHr.toLong(); "avg_hr" -> d.avgHr.toLong()
        "max_hr" -> d.maxHr.toLong(); "battery" -> d.battery.toLong(); else -> 0L
    }

    override fun upsert(d: M.Day) {
        val db = writableDatabase
        val merged = ContentValues()
        val old = day(d.date)
        for (k in vals(d).keySet()) {
            if (k == "date") continue
            val v = vals(d).get(k) as? Number ?: continue
            val nv = v.toLong()
            if (nv != 0L) merged.put(k, nv)
            else if (old != null && field(old, k) != 0L) merged.put(k, field(old, k))
            else merged.put(k, nv)
        }
        merged.put("date", d.date)
        db.insertWithOnConflict("daily", null, merged, SQLiteDatabase.CONFLICT_REPLACE)
    }

    override fun day(date: String): M.Day? {
        readableDatabase.query("daily", null, "date=?", arrayOf(date), null, null, null).use {
            return if (it.moveToFirst()) read(it) else null
        }
    }

    override fun lastDays(n: Int): List<M.Day> {
        val out = ArrayList<M.Day>()
        readableDatabase.rawQuery("SELECT * FROM daily ORDER BY date DESC LIMIT ?", arrayOf(n.toString())).use {
            while (it.moveToNext()) out.add(read(it))
        }
        return out
    }

    // ---- hr
    override fun insertHr(ts: Long, bpm: Int) {
        if (bpm <= 0 || bpm > 255) return
        val v = ContentValues().apply { put("ts", ts); put("bpm", bpm) }
        writableDatabase.insertWithOnConflict("hr_samples", null, v, SQLiteDatabase.CONFLICT_IGNORE)
    }

    override fun insertHrBulk(s: List<Pair<Long, Int>>) {
        if (s.isEmpty()) return
        val db = writableDatabase; db.beginTransaction()
        try { for ((t, b) in s) insertHr(t, b); db.setTransactionSuccessful() } finally { db.endTransaction() }
    }

    override fun hr(from: Long, to: Long): List<M.HrSample> {
        val out = ArrayList<M.HrSample>()
        readableDatabase.rawQuery("SELECT ts,bpm FROM hr_samples WHERE ts>=? AND ts<=? ORDER BY ts",
            arrayOf(from.toString(), to.toString())).use {
            while (it.moveToNext()) out.add(M.HrSample(it.getLong(0), it.getInt(1)))
        }
        return out
    }

    override fun latestHr(): M.HrSample? {
        readableDatabase.rawQuery("SELECT ts,bpm FROM hr_samples ORDER BY ts DESC LIMIT 1", null).use {
            return if (it.moveToFirst()) M.HrSample(it.getLong(0), it.getInt(1)) else null
        }
    }

    override fun hrStats(from: Long, to: Long): Triple<Int, Int, Int> {
        readableDatabase.rawQuery(
            "SELECT COALESCE(AVG(bpm),0),COALESCE(MIN(bpm),0),COALESCE(MAX(bpm),0) FROM hr_samples WHERE ts>=? AND ts<=?",
            arrayOf(from.toString(), to.toString())).use {
            return if (it.moveToFirst()) Triple(Math.round(it.getDouble(0)).toInt(), it.getInt(1), it.getInt(2))
            else Triple(0, 0, 0)
        }
    }

    // ---- workouts
    override fun insertWorkout(w: M.Workout): Long {
        val v = ContentValues().apply {
            put("type", w.type); put("start_ts", w.startTs); put("end_ts", w.endTs)
            put("duration_sec", w.durationSec); put("distance_m", w.distanceM)
            put("avg_hr", w.avgHr); put("max_hr", w.maxHr); put("calories", w.calories)
            put("avg_pace", w.avgPaceSecPerKm); put("source", w.source)
            val sb = StringBuilder()
            for ((i, p) in w.route.withIndex()) {
                if (i > 0) sb.append(';')
                sb.append(String.format(Locale.US, "%.6f,%.6f,%.0f", p[0], p[1], p[2]))
            }
            put("route", sb.toString())
        }
        return writableDatabase.insert("workouts", null, v)
    }

    private fun wread(c: Cursor): M.Workout {
        val w = M.Workout(
            id = c.getLong(c.getColumnIndexOrThrow("id")),
            type = c.getString(c.getColumnIndexOrThrow("type")),
            startTs = c.getLong(c.getColumnIndexOrThrow("start_ts")),
            endTs = c.getLong(c.getColumnIndexOrThrow("end_ts")),
            durationSec = c.getInt(c.getColumnIndexOrThrow("duration_sec")),
            distanceM = c.getInt(c.getColumnIndexOrThrow("distance_m")),
            avgHr = c.getInt(c.getColumnIndexOrThrow("avg_hr")),
            maxHr = c.getInt(c.getColumnIndexOrThrow("max_hr")),
            calories = c.getInt(c.getColumnIndexOrThrow("calories")),
            avgPaceSecPerKm = c.getInt(c.getColumnIndexOrThrow("avg_pace")),
            source = c.getString(c.getColumnIndexOrThrow("source")),
        )
        val r = c.getString(c.getColumnIndexOrThrow("route"))
        if (!r.isNullOrEmpty()) for (pt in r.split(";")) {
            val p = pt.split(",")
            if (p.size >= 3) try { w.route.add(doubleArrayOf(p[0].toDouble(), p[1].toDouble(), p[2].toDouble())) } catch (ignored: Exception) {}
        }
        return w
    }

    override fun workouts(n: Int): List<M.Workout> {
        val out = ArrayList<M.Workout>()
        readableDatabase.rawQuery("SELECT * FROM workouts ORDER BY start_ts DESC LIMIT ?", arrayOf(n.toString())).use {
            while (it.moveToNext()) out.add(wread(it))
        }
        return out
    }

    override fun deleteWorkout(id: Long) { writableDatabase.delete("workouts", "id=?", arrayOf(id.toString())) }

    // ---- battery / log / state
    override fun insertBattery(ts: Long, p: Int) = battery(ts, p)

    override fun upsertDay(d: M.Day) = upsert(d)

    override fun battery(ts: Long, p: Int) {
        val v = ContentValues().apply { put("ts", ts); put("percent", p) }
        writableDatabase.insertWithOnConflict("battery_samples", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    override fun battery(n: Int): List<M.BatterySample> {
        val out = ArrayList<M.BatterySample>()
        readableDatabase.rawQuery("SELECT ts,percent FROM battery_samples ORDER BY ts DESC LIMIT ?", arrayOf(n.toString())).use {
            while (it.moveToNext()) out.add(M.BatterySample(it.getLong(0), it.getInt(1)))
        }
        return out
    }

    override fun log(level: String, msg: String) {
        val v = ContentValues().apply { put("ts", System.currentTimeMillis()); put("level", level); put("message", msg) }
        writableDatabase.insert("sync_log", null, v)
    }

    override fun syncLog(n: Int): List<M.SyncLogEntry> {
        val out = ArrayList<M.SyncLogEntry>()
        readableDatabase.rawQuery("SELECT id,ts,level,message FROM sync_log ORDER BY id DESC LIMIT ?", arrayOf(n.toString())).use {
            while (it.moveToNext()) out.add(M.SyncLogEntry(it.getLong(0), it.getLong(1), it.getString(2), it.getString(3)))
        }
        return out
    }

    override fun clearLog() { writableDatabase.delete("sync_log", null, null) }
    override fun clearAll() {
        val db = writableDatabase; db.beginTransaction()
        try {
            for (t in listOf("daily", "hr_samples", "workouts", "sync_log", "battery_samples", "sync_state", "dedupe"))
                db.delete(t, null, null)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    override fun getState(k: String): String? {
        readableDatabase.query("sync_state", arrayOf("value"), "key=?", arrayOf(k), null, null, null).use {
            return if (it.moveToFirst()) it.getString(0) else null
        }
    }

    override fun putState(k: String, v: String) {
        val c = ContentValues().apply { put("key", k); put("value", v) }
        writableDatabase.insertWithOnConflict("sync_state", null, c, SQLiteDatabase.CONFLICT_REPLACE)
    }

    override fun dup(h: String): Boolean {
        readableDatabase.query("dedupe", arrayOf("hash"), "hash=?", arrayOf(h), null, null, "1").use { return it.moveToFirst() }
    }

    override fun remember(h: String) {
        val c = ContentValues().apply { put("hash", h); put("ts", System.currentTimeMillis()) }
        writableDatabase.insertWithOnConflict("dedupe", null, c, SQLiteDatabase.CONFLICT_IGNORE)
    }

    // ---- porty Store
    override fun hrRange(from: Long, to: Long): List<M.HrSample> = hr(from, to)
    override fun days(n: Int): List<M.Day> = lastDays(n)
    override fun ws(n: Int): List<M.Workout> = workouts(n)
}
