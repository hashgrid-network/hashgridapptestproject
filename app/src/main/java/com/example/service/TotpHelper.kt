package com.example.service

import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Standard RFC 6238 Time-based One-Time Password (TOTP) algorithm implementation.
 * Compatible with Google Authenticator, Microsoft Authenticator, Authy, etc.
 */
object TotpHelper {

    private const val BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private const val TIME_STEP_SECONDS = 30L
    private const val DIGITS = 6

    /**
     * Generates a secure random Base32 secret key (16 characters / 80 bits).
     */
    fun generateSecret(length: Int = 16): String {
        val random = SecureRandom()
        val sb = java.lang.StringBuilder(length)
        for (i in 0 until length) {
            val idx = random.nextInt(BASE32_CHARS.length)
            sb.append(BASE32_CHARS[idx])
        }
        return sb.toString()
    }

    /**
     * Generates the current 6-digit TOTP code for the given secret key.
     */
    fun generateCurrentTotp(secret: String): String {
        val currentCounter = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS
        return generateTotpForCounter(secret, currentCounter)
    }

    /**
     * Verifies an input 6-digit OTP code against the secret key.
     * Checks current window and previous/next window (window = 1 => +/- 30s) to tolerate clock drift.
     */
    fun verifyTotp(inputCode: String, secret: String, window: Int = 1): Boolean {
        val cleanCode = inputCode.trim()
        if (cleanCode.length != DIGITS || cleanCode.any { !it.isDigit() }) {
            return false
        }
        val cleanSecret = secret.trim().uppercase().replace(" ", "").replace("-", "")
        if (cleanSecret.isBlank()) {
            return false
        }

        val currentCounter = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS
        for (i in -window..window) {
            val expectedCode = generateTotpForCounter(cleanSecret, currentCounter + i)
            if (cleanCode == expectedCode) {
                return true
            }
        }
        return false
    }

    /**
     * Generates the standard otpauth:// URI for QR codes and deep links into Google Authenticator.
     */
    fun getOtpAuthUri(email: String, secret: String, issuer: String = "HashGrid"): String {
        val cleanSecret = secret.trim().uppercase().replace(" ", "").replace("-", "")
        val encodedIssuer = URLEncoder.encode(issuer, "UTF-8").replace("+", "%20")
        val encodedEmail = URLEncoder.encode(email, "UTF-8").replace("+", "%20")
        return "otpauth://totp/$encodedIssuer:$encodedEmail?secret=$cleanSecret&issuer=$encodedIssuer&algorithm=SHA1&digits=$DIGITS&period=$TIME_STEP_SECONDS"
    }

    /**
     * Generates a 6-digit OTP for a specific counter step.
     */
    fun generateTotpForCounter(secret: String, counter: Long): String {
        return try {
            val keyBytes = decodeBase32(secret)
            val data = ByteArray(8)
            var v = counter
            for (i in 7 downTo 0) {
                data[i] = (v and 0xFF).toByte()
                v = v shr 8
            }
            val mac = Mac.getInstance("HmacSHA1")
            val keySpec = SecretKeySpec(keyBytes, "HmacSHA1")
            mac.init(keySpec)
            val hash = mac.doFinal(data)

            val offset = (hash[hash.size - 1].toInt() and 0x0F)
            val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                    ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hash[offset + 3].toInt() and 0xFF)

            val otp = binary % 1000000
            String.format(Locale.US, "%06d", otp)
        } catch (_: Exception) {
            "000000"
        }
    }

    /**
     * Decodes a Base32 string into a byte array.
     */
    fun decodeBase32(base32: String): ByteArray {
        val clean = base32.uppercase().replace("=", "").replace(" ", "").replace("-", "").trim()
        var buffer = 0
        var bitsLeft = 0
        val out = ByteArrayOutputStream()

        for (c in clean) {
            val valIndex = BASE32_CHARS.indexOf(c)
            if (valIndex < 0) continue
            buffer = (buffer shl 5) or valIndex
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                out.write((buffer shr bitsLeft) and 0xFF)
            }
        }
        return out.toByteArray()
    }
}
