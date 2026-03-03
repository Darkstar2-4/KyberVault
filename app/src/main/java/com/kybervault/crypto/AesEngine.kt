package com.kybervault.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM authenticated encryption engine.
 *
 * Used as the symmetric layer after Kyber KEM key agreement.
 * GCM provides both confidentiality AND integrity (authenticated encryption).
 *
 * Ciphertext format: [12-byte IV][ciphertext + 16-byte GCM tag]
 */
object AesEngine {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE_BITS = 256
    private const val IV_SIZE_BYTES = 12        // NIST recommended for GCM
    private const val TAG_SIZE_BITS = 128       // Full 128-bit authentication tag

    /**
     * Encrypt plaintext using AES-256-GCM.
     *
     * @param plaintext The data to encrypt
     * @param key 32-byte AES-256 key (typically from Kyber shared secret)
     * @param aad Optional additional authenticated data (authenticated but not encrypted)
     * @return Ciphertext with prepended IV: [IV (12 bytes)][ciphertext + tag]
     */
    fun encrypt(
        plaintext: ByteArray,
        key: ByteArray,
        aad: ByteArray? = null
    ): ByteArray {
        require(key.size == KEY_SIZE_BITS / 8) {
            "Key must be exactly 32 bytes for AES-256. Got ${key.size} bytes."
        }

        val iv = ByteArray(IV_SIZE_BYTES).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(TAG_SIZE_BITS, iv)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        aad?.let { cipher.updateAAD(it) }

        val ciphertext = cipher.doFinal(plaintext)

        // Prepend IV to ciphertext: [IV][ciphertext+tag]
        return iv + ciphertext
    }

    /**
     * Decrypt AES-256-GCM ciphertext.
     *
     * @param ciphertextWithIv The full ciphertext with prepended IV
     * @param key 32-byte AES-256 key
     * @param aad Optional AAD that was used during encryption (must match exactly)
     * @return Decrypted plaintext
     * @throws javax.crypto.AEADBadTagException if authentication fails (tampered data)
     */
    fun decrypt(
        ciphertextWithIv: ByteArray,
        key: ByteArray,
        aad: ByteArray? = null
    ): ByteArray {
        require(key.size == KEY_SIZE_BITS / 8) {
            "Key must be exactly 32 bytes for AES-256. Got ${key.size} bytes."
        }
        require(ciphertextWithIv.size > IV_SIZE_BYTES) {
            "Ciphertext too short — must contain at least IV + tag."
        }

        val iv = ciphertextWithIv.copyOfRange(0, IV_SIZE_BYTES)
        val ciphertext = ciphertextWithIv.copyOfRange(IV_SIZE_BYTES, ciphertextWithIv.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(TAG_SIZE_BITS, iv)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        aad?.let { cipher.updateAAD(it) }

        return cipher.doFinal(ciphertext)
    }

}
