package pl.vitaltrack.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import pl.vitaltrack.core.common.SecretsPort
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Klucz parowania: Android Keystore (AES-256-GCM), prefs wykluczone z backupu. */
class Vault(context: Context) : SecretsPort {
    private val prefs = context.getSharedPreferences("vitaltrack_auth", Context.MODE_PRIVATE)
    private val ks: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    override fun hasKey(): Boolean = prefs.contains("cipher") && loadKey() != null

    override fun saveKey(hex: String): Boolean {
        val b = hexToBytes(hex) ?: return false
        return try {
            val key = master()
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.ENCRYPT_MODE, key)
            val ct = c.doFinal(b)
            val blob = ByteArray(12 + ct.size)
            System.arraycopy(c.iv, 0, blob, 0, 12)
            System.arraycopy(ct, 0, blob, 12, ct.size)
            prefs.edit().putString("cipher", Base64.encodeToString(blob, Base64.NO_WRAP)).apply()
            true
        } catch (e: Exception) { false }
    }

    override fun loadKey(): ByteArray? {
        val b64 = prefs.getString("cipher", null) ?: return null
        return try {
            val blob = Base64.decode(b64, Base64.NO_WRAP)
            if (blob.size < 28) return null
            val iv = blob.copyOfRange(0, 12)
            val ct = blob.copyOfRange(12, blob.size)
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, master(), GCMParameterSpec(128, iv))
            val d = c.doFinal(ct)
            if (d.size == 16) d else null
        } catch (e: Exception) { null }
    }

    override fun clearKey() { prefs.edit().remove("cipher").apply() }

    private fun master(): SecretKey {
        ks.getKey("vitaltrack_auth", null) as? SecretKey ?: run {
            val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            kg.init(KeyGenParameterSpec.Builder("vitaltrack_auth",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
            return kg.generateKey()
        }
        return ks.getKey("vitaltrack_auth", null) as SecretKey
    }

    private fun hexToBytes(h: String): ByteArray? {
        val s = h.trim().replace("0x", "").replace("0X", "").replace(" ", "")
        if (s.length != 32 || !s.matches(Regex("(?i)[0-9a-f]{32}"))) return null
        return ByteArray(16) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }
}
