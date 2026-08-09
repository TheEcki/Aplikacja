package pl.vitaltrack.device.amazfit

import pl.vitaltrack.core.model.M
import java.security.MessageDigest

object Codec {
    fun heartRate(p: ByteArray): Int? {
        if (p.size < 2) return null
        val sixteen = (p[0].toInt() and 0x01) == 1
        return if (sixteen && p.size >= 3) ((p[2].toInt() and 0xff) shl 8) or (p[1].toInt() and 0xff)
        else p[1].toInt() and 0xff
    }

    fun battery(p: ByteArray): Int? = if (p.isEmpty()) null else p[0].toInt() and 0xff

    fun alarmPacket(a: M.Alarm): ByteArray {
        val mask = if (a.repeatMask == 0) 0x80 else (a.repeatMask and 0x7f)
        return byteArrayOf(0x02,
            ((if (a.enabled) 0x80 else 0) or (a.index and 0x0f)).toByte(),
            a.hour.toByte(), a.minute.toByte(), mask.toByte())
    }

    object Opt {
        val LIFT_ON = byteArrayOf(0x06, 0x05, 0x00, 0x01)
        val LIFT_OFF = byteArrayOf(0x06, 0x05, 0x00, 0x00)
        val GOAL_ON = byteArrayOf(0x06, 0x06, 0x00, 0x01)
        val GOAL_OFF = byteArrayOf(0x06, 0x06, 0x00, 0x00)
        val DISC_ON = byteArrayOf(0x06, 0x0c, 0x00, 0x01, 0, 0, 0, 0)
        val DISC_OFF = byteArrayOf(0x06, 0x0c, 0x00, 0x00, 0, 0, 0, 0)
        val CALLER_ON = byteArrayOf(0x06, 0x10, 0x00, 0x00, 0x01)
        val CALLER_OFF = byteArrayOf(0x06, 0x10, 0x00, 0x00, 0x00)
        val METRIC_ON = byteArrayOf(0x06, 0x03, 0x00, 0x00)
        val METRIC_OFF = byteArrayOf(0x06, 0x03, 0x00, 0x01)
        val T24_ON = byteArrayOf(0x06, 0x02, 0x00, 0x01)
        val T24_OFF = byteArrayOf(0x06, 0x02, 0x00, 0x00)
    }

    fun notifyPacket(text: String): ByteArray {
        val b = text.toByteArray(Charsets.UTF_8)
        val p = ByteArray(3 + b.size)
        p[0] = 0x05; p[1] = 0x01; p[2] = b.size.toByte()
        System.arraycopy(b, 0, p, 3, b.size)
        return p
    }

    fun stableHash(device: String, ts: Long, type: String, value: Long, sec: Long = 0): String {
        val d = MessageDigest.getInstance("SHA-256")
            .digest("$device|$ts|$type|$value|$sec".toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(64)
        for (b in d) sb.append(String.format("%02x", b.toInt() and 0xff))
        return sb.toString()
    }
}
