package pl.vitaltrack.core.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Czas: dane jako epoch-millis UTC, daty lokalne wyliczane przez strefę. */
object Time {
    const val MIN = 60_000L
    const val HOUR = 3_600_000L
    const val DAY = 24 * HOUR

    private fun fmt(p: String, ts: Long, z: TimeZone): String {
        val f = SimpleDateFormat(p, Locale.US); f.timeZone = z; return f.format(Date(ts))
    }

    fun localDate(ts: Long, z: TimeZone = TimeZone.getDefault()): String = fmt("yyyy-MM-dd", ts, z)
    fun utcDate(ts: Long): String = fmt("yyyy-MM-dd", ts, TimeZone.getTimeZone("UTC"))
    fun today(z: TimeZone = TimeZone.getDefault()): String = localDate(System.currentTimeMillis(), z)
    fun isoUtc(ts: Long): String = fmt("yyyy-MM-dd'T'HH:mm:ss'Z'", ts, TimeZone.getTimeZone("UTC"))
    fun hhmm(ts: Long): String = fmt("HH:mm", ts, TimeZone.getDefault())

    fun addDays(date: String, days: Int): String {
        return try {
            val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val c = Calendar.getInstance(); c.time = f.parse(date) ?: return date
            c.add(Calendar.DAY_OF_YEAR, days); f.format(c.time)
        } catch (e: Exception) { date }
    }

    fun startOfDay(ts: Long, z: TimeZone = TimeZone.getDefault()): Long {
        val c = Calendar.getInstance(z); c.timeInMillis = ts
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun hourOf(ts: Long, z: TimeZone = TimeZone.getDefault()): Int =
        Calendar.getInstance(z).apply { timeInMillis = ts }.get(Calendar.HOUR_OF_DAY)

    fun minuteBucket(ts: Long): Long = ts / MIN
}
