package pl.vitaltrack.device

import pl.vitaltrack.core.model.M
import pl.vitaltrack.core.testing.Assert
import pl.vitaltrack.core.testing.Test
import pl.vitaltrack.device.amazfit.Codec

class CodecTest {
    @Test fun hr() {
        Assert.assertEquals(72, Codec.heartRate(byteArrayOf(0x00, 72)))
        Assert.assertEquals(300, Codec.heartRate(byteArrayOf(0x01, (300 and 0xff).toByte(), (300 shr 8).toByte())))
        Assert.assertNull(Codec.heartRate(ByteArray(0)))
    }
    @Test fun battery() {
        Assert.assertEquals(78, Codec.battery(byteArrayOf(78)))
        Assert.assertNull(Codec.battery(ByteArray(0)))
    }
    @Test fun alarm() {
        val p = Codec.alarmPacket(M.Alarm(0, true, 7, 30, 0))
        Assert.assertArrayEquals(byteArrayOf(0x02, 0x80.toByte(), 0x07, 0x1e, 0x80.toByte()), p)
    }
    @Test fun hash() {
        val h1 = Codec.stableHash("AA", 1000L, "hr", 72L)
        Assert.assertEquals(h1, Codec.stableHash("AA", 1000L, "hr", 72L))
        Assert.assertFalse(h1 == Codec.stableHash("AA", 1000L, "hr", 73L))
        Assert.assertEquals(64, h1.length)
    }
    @Test fun notifyPacket() {
        val p = Codec.notifyPacket("Test")
        Assert.assertEquals(0x05, p[0].toInt() and 0xff)
        Assert.assertEquals(p.size, 3 + 4)
    }
}
