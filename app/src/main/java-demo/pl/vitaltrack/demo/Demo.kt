package pl.vitaltrack.demo

import pl.vitaltrack.core.common.Time
import pl.vitaltrack.core.database.Db
import pl.vitaltrack.core.model.M

/** DEMO-ONLY (--demo). Produkcja NIE zawiera tej klasy. */
object Demo {
    fun seed(db: Db) {
        if (db.getState("demo_seeded") == "1") return
        val now = System.currentTimeMillis()
        val base = now - 13 * Time.DAY
        for (i in 0 until 14) {
            val date = Time.localDate(base + i * Time.DAY)
            val d = M.Day(date)
            d.steps = 5200 + ((i * 1373) % 6800)
            d.distanceM = d.steps * 72 / 100
            d.calories = 280 + d.steps / 18
            d.moveMinutes = 22 + (i % 3) * 9
            d.cardioPoints = 3 + (i % 5)
            d.sleepMin = 380 + (i % 4) * 35
            d.deepMin = d.sleepMin * 18 / 100
            d.lightMin = d.sleepMin * 52 / 100
            d.remMin = d.sleepMin * 22 / 100
            d.awakeMin = d.sleepMin * 8 / 100
            d.restingHr = 58 + (i % 3)
            d.avgHr = 74 + (i % 5)
            d.maxHr = 128 + (i % 7) * 6
            if (date == Time.today()) d.battery = 78
            db.upsert(d)
        }
        val dayStart = Time.startOfDay(now)
        var ts = dayStart - Time.DAY
        while (ts < now) {
            val hour = Time.hourOf(ts)
            db.insertHr(ts, 58 + ((hour * 7) % 40) + (if (hour in 8..19) 10 else 0))
            ts += 5 * 60 * 1000L
        }
        db.putState("demo_seeded", "1")
    }
}
