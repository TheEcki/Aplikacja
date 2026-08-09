package pl.vitaltrack.core

import pl.vitaltrack.core.sync.Range
import pl.vitaltrack.core.sync.Ranges
import pl.vitaltrack.core.testing.Assert
import pl.vitaltrack.core.testing.Test

class RangesTest {
    @Test fun first() {
        val now = 1_000_000_000L
        Assert.assertEquals(now - Ranges.INITIAL, Ranges.next(0, now).from)
    }
    @Test fun incremental() {
        val now = 1_000_000_000L
        Assert.assertEquals(900_000_000L, Ranges.next(900_000_000L, now).from)
    }
    @Test fun invalid() {
        var threw = false
        try { Range(10L, 5L) } catch (e: IllegalArgumentException) { threw = true }
        Assert.assertTrue(threw)
    }
}
