package com.kybervault.crypto

import com.kybervault.util.SecureWipe
import java.security.PublicKey
import java.security.PrivateKey
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Hybrid KEM combiner: Kyber-1024 + X25519.
 *
 * Combines two independent shared secrets via HKDF-SHA256.
 * Security = max(Kyber, X25519) — attacker must break BOTH.
 */
object HybridKem {

    private val HKDF_SALT = "KyberVault-Hybrid-v1".toByteArray(Charsets.UTF_8)
    private val HKDF_INFO = "KYBER1024-X25519-HYBRID".toByteArray(Charsets.UTF_8)
    private const val OUTPUT_KEY_LEN = 32

    /**
     * Hybrid encapsulation:
     *   1. Kyber-1024 encapsulate → (kem_ct, kyber_ss)
     *   2. Generate ephemeral X25519 keypair
     *   3. X25519 DH with recipient's X25519 public key → x25519_ss
     *   4. HKDF(kyber_ss || x25519_ss) → combined key
     */
    fun encapsulate(
        recipientKyberPubKey: PublicKey,
        recipientX25519PubKey: PublicKey
    ): HybridEncapsulationResult {
        val kyberResult = KyberEngine.encapsulate(recipientKyberPubKey)
        val ephemeralX25519 = X25519Engine.generateKeyPair()

        val x25519SharedSecret = X25519Engine.deriveSharedSecret(
            ownPrivateKey = ephemeralX25519.private,
            peerPublicKey = recipientX25519PubKey
        )

        val combinedSecret = hkdfCombine(kyberResult.sharedSecret, x25519SharedSecret)

        kyberResult.wipeSecret()
        SecureWipe.wipe(x25519SharedSecret)
        SecureWipe.wipeKeyPair(ephemeralX25519)

        return HybridEncapsulationResult(
            kyberCiphertext = kyberResult.ciphertext,
            x25519EphemeralPubKey = ephemeralX25519.public.encoded,
            combinedSecret = combinedSecret
        )
    }

    /**
     * Hybrid decapsulation:
     *   1. Kyber-1024 decapsulate → kyber_ss
     *   2. X25519 DH with sender's ephemeral public key → x25519_ss
     *   3. HKDF(kyber_ss || x25519_ss) → combined key
     */
    fun decapsulate(
        kyberCiphertext: ByteArray,
        senderX25519EphPubKeyBytes: ByteArray,
        ownKyberPrivKey: PrivateKey,
        ownX25519PrivKey: PrivateKey
    ): ByteArray {
        val kyberSecret = KyberEngine.decapsulate(ownKyberPrivKey, kyberCiphertext)

        val x25519KeyFactory = java.security.KeyFactory.getInstance("X25519")
        val senderEphPubKey = x25519KeyFactory.generatePublic(
            java.security.spec.X509EncodedKeySpec(senderX25519EphPubKeyBytes)
        )

        val x25519Secret = X25519Engine.deriveSharedSecret(
            ownPrivateKey = ownX25519PrivKey,
            peerPublicKey = senderEphPubKey
        )

        val combinedSecret = hkdfCombine(kyberSecret, x25519Secret)
        SecureWipe.wipeAll(kyberSecret, x25519Secret)
        return combinedSecret
    }

    private fun hkdfCombine(kyberSecret: ByteArray, x25519Secret: ByteArray): ByteArray {
        val ikm = kyberSecret + x25519Secret
        val prk = hkdfExtract(HKDF_SALT, ikm)
        val okm = hkdfExpand(prk, HKDF_INFO, OUTPUT_KEY_LEN)
        SecureWipe.wipeAll(ikm, prk)
        return okm
    }

    private fun hkdfExtract(salt: ByteArray, ikm: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        return mac.doFinal(ikm)
    }

    private fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        require(length <= 32)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update(info)
        mac.update(0x01.toByte())
        return mac.doFinal().copyOf(length)
    }
}

data class HybridEncapsulationResult(
    val kyberCiphertext: ByteArray,
    val x25519EphemeralPubKey: ByteArray,
    val combinedSecret: ByteArray
) {
    fun wipeCombinedSecret() { combinedSecret.fill(0) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HybridEncapsulationResult) return false
        return kyberCiphertext.contentEquals(other.kyberCiphertext) &&
                x25519EphemeralPubKey.contentEquals(other.x25519EphemeralPubKey) &&
                combinedSecret.contentEquals(other.combinedSecret)
    }

    override fun hashCode(): Int {
        var result = kyberCiphertext.contentHashCode()
        result = 31 * result + x25519EphemeralPubKey.contentHashCode()
        result = 31 * result + combinedSecret.contentHashCode()
        return result
    }
}
