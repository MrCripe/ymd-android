package com.mrcriper.ymd.data.remote.download

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DecryptorTest {

    private val IV = ByteArray(12) + byteArrayOf(0, 0, 0, 1)

    private fun encrypt(plain: ByteArray, key: String): ByteArray {
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(hex(key), "AES"),
            IvParameterSpec(IV),
        )
        return cipher.doFinal(plain)
    }

    @Test fun `decrypt inverts encrypt with ctr`() {
        val key = "00112233445566778899aabbccddeeff"
        val plain = "YMD test plaintext".toByteArray()
        val encrypted = encrypt(plain, key)
        val decrypted = Decryptor.decrypt(encrypted, key)
        assertArrayEquals(plain, decrypted)
    }

    @Test fun `different keys produce different plaintexts`() {
        val plain = "hello".toByteArray()
        val k1 = "00000000000000000000000000000000"
        val k2 = "00000000000000000000000000000001"
        val e1 = encrypt(plain, k1)
        val e2 = encrypt(plain, k2)
        assertArrayEquals(plain, Decryptor.decrypt(e1, k1))
        assertArrayEquals(plain, Decryptor.decrypt(e2, k2))
        assertNotEquals(e1.toList(), e2.toList())
    }

    @Test fun `invalid hex length throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Decryptor.decrypt(ByteArray(16), "abcd")
        }
    }

    @Test fun `empty payload returns empty`() {
        val plain = ByteArray(0)
        val key = "00112233445566778899aabbccddeeff"
        val encrypted = encrypt(plain, key)
        assertEquals(0, Decryptor.decrypt(encrypted, key).size)
    }

    private fun hex(s: String): ByteArray = ByteArray(s.length / 2) {
        ((s[it * 2].digitToInt(16) shl 4) or s[it * 2 + 1].digitToInt(16)).toByte()
    }
}