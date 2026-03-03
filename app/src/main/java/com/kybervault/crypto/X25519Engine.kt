package com.kybervault.crypto

import com.kybervault.util.SecureWipe
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.KeyAgreement

/**
 * X25519 Elliptic Curve Diffie-Hellman engine.
 * Uses Android's built-in XDH provider (API 31+).
 */
object X25519Engine {

    fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("X25519")
        return kpg.generateKeyPair()
    }

    fun deriveSharedSecret(ownPrivateKey: PrivateKey, peerPublicKey: PublicKey): ByteArray {
        val keyAgreement = KeyAgreement.getInstance("XDH")
        keyAgreement.init(ownPrivateKey)
        keyAgreement.doPhase(peerPublicKey, true)
        return keyAgreement.generateSecret()
    }
}

/**
 * Container for hybrid key material: Kyber-1024 + X25519 keypairs.
 * Both are standard JCA KeyPair objects.
 */
data class HybridKeyBundle(
    val kyberKeyPair: KeyPair,
    val x25519KeyPair: KeyPair,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun wipe() {
        SecureWipe.wipeKeyPair(kyberKeyPair)
        SecureWipe.wipeKeyPair(x25519KeyPair)
    }
}