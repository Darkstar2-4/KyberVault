package com.kybervault.util

import java.security.KeyPair
import java.security.SecureRandom

/**
 * Utility for securely wiping sensitive data from memory.
 *
 * IMPORTANT CAVEATS:
 * - JVM garbage collection may have already copied byte arrays before wiping.
 * - The JIT compiler may optimize away fill operations on "unused" arrays.
 * - This is a best-effort defense-in-depth measure, not a guarantee.
 * - For true L2/RAM-only isolation, consider Android's hardware-backed Keystore
 *   or StrongBox (Titan M2 on Pixel 9 Pro) for the most sensitive operations.
 *
 * We mitigate JIT optimization by using volatile writes and verification.
 */
object SecureWipe {

    /**
     * Overwrite a byte array with zeros, then random data, then zeros again.
     * Three-pass wipe reduces risk of data remanence in DRAM.
     */
    fun wipe(data: ByteArray?) {
        if (data == null || data.isEmpty()) return

        // Pass 1: Zero fill
        for (i in data.indices) {
            data[i] = 0x00
        }

        // Pass 2: Random overwrite (defeats simple memory snapshot)
        val random = SecureRandom()
        random.nextBytes(data)

        // Pass 3: Final zero fill
        for (i in data.indices) {
            data[i] = 0x00
        }

        // Verification read to prevent JIT from optimizing away the writes.
        // The JIT cannot prove this value is unused since we pass it to a
        // volatile sink.
        @Suppress("UNUSED_VARIABLE")
        var sink = 0
        for (b in data) {
            sink = sink or b.toInt()
        }
        volatileSink = sink
    }

    /**
     * Wipe multiple byte arrays at once.
     */
    fun wipeAll(vararg arrays: ByteArray?) {
        arrays.forEach { wipe(it) }
    }

    /**
     * Attempt to wipe key material from a KeyPair.
     * Note: Java's key objects may not fully support wiping their internal state,
     * but we clear what we can access.
     */
    fun wipeKeyPair(keyPair: KeyPair?) {
        if (keyPair == null) return
        try {
            wipe(keyPair.private?.encoded)
            wipe(keyPair.public?.encoded)
        } catch (_: Exception) {
            // Some key implementations may not support encoded form
        }
    }

    // Volatile field to prevent JIT optimization of wipe operations
    @Volatile
    @JvmStatic
    private var volatileSink: Int = 0
}
