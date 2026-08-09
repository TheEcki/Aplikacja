package pl.vitaltrack.core.common

import java.util.Locale

object Hex {
    fun encode(b: ByteArray): String {
        val sb = StringBuilder(b.size * 2)
        for (x in b) sb.append(String.format(Locale.US, "%02x", x.toInt() and 0xff))
        return sb.toString()
    }

    fun decode(h: String): ByteArray? {
        val s = h.trim().replace("0x", "").replace("0X", "").replace(" ", "")
        if (s.isEmpty() || s.length % 2 != 0 || !s.matches(Regex("(?i)[0-9a-f]+"))) return null
        val out = ByteArray(s.length / 2)
        for (i in out.indices) out[i] = s.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        return out
    }

    fun maskMac(mac: String): String {
        val p = mac.trim().split(":")
        return if (p.size != 6) "••••" else p[0] + ":" + p[1] + ":••:••:" + p[4] + ":" + p[5]
    }

    fun is32(s: String): Boolean = s.trim().matches(Regex("(?i)[0-9a-f]{32}"))
}

class AuthGuard(private val b: ByteArray) {
    val value: ByteArray get() = b
    val valid: Boolean get() = b.size == 16
    override fun toString(): String = "<auth-key:${if (valid) "16B" else "invalid"}>"
}
