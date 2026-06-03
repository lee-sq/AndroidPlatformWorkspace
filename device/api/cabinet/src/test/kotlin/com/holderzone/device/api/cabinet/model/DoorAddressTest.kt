package com.holderzone.device.api.cabinet.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DoorAddressTest {
    @Test
    fun primaryDoorUsesSequentialIndex() {
        val address = DoorAddress.primary(index = 1)

        assertEquals(1, address.primaryIndex)
        assertEquals(null, address.compartmentIndex)
        assertFalse(address.isCompartment)
    }

    @Test
    fun compartmentDoorUsesPrimaryAndCompartmentIndexes() {
        val address = DoorAddress.compartment(
            primaryIndex = 1,
            compartmentIndex = 3,
        )

        assertEquals(1, address.primaryIndex)
        assertEquals(3, address.compartmentIndex)
        assertTrue(address.isCompartment)
    }

    @Test
    fun indexesMustNotBeNegative() {
        assertThrows(IllegalArgumentException::class.java) {
            DoorAddress.primary(index = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DoorAddress.compartment(primaryIndex = 0, compartmentIndex = -1)
        }
    }
}
