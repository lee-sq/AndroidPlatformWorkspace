package com.holderzone.device.driver.cabinet.jw.serial.protocol

import com.holderzone.device.api.base.model.Frame
import com.holderzone.device.api.cabinet.model.DoorState
import com.holderzone.device.api.scale.model.WeightUnit
import com.holderzone.device.core.channel.FrameStreamReader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JwCabinetProtocolTest {
    @Test
    fun controlCommandsMatchLegacySerialProtocol() {
        assertEquals("020300000012C5F4", JwCabinetProtocol.bytesToHex(JwCabinetProtocol.buildReadWeightCommand()))
        assertEquals("100300000009868D", JwCabinetProtocol.bytesToHex(JwCabinetProtocol.buildReadDoorCommand()))
        assertEquals("02100002000102AAAA4D9D", JwCabinetProtocol.bytesToHex(JwCabinetProtocol.zeroCommand))
        assertEquals("02100005000102AAAA4C2A", JwCabinetProtocol.bytesToHex(JwCabinetProtocol.lightOnCommand))
        assertEquals("021000050001020000B2F5", JwCabinetProtocol.bytesToHex(JwCabinetProtocol.lightOffCommand))
    }

    @Test
    fun parseWeightFrameUsesLegacyOffsetsAndSlope() {
        val frame = JwCabinetProtocol.hexToBytes(WEIGHT_FRAME_HEX)

        val reading = JwCabinetProtocol.parseWeightFrame(frame, slope = 2.0)!!

        assertEquals(6_000.0, reading.result.value, 0.0)
        assertEquals(WeightUnit.GRAM, reading.result.unit)
        assertEquals(6_000.0, reading.result.grams, 0.0)
        assertEquals(3_000.0, reading.rawGrams, 0.0)
        assertEquals(2_000.0, reading.zeroOffsetGrams, 0.0)
        assertTrue(reading.outsideDoorOpen)
        assertTrue(reading.outsideLightOpen)
    }

    @Test
    fun parseDoorTemperatureFrameUsesZeroBasedDoorIndexes() {
        val frame = JwCabinetProtocol.hexToBytes(DOOR_FRAME_HEX)

        val reading = JwCabinetProtocol.parseDoorTemperatureFrame(frame, doorCount = 8)!!

        assertEquals(26.0, reading.temperature.celsius, 0.0)
        assertEquals(DoorState.OPEN, reading.doorStates[0])
        assertEquals(DoorState.CLOSED, reading.doorStates[1])
        assertEquals(DoorState.CLOSED, reading.doorStates[2])
        assertEquals(DoorState.OPEN, reading.doorStates[3])
    }

    @Test
    fun invalidCrcDoesNotParse() {
        val frame = JwCabinetProtocol.hexToBytes(WEIGHT_FRAME_HEX).also {
            it[it.lastIndex] = 0x00
        }

        assertNull(JwCabinetProtocol.parseWeightFrame(frame, slope = 1.0))
    }

    @Test
    fun parserHandlesNoiseHalfPacketAndStickyPacket() = runTest {
        val parser = JwCabinetFrameParser()
        val reader = FrameStreamReader(parser)
        val firstFrame = JwCabinetProtocol.hexToBytes(WEIGHT_FRAME_HEX)
        val secondFrame = JwCabinetProtocol.hexToBytes(DOOR_FRAME_HEX)
        val noisyFirstChunk = byteArrayOf(0x55, 0x66) + firstFrame.copyOfRange(0, 8)

        assertEquals(emptyList<Frame>(), reader.append(noisyFirstChunk))

        val frames = reader.append(firstFrame.copyOfRange(8, firstFrame.size) + secondFrame)

        assertEquals(2, frames.size)
        assertEquals(firstFrame.toList(), frames[0].payload.toList())
        assertEquals(secondFrame.toList(), frames[1].payload.toList())
        assertTrue(frames[0].raw.size > frames[0].payload.size)

        parser.parseFrame(frames[0])
        assertEquals(3_000.0, parser.observeWeight().first().value, 0.0)
        parser.parseFrame(frames[1])
        assertEquals(26.0, parser.observeTemperature().first().celsius, 0.0)
    }

    companion object {
        private const val WEIGHT_FRAME_HEX = "020312000000000000000200010005000100000000AF59"
        private const val DOOR_FRAME_HEX = "100312004200004269000000004269000000000000C2B2"
    }
}
