package com.holderzone.device.driver.scale.ly.protocol

import com.holderzone.device.api.base.model.Frame
import com.holderzone.device.api.base.model.ProbeResult
import com.holderzone.device.api.scale.model.WeightUnit
import com.holderzone.device.core.channel.FrameStreamReader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyScaleProtocolTest {
    @Test
    fun parseFrameUsesLegacyAsciiOffsets() {
        val frame = LyScaleProtocol.buildFrameForTest(
            tareWeightKg = 1.2,
            netWeightKg = 11.34,
            flag = "2",
        )

        val reading = LyScaleProtocol.parseFrame(frame)!!

        assertEquals(11.34, reading.result.value, 0.0)
        assertEquals(WeightUnit.KILOGRAM, reading.result.unit)
        assertEquals(11_340.0, reading.result.grams, 0.0)
        assertEquals(1.2, reading.tareWeight, 0.0)
        assertTrue(reading.result.stable)
        assertTrue(reading.netMode)
    }

    @Test
    fun parseFrameRejectsInvalidShape() {
        assertNull(LyScaleProtocol.parseFrame("=bad".encodeToByteArray()))
        assertNull(LyScaleProtocol.parseFrame(ByteArray(LyScaleProtocol.FRAME_LENGTH) { 0x30 }))
    }

    @Test
    fun parserHandlesNoiseHalfPacketAndStickyPacket() = runTest {
        val parser = LyScaleFrameParser()
        val reader = FrameStreamReader(parser)
        val firstFrame = LyScaleProtocol.buildFrameForTest(
            tareWeightKg = 0.0,
            netWeightKg = 2.5,
            flag = "7",
        )
        val secondFrame = LyScaleProtocol.buildFrameForTest(
            tareWeightKg = 0.3,
            netWeightKg = -1.25,
            flag = "0",
        )
        val noisyFirstChunk = "NOISE".encodeToByteArray() + firstFrame.copyOfRange(0, 10)

        assertEquals(emptyList<Frame>(), reader.append(noisyFirstChunk))

        val frames = reader.append(firstFrame.copyOfRange(10, firstFrame.size) + secondFrame)

        assertEquals(2, frames.size)
        assertEquals(firstFrame.toList(), frames[0].payload.toList())
        assertEquals(secondFrame.toList(), frames[1].payload.toList())
        assertTrue(frames[0].raw.size > frames[0].payload.size)

        parser.parseFrame(frames[0])
        assertEquals(2.5, parser.observeWeight().first().value, 0.0)
    }

    @Test
    fun probeAcceptsReadableLyFrame() {
        val probe = LyScaleProbeStrategy()
        val response = LyScaleProtocol.buildFrameForTest(
            tareWeightKg = 0.0,
            netWeightKg = 3.21,
            flag = "3",
        )

        val result = probe.validateResponse(response)

        assertTrue(result is ProbeResult.Matched)
    }

    @Test
    fun probeRejectsUnreadableFrame() {
        val probe = LyScaleProbeStrategy()

        val result = probe.validateResponse("hello".encodeToByteArray())

        assertTrue(result is ProbeResult.Mismatched)
    }
}
