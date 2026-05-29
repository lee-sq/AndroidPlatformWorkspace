package com.holderzone.foundation.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseUrlManagerTest {
    @Test
    fun `initial base url is normalized`() {
        val manager = BaseUrlManager(" https://api.example.com ")

        assertEquals("https://api.example.com/", manager.getCurrentBaseUrl())
        assertTrue(manager.hasBaseUrl())
    }

    @Test
    fun `setBaseUrl normalizes missing slash`() {
        val manager = BaseUrlManager()

        manager.setBaseUrl("http://localhost:8080")

        assertEquals("http://localhost:8080/", manager.getCurrentBaseUrl())
    }

    @Test
    fun `invalid scheme is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            BaseUrlManager("ftp://api.example.com")
        }
    }

    @Test
    fun `clearBaseUrl removes current value`() {
        val manager = BaseUrlManager("https://api.example.com")

        manager.clearBaseUrl()

        assertNull(manager.getCurrentBaseUrl())
        assertFalse(manager.hasBaseUrl())
    }
}
