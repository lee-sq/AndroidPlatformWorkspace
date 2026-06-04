package com.holderzone.device.driver.cabinet.print

import com.holderzone.device.api.cabinet.model.PrintContent
import java.nio.charset.Charset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TsplLabelRendererTest {
    private val charset = Charset.forName("GB18030")

    @Test
    fun renderUsesDriverDefaultPaperSizeWhenContentSizeIsEmpty() {
        val command = TsplLabelRenderer.render(
            content = PrintContent(title = "测试"),
            defaultWidthMm = 50,
            defaultHeightMm = 50,
        ).toString(charset)

        assertTrue(command.contains("SIZE 50 mm,50 mm\r\n"))
    }

    @Test
    fun renderUsesContentPaperSizeBeforeDriverDefault() {
        val command = TsplLabelRenderer.render(
            content = PrintContent(
                title = "测试",
                widthMm = 40,
                heightMm = 30,
            ),
            defaultWidthMm = 60,
            defaultHeightMm = 40,
        ).toString(charset)

        assertTrue(command.contains("SIZE 40 mm,30 mm\r\n"))
    }

    @Test
    fun renderStarLabelUsesVendorLabelCommand() {
        val label = TsplLabelRenderer.renderStarLabel(
            content = PrintContent(
                title = "测试",
                lines = listOf("设备：Star"),
            ),
            defaultWidthMm = 50,
            defaultHeightMm = 50,
        )
        val command = ByteArray(label.command.size) { index -> label.command[index] }

        assertTrue(command.isNotEmpty())
        assertFalse(command.toString(charset).contains("SIZE 50 mm,50 mm"))
    }
}
