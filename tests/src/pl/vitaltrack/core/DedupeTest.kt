package pl.vitaltrack.core

import pl.vitaltrack.core.sync.Dedupe
import pl.vitaltrack.core.sync.MemDedupe
import pl.vitaltrack.core.testing.Assert
import pl.vitaltrack.core.testing.Test

class DedupeTest {
    @Test fun sameTwice() {
        val d = Dedupe(MemDedupe())
        Assert.assertTrue(d.isNew("AA", 1000L, "hr", 72L))
        Assert.assertFalse(d.isNew("AA", 1000L, "hr", 72L))
    }
    @Test fun differentIsNew() {
        val d = Dedupe(MemDedupe())
        Assert.assertTrue(d.isNew("AA", 1000L, "hr", 72L))
        Assert.assertTrue(d.isNew("AA", 1000L, "hr", 73L))
        Assert.assertTrue(d.isNew("BB", 1000L, "hr", 72L))
    }
    @Test fun filterNew() {
        val d = Dedupe(MemDedupe())
        val recs = listOf(1000L to 70L, 1001L to 71L)
        Assert.assertEquals(2, d.filterNew("AA", "hr", recs).size)
        Assert.assertEquals(0, d.filterNew("AA", "hr", recs).size)
    }
}
