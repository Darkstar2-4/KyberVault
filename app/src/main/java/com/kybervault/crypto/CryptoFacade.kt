package com.kybervault.crypto

import android.util.Base64

/**
 * Unified cryptographic facade — 3 operation modes:
 *
 *   HYBRID:  Kyber-1024 + X25519 → HKDF → AES-256-GCM  (key exchange, recommended)
 *   KYBER:   Kyber-1024 → AES-256-GCM                   (key exchange, post-quantum only)
 *   AES:     Direct AES-256-GCM with pre-shared key      (ongoing session communication)
 */
object CryptoFacade {

    // ── HYBRID KEM ──

    // ── PURE KYBER KEM ──

    // ── AES SESSION ──

    fun encryptAes(plaintext: String, sessionKey: ByteArray): EncryptedPayload {
        val aad = "KYBERVAULT-AES256GCM-SESSION".toByteArray(Charsets.UTF_8)
        val encryptedData = AesEngine.encrypt(
            plaintext.toByteArray(Charsets.UTF_8), sessionKey, aad
        )
        return EncryptedPayload(
            kemCiphertext = ByteArray(0), x25519EphemeralPubKey = null,
            encryptedData = encryptedData, mode = EncryptionMode.AES, version = 2
        )
    }

    fun decryptAes(payload: EncryptedPayload, sessionKey: ByteArray): String {
        val aad = "KYBERVAULT-AES256GCM-SESSION".toByteArray(Charsets.UTF_8)
        return String(AesEngine.decrypt(payload.encryptedData, sessionKey, aad), Charsets.UTF_8)
    }
}

data class DecryptionResult(
    val plaintext: String,
    val derivedSessionKey: ByteArray
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DecryptionResult) return false
        return plaintext == other.plaintext && derivedSessionKey.contentEquals(other.derivedSessionKey)
    }
    override fun hashCode(): Int = 31 * plaintext.hashCode() + derivedSessionKey.contentHashCode()
}

enum class EncryptionMode(val wireCode: String) {
    HYBRID("H"),
    KYBER("K"),
    AES("A");

    companion object {
        fun fromWireCode(code: String): EncryptionMode = when (code) {
            "H" -> HYBRID; "K" -> KYBER; "A" -> AES; "R" -> KYBER
            else -> throw IllegalArgumentException("Unknown wire code: $code")
        }
    }
}

data class EncryptedPayload(
    val kemCiphertext: ByteArray,
    val x25519EphemeralPubKey: ByteArray?,
    val encryptedData: ByteArray,
    val mode: EncryptionMode,
    val version: Int = 2
) {
    fun serialize(wireFormat: Boolean = true): String {
        val kemB64 = if (kemCiphertext.isEmpty()) "NONE"
        else Base64.encodeToString(kemCiphertext, Base64.NO_WRAP)
        val dataB64 = Base64.encodeToString(encryptedData, Base64.NO_WRAP)
        val x25519B64 = x25519EphemeralPubKey?.let {
            Base64.encodeToString(it, Base64.NO_WRAP)
        } ?: "NONE"
        return if (wireFormat) {
            "KV$version:${mode.wireCode}:$kemB64:$x25519B64:$dataB64"
        } else {
            // Raw format — just Base64 data (for AES session messages) or full concat for KEM
            if (mode == EncryptionMode.AES) dataB64
            else "$kemB64:$x25519B64:$dataB64"
        }
    }

    companion object {
        fun deserialize(encoded: String): EncryptedPayload {
            val parts = encoded.split(":")
            return when (parts[0]) {
                "KV2" -> {
                    require(parts.size == 5) { "Invalid v2 payload" }
                    val mode = EncryptionMode.fromWireCode(parts[1])
                    val kemCt = if (parts[2] == "NONE") ByteArray(0)
                    else Base64.decode(parts[2], Base64.NO_WRAP)
                    val x25519 = if (parts[3] == "NONE") null
                    else Base64.decode(parts[3], Base64.NO_WRAP)
                    val data = Base64.decode(parts[4], Base64.NO_WRAP)
                    EncryptedPayload(kemCt, x25519, data, mode, 2)
                }
                "KV1" -> {
                    require(parts.size == 4) { "Invalid v1 payload" }
                    val kemCt = Base64.decode(parts[2], Base64.NO_WRAP)
                    val data = Base64.decode(parts[3], Base64.NO_WRAP)
                    EncryptedPayload(kemCt, null, data, EncryptionMode.KYBER, 1)
                }
                else -> throw IllegalArgumentException("Unknown wire format: ${parts[0]}")
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedPayload) return false
        return kemCiphertext.contentEquals(other.kemCiphertext) &&
                encryptedData.contentEquals(other.encryptedData) &&
                mode == other.mode && version == other.version
    }

    override fun hashCode(): Int {
        var result = kemCiphertext.contentHashCode()
        result = 31 * result + encryptedData.contentHashCode()
        result = 31 * result + mode.hashCode()
        result = 31 * result + version
        return result
    }
}
