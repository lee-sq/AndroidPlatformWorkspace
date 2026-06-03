package com.holderzone.device.driver.scale.jw.protocol

import com.holderzone.device.api.base.model.Frame
import com.holderzone.device.api.scale.model.WeightUnit
import com.holderzone.device.core.channel.FrameStreamReader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JWScale3568ProtocolTest {
    @Test
    fun parseWeightFrameUses3568OffsetsAndScale() {
        val frame = JWScale3568Protocol.hexToBytes(WEIGHT_FRAME_HEX)

        val reading = JWScale3568Protocol.parseWeight(frame)!!

        assertEquals(123456L, reading.adValue)
        assertEquals(120000L, reading.zeroAdValue)
        assertEquals(24.5, reading.batteryVoltage, 0.0)
        assertEquals(23, reading.temperatureCelsius)
        assertEquals(18200L, reading.coefficient)
        assertEquals(12.3, reading.grossWeight, 0.0)
        assertEquals(1.2, reading.tareWeight, 0.0)
        assertEquals(11.1, reading.result.value, 0.0)
        assertEquals(WeightUnit.KILOGRAM, reading.result.unit)
        assertEquals(11_100.0, reading.result.grams, 0.0)
        assertTrue(reading.result.stable)
    }

    @Test
    fun parseWeightFrameHandlesNegativeSign() {
        val frame = JWScale3568Protocol.hexToBytes(NEGATIVE_WEIGHT_FRAME_HEX)

        val reading = JWScale3568Protocol.parseWeight(frame)!!

        assertEquals(-11.1, reading.result.value, 0.0)
    }

    @Test
    fun invalidCrcDoesNotParse() {
        val frame = JWScale3568Protocol.hexToBytes(WEIGHT_FRAME_HEX).also {
            it[it.lastIndex] = 0x00
        }

        assertNull(JWScale3568Protocol.parseWeight(frame))
    }

    @Test
    fun controlCommandsMatchDemoProtocolShape() {
        assertEquals("01030200000705B0", JWScale3568Protocol.bytesToHex(JWScale3568Protocol.readVersionCommand))
        assertEquals("01030100001045FA", JWScale3568Protocol.bytesToHex(JWScale3568Protocol.readParameterCommand))
        assertEquals("0110000000020400010000A26F", JWScale3568Protocol.bytesToHex(JWScale3568Protocol.buildZeroCommand()))
        assertEquals("0110000000020400000001326F", JWScale3568Protocol.bytesToHex(JWScale3568Protocol.buildTareCommand()))
    }

    @Test
    fun parserHandlesNoiseHalfPacketAndStickyPacket() = runTest {
        val parser = JWScale3568FrameParser()
        val reader = FrameStreamReader(parser)
        val firstFrame = JWScale3568Protocol.hexToBytes(WEIGHT_FRAME_HEX)
        val secondFrame = JWScale3568Protocol.hexToBytes(NEGATIVE_WEIGHT_FRAME_HEX)
        val noisyFirstChunk = byteArrayOf(0x55, 0x66) + firstFrame.copyOfRange(0, 10)

        assertEquals(emptyList<Frame>(), reader.append(noisyFirstChunk))

        val frames = reader.append(firstFrame.copyOfRange(10, firstFrame.size) + secondFrame)

        assertEquals(2, frames.size)
        assertEquals(firstFrame.toList(), frames[0].payload.toList())
        assertEquals(secondFrame.toList(), frames[1].payload.toList())
        assertTrue(frames[0].raw.size > frames[0].payload.size)

        parser.parseFrame(frames[0])
        assertEquals(11.1, parser.observeWeight().first().value, 0.0)
    }

    @Test
    fun probeAcceptsKnown3568Frames() {
        val probe = JWScale3568ProbeStrategy()

        val result = probe.validateResponse(JWScale3568Protocol.hexToBytes(VERSION_FRAME_HEX))

        assertTrue(result is com.holderzone.device.api.base.model.ProbeResult.Matched)
    }

    @Test
    fun probeRejectsInvalidFrames() {
        val probe = JWScale3568ProbeStrategy()

        val result = probe.validateResponse(byteArrayOf(0x01, 0x03, 0x18))

        assertTrue(result is com.holderzone.device.api.base.model.ProbeResult.Mismatched)
    }

    companion object {
        private const val WEIGHT_FRAME_HEX =
            "0103180001E2400001D4C0099204D20000003F00004718007B0001D44A"
        private const val NEGATIVE_WEIGHT_FRAME_HEX =
            "0103180001E2400001D4C0099204D20001003F00004718007B0001D0B6"
        private const val VERSION_FRAME_HEX =
            "01030E4A57333536382D56372E302E300074ED"
    }
}
