package com.walyo.browser.util

import java.security.MessageDigest
import java.security.SecureRandom

/** Salted SHA-256 for the app-lock PIN. Stored as "saltHex:hashHex". */
object PinHasher {
    const val PIN_LENGTH = 4
    private const val SALT_BYTES = 16

    fun hash(pin: String, salt: ByteArray = randomSalt()): String =
        salt.toHex() + ":" + digest(salt, pin).toHex()

    fun verify(pin: String, stored: String?): Boolean {
        val parts = stored?.split(':') ?: return false
        if (parts.size != 2) return false
        val salt = parts[0].hexToBytesOrNull() ?: return false
        val expected = parts[1].hexToBytesOrNull() ?: return false
        return MessageDigest.isEqual(digest(salt, pin), expected)
    }

    private fun digest(salt: ByteArray, pin: String): ByteArray =
        MessageDigest.getInstance("SHA-256").run {
            update(salt)
            digest(pin.toByteArray(Charsets.UTF_8))
        }

    private fun randomSalt(): ByteArray = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytesOrNull(): ByteArray? {
        if (length % 2 != 0) return null
        return try {
            ByteArray(length / 2) { substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        } catch (e: NumberFormatException) {
            null
        }
    }
}
