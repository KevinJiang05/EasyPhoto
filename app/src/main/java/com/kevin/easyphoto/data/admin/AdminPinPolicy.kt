package com.kevin.easyphoto.data.admin

import java.security.MessageDigest
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object AdminPinPolicy {
    fun isValid(pin: String): Boolean = pin.length == 6 && pin.all { it in '0'..'9' }

    fun createSalt(): String = UUID.randomUUID().toString()

    fun hash(pin: String, salt: String): String {
        require(isValid(pin))
        val specification = PBEKeySpec(pin.toCharArray(), salt.toByteArray(Charsets.UTF_8), 120_000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(specification)
                .encoded
                .joinToString("") { "%02x".format(it) }
        } finally {
            specification.clearPassword()
        }
    }

    fun matches(pin: String, salt: String, expectedHash: String): Boolean =
        isValid(pin) && MessageDigest.isEqual(
            hash(pin, salt).toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8),
        )
}
