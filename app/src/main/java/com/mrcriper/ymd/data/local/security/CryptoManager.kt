package com.mrcriper.ymd.data.local.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Wraps Android Keystore-backed EncryptedSharedPreferences and exposes AES/GCM helpers.
 * Tokens never leave this layer in plaintext.
 */
class CryptoManager(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            "auth_tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun putToken(accountKey: String, token: String) {
        prefs.edit().putString(accountKey, token).apply()
    }

    fun getToken(accountKey: String): String? = prefs.getString(accountKey, null)

    fun removeToken(accountKey: String) {
        prefs.edit().remove(accountKey).apply()
    }

    fun allAccounts(): Set<String> = prefs.all.keys

    fun encryptBytes(plain: ByteArray): EncryptedBlob {
        val key = generateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ct = cipher.doFinal(plain)
        return EncryptedBlob(encryptedKey = key.encoded, iv = iv, ciphertext = ct)
    }

    fun decryptBytes(blob: EncryptedBlob): ByteArray {
        val key = javax.crypto.spec.SecretKeySpec(blob.encryptedKey, "AES")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, blob.iv))
        return cipher.doFinal(blob.ciphertext)
    }

    private fun generateKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(KEY_BITS)
        return keyGen.generateKey()
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val KEY_BITS = 256
    }
}

data class EncryptedBlob(
    val encryptedKey: ByteArray,
    val iv: ByteArray,
    val ciphertext: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is EncryptedBlob &&
            encryptedKey.contentEquals(other.encryptedKey) &&
            iv.contentEquals(other.iv) &&
            ciphertext.contentEquals(other.ciphertext))

    override fun hashCode(): Int =
        encryptedKey.contentHashCode() * 31 + iv.contentHashCode() * 17 + ciphertext.contentHashCode()
}

@Suppress("unused")
private val random = SecureRandom()
