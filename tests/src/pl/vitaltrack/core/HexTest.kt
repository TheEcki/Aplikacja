package pl.vitaltrack.core

import pl.vitaltrack.core.common.Hex
import pl.vitaltrack.core.testing.Assert
import pl.vitaltrack.core.testing.Test

class HexTest {
    @Test fun roundtrip() {
        val b = byteArrayOf(0x00, 0x0f, 0x10, 0x7f.toByte(), 0x80.toByte(), 0xff.toByte())
        Assert.assertEquals("000f107f80ff", Hex.encode(b))
        Assert.assertArrayEquals(b, Hex.decode(Hex.encode(b))!!)
    }
    @Test fun invalid() {
        Assert.assertNull(Hex.decode("xyz")); Assert.assertNull(Hex.decode("123")); Assert.assertNull(Hex.decode(""))
    }
    @Test fun normalize() { Assert.assertArrayEquals(byteArrayOf(0x00, 0x11, 0x22), Hex.decode("0x00 11 22")!!) }
    @Test fun mask() {
        Assert.assertEquals("aa:bb:••:••:ee:ff", Hex.maskMac("aa:bb:cc:dd:ee:ff"))
        Assert.assertEquals("••••", Hex.maskMac("x"))
    }
    @Test fun is32() {
        Assert.assertTrue(Hex.is32("00112233445566778899aabbccddeeff"))
        Assert.assertFalse(Hex.is32("00112233445566778899aabbccddee"))
    }
}
