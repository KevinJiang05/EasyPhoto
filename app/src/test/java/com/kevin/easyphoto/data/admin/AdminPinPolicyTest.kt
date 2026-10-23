package com.kevin.easyphoto.data.admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminPinPolicyTest {
    @Test
    fun acceptsExactlySixAsciiDigitsIncludingLeadingZero() {
        assertTrue(AdminPinPolicy.isValid("012345"))
        listOf("", "12345", "1234567", "12345a", "１２３４５６", " 123456").forEach {
            assertFalse(AdminPinPolicy.isValid(it))
        }
    }

    @Test
    fun onlyTheConfiguredPinMatchesItsSavedHash() {
        val salt = AdminPinPolicy.createSalt()
        val hash = AdminPinPolicy.hash("012345", salt)

        assertTrue(AdminPinPolicy.matches("012345", salt, hash))
        assertFalse(AdminPinPolicy.matches("012346", salt, hash))
        assertFalse(AdminPinPolicy.matches("12345", salt, hash))
    }

    @Test
    fun differentDevicesDoNotShareTheSameHashForTheSamePin() {
        val firstSalt = AdminPinPolicy.createSalt()
        val secondSalt = AdminPinPolicy.createSalt()

        assertNotEquals(firstSalt, secondSalt)
        assertNotEquals(AdminPinPolicy.hash("012345", firstSalt), AdminPinPolicy.hash("012345", secondSalt))
    }
}
