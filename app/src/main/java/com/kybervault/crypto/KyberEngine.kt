package com.kybervault.crypto

import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider
import org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec
import java.security.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Kyber-1024 KEM engine.
 *
 * Key generation: KeyPairGenerator.getInstance("Kyber", pqcProvider) — works on Android.
 * KEM: Cipher.wrap/unwrap instead of KeyGenerator — Android intercepts KeyGenerator
 *      but NOT Cipher for unknown algorithms like Kyber.
 *
 * Flow:
 *   Encapsulate: generate random AES-256 key → Cipher.wrap with recipient's public key
 *   Decapsulate: Cipher.unwrap with private key → recover AES-256 key
 *   Both sides end up with the same 32-byte key.
 */
object KyberEngine {

    private val pqcProvider = BouncyCastlePQCProvider()

    fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("Kyber", pqcProvider)
        kpg.initialize(KyberParameterSpec.kyber1024, SecureRandom())
        return kpg.generateKeyPair()
    }

    /**
     * Encapsulate: wrap a random AES-256 key using Kyber KEM.
     * Returns the wrapped ciphertext and the raw key (shared secret).
     */
    fun encapsulate(publicKey: PublicKey): EncapsulationResult {
        // Generate random 32-byte session key
        val rawKey = ByteArray(32)
        SecureRandom().nextBytes(rawKey)
        val aesKey = SecretKeySpec(rawKey, "AES")

        // Kyber Cipher.wrap internally does KEM encapsulation + key wrapping
        val cipher = Cipher.getInstance("Kyber", pqcProvider)
        cipher.init(Cipher.WRAP_MODE, publicKey, SecureRandom())
        val wrappedBytes = cipher.wrap(aesKey)

        return EncapsulationResult(
            ciphertext = wrappedBytes,
            sharedSecret = rawKey
        )
    }

    /**
     * Decapsulate: unwrap to recover the AES-256 key.
     */
    fun decapsulate(privateKey: PrivateKey, ciphertext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("Kyber", pqcProvider)
        cipher.init(Cipher.UNWRAP_MODE, privateKey)
        val unwrapped = cipher.unwrap(ciphertext, "AES", Cipher.SECRET_KEY)
        return unwrapped.encoded.copyOf()
    }
}

data class EncapsulationResult(
    val ciphertext: ByteArray,
    val sharedSecret: ByteArray
) {
    fun wipeSecret() { sharedSecret.fill(0) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncapsulationResult) return false
        return ciphertext.contentEquals(other.ciphertext) &&
                sharedSecret.contentEquals(other.sharedSecret)
    }
    override fun hashCode(): Int =
        31 * ciphertext.contentHashCode() + sharedSecret.contentHashCode()
}
