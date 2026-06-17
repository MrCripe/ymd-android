package com.mrcriper.ymd.data.remote.download

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-CTR decryption for Yandex Music encrypted audio payloads.
 *
 * Port of [py-ref/ymd/api.py::decrypt_data]. Counter starts at 0 (initial
 * counter block is `nonce (12 zero bytes) || 0x00000000`). No padding.
 */
object Decryptor {
    private const val ALGORITHM = "AES/CTR/NoPadding"

    fun decrypt(encrypted: ByteArray, hexKey: String): ByteArray {
        require(hexKey.length == 32) { "Expected 32 hex chars (16 bytes), got ${hexKey.length}" }
        val keyBytes = hexKey.hexToBytes()
        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(keyBytes, "AES")
        // pycryptodome `nonce=bytes(12)` ⇒ initial counter block = bytes(12) || counter[0..4]=0x00000001.
        // Java AES/CTR IV = initial counter block, so we use 12 zero bytes + 00000001.
        val iv = ByteArray(12) + byteArrayOf(0, 0, 0, 1)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(iv))
        return cipher.doFinal(encrypted)
    }

    private fun String.hexToBytes(): ByteArray {
        require(length % 2 == 0) { "Odd hex string length" }
        val out = ByteArray(length / 2)
        for (i in indices step 2) {
            val hi = Character.digit(this[i], 16)
            val lo = Character.digit(this[i + 1], 16)
            require(hi >= 0 && lo >= 0) { "Invalid hex char at $i" }
            out[i / 2] = ((hi shl 4) or lo).toByte()
        }
        return out
    }
}
