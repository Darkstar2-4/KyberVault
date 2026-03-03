package com.kybervault.security

import android.util.Base64
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * TOTP (Time-based One-Time Password) implementation per RFC 6238.
 * Compatible with Google Authenticator, Authy, Microsoft Authenticator, etc.
 *
 * Uses HMAC-SHA1, 6-digit codes, 30-second step — the universal standard.
 */
object TotpEngine {

    private const val DIGITS = 6
    private const val PERIOD_SECONDS = 30L
    private const val SECRET_BYTES = 20 // 160-bit secret
    private const val ALGORITHM = "HmacSHA1"

    /** Generate a new random TOTP secret (160-bit). */
    fun generateSecret(): ByteArray {
        val secret = ByteArray(SECRET_BYTES)
        SecureRandom().nextBytes(secret)
        return secret
    }

    /** Encode secret as Base32 for QR code / manual entry (no padding). */
    fun toBase32(secret: ByteArray): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val sb = StringBuilder()
        var buffer = 0
        var bitsLeft = 0
        for (byte in secret) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                bitsLeft -= 5
                sb.append(alphabet[(buffer shr bitsLeft) and 0x1F])
            }
        }
        if (bitsLeft > 0) {
            sb.append(alphabet[(buffer shl (5 - bitsLeft)) and 0x1F])
        }
        return sb.toString()
    }

    /**
     * Verify a TOTP code with ±1 window tolerance (covers clock skew).
     * @return true if code matches current, previous, or next period
     */
    fun verifyCode(secret: ByteArray, code: String, timeMillis: Long = System.currentTimeMillis()): Boolean {
        val counter = timeMillis / 1000L / PERIOD_SECONDS
        for (offset in -1L..1L) {
            if (generateHotp(secret, counter + offset) == code.padStart(DIGITS, '0')) {
                return true
            }
        }
        return false
    }

    /** Seconds remaining in the current TOTP period. */
    fun secondsRemaining(timeMillis: Long = System.currentTimeMillis()): Int {
        return (PERIOD_SECONDS - ((timeMillis / 1000L) % PERIOD_SECONDS)).toInt()
    }

    /**
     * Build an otpauth:// URI for QR code scanning.
     * Format: otpauth://totp/KyberVault?secret=BASE32&issuer=KyberVault&algorithm=SHA1&digits=6&period=30
     */
    fun buildOtpAuthUri(secret: ByteArray, accountName: String = "KyberVault"): String {
        val base32 = toBase32(secret)
        return "otpauth://totp/KyberVault:${accountName}?secret=${base32}&issuer=KyberVault&algorithm=SHA1&digits=${DIGITS}&period=${PERIOD_SECONDS}"
    }

    /** Store secret as Base64 for SharedPreferences. */
    fun secretToStorage(secret: ByteArray): String = Base64.encodeToString(secret, Base64.NO_WRAP)

    /** Restore secret from Base64 storage. */
    fun secretFromStorage(stored: String): ByteArray = Base64.decode(stored, Base64.NO_WRAP)

    // ── HOTP core (RFC 4226) ──

    private fun generateHotp(secret: ByteArray, counter: Long): String {
        val msg = ByteBuffer.allocate(8).putLong(counter).array()
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(SecretKeySpec(secret, ALGORITHM))
        val hash = mac.doFinal(msg)

        // Dynamic truncation
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val otp = binary % pow10(DIGITS)
        return otp.toString().padStart(DIGITS, '0')
    }

    private fun pow10(n: Int): Int {
        var result = 1
        repeat(n) { result *= 10 }
        return result
    }
}
