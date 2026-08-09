package pl.vitaltrack.device

import pl.vitaltrack.core.testing.Assert
import pl.vitaltrack.core.testing.Test
import pl.vitaltrack.device.amazfit.Auth

class AuthTest {
    private val key = byteArrayOf(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15)
    private val ch = byteArrayOf(0x10,0x20,0x30,0x40,0x50,0x60,0x70,0x80.toByte(),
        0x90.toByte(),0xa0.toByte(),0xb0.toByte(),0xc0.toByte(),0xd0.toByte(),0xe0.toByte(),0xf0.toByte(),0x00)

    @Test fun keyPacket() {
        val p = Auth.keyPacket(key)
        Assert.assertEquals(18, p.size)
        Assert.assertEquals(0x01, p[0].toInt() and 0xff)
        Assert.assertArrayEquals(key, p.copyOfRange(2, 18))
    }
    @Test fun challengeReq() { Assert.assertArrayEquals(byteArrayOf(0x02, 0x08), Auth.challengeRequest()) }
    @Test fun response() {
        val p = Auth.challengeResponse(ch, key)
        Assert.assertEquals(18, p.size)
        Assert.assertEquals(0x03, p[0].toInt() and 0xff)
        Assert.assertFalse(p.copyOfRange(2, 18).contentEquals(ch))
    }
    @Test fun aesFips() {
        val k = hex("2b7e151628aed2a6abf7158809cf4f3c")
        val pt = hex("3243f6a8885a308d313198a2e0370734")
        Assert.assertArrayEquals(hex("3925841d02dc09fbdc118597196a0b32"), Auth.aes(pt, k))
    }
    @Test fun parse() {
        Assert.assertTrue(Auth.parse(byteArrayOf(0x10,0x01,0x01)) is Auth.Packet.KeyAccepted)
        val c = Auth.parse(byteArrayOf(0x10,0x02,0x01).plus(ch))
        Assert.assertTrue(c is Auth.Packet.Challenge)
        Assert.assertTrue(Auth.parse(byteArrayOf(0x10,0x03,0x01)) is Auth.Packet.Success)
        Assert.assertTrue(Auth.parse(byteArrayOf(0x10,0x04,0x01)) is Auth.Packet.KeyChange)
        Assert.assertTrue(Auth.parse(byteArrayOf(0x10,0x08,0x01)) is Auth.Packet.Rejected)
        Assert.assertTrue(Auth.parse(byteArrayOf(1,2,3)) is Auth.Packet.Unknown)
    }
    private fun hex(s: String): ByteArray = ByteArray(s.length / 2) { s.substring(it*2, it*2+2).toInt(16).toByte() }
}
