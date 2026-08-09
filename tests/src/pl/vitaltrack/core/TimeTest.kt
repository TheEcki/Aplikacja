package pl.vitaltrack.core

import pl.vitaltrack.core.common.Time
import pl.vitaltrack.core.testing.Assert
import pl.vitaltrack.core.testing.Test
import java.util.TimeZone

class TimeTest {
    @Test fun localDate_usesZone() {
        val z = TimeZone.getTimeZone("Europe/Warsaw")
        val ts = java.time.Instant.parse("2026-08-07T20:30:00Z").toEpochMilli()
        Assert.assertEquals("2026-08-07", Time.localDate(ts, z))
    }
    @Test fun localDate_crossesMidnight() {
        val z = TimeZone.getTimeZone("Europe/Warsaw")
        val ts = java.time.Instant.parse("2026-08-07T22:30:00Z").toEpochMilli()
        Assert.assertEquals("2026-08-08", Time.localDate(ts, z))
        Assert.assertEquals("2026-08-07", Time.utcDate(ts))
    }
    @Test fun addDays() {
        Assert.assertEquals("2026-09-01", Time.addDays("2026-08-31", 1))
        Assert.assertEquals("2027-01-01", Time.addDays("2026-12-31", 1))
        Assert.assertEquals("2028-03-01", Time.addDays("2028-02-28", 2))
    }
    @Test fun startOfDay() {
        val z = TimeZone.getTimeZone("Europe/Warsaw")
        val ts = java.time.Instant.parse("2026-08-07T20:30:00Z").toEpochMilli()
        val start = Time.startOfDay(ts, z)
        Assert.assertEquals("2026-08-07", Time.localDate(start, z))
        Assert.assertEquals(0, Time.hourOf(start, z))
    }
    @Test fun iso() {
        val ts = java.time.Instant.parse("2026-08-07T20:30:00Z").toEpochMilli()
        Assert.assertEquals("2026-08-07T20:30:00Z", Time.isoUtc(ts))
    }
}
