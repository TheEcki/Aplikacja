package pl.vitaltrack.device.amazfit

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Handshake Huami (FEE1/00000009, Gadgetbridge/Amazfish):
 * 01 08+key → 10 01 01 → 02 08 → 10 02 01+random → 03 08+AES-128-ECB → 10 03 01
 */
object Auth {
    sealed interface Packet {
        object KeyAccepted : Packet
        class Challenge(val random: ByteArray) : Packet
        object Success : Packet
        object KeyChange : Packet
        class Rejected(val code: Byte) : Packet
        object Unknown : Packet
    }

    fun keyPacket(key: ByteArray): ByteArray = prepend(byteArrayOf(0x01, 0x08), key)
    fun challengeRequest(): ByteArray = byteArrayOf(0x02, 0x08)
    fun challengeResponse(ch: ByteArray, key: ByteArray): ByteArray =
        prepend(byteArrayOf(0x03, 0x08), aes(ch, key))

    fun parse(p: ByteArray): Packet {
        if (p.size < 3 || p[0].toInt() != 0x10) return Packet.Unknown
        return when {
            p[1].toInt() == 0x01 && p[2].toInt() == 0x01 -> Packet.KeyAccepted
            p[1].toInt() == 0x02 && p[2].toInt() == 0x01 && p.size >= 19 -> {
                val r = ByteArray(16); System.arraycopy(p, 3, r, 0, 16); Packet.Challenge(r)
            }
            p[1].toInt() == 0x03 && p[2].toInt() == 0x01 -> Packet.Success
            p[1].toInt() == 0x04 && p[2].toInt() == 0x01 -> Packet.KeyChange
            else -> Packet.Rejected(p[1])
        }
    }

    fun aes(block: ByteArray, key: ByteArray): ByteArray {
        require(block.size == 16)
        val c = Cipher.getInstance("AES/ECB/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        return c.doFinal(block)
    }

    private fun prepend(pre: ByteArray, data: ByteArray): ByteArray {
        val out = ByteArray(pre.size + data.size)
        System.arraycopy(pre, 0, out, 0, pre.size)
        System.arraycopy(data, 0, out, pre.size, data.size)
        return out
    }
}
