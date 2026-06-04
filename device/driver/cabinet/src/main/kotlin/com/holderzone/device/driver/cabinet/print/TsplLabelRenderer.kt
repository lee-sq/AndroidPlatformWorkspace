package com.holderzone.device.driver.cabinet.print

import com.holderzone.device.api.cabinet.model.PrintContent
import com.kongqw.serialportlibrary.command.Label
import java.nio.charset.Charset
import java.util.Vector

/**
 * Auth：ligen26
 * CrateTime：2026/6/3 18:18
 * Description: 轻量 TSPL 标签渲染器，把通用 PrintContent 转成串口打印命令。
 */
object TsplLabelRenderer {
    private val chineseCharset: Charset = Charset.forName("GB18030")

    fun render(
        content: PrintContent,
        defaultWidthMm: Int,
        defaultHeightMm: Int,
    ): ByteArray {
        val widthMm = content.widthMm?.takeIf { it > 0 } ?: defaultWidthMm
        val heightMm = content.heightMm?.takeIf { it > 0 } ?: defaultHeightMm
        val command = StringBuilder()
        command.append("SIZE ")
        command.append(widthMm)
        command.append(" mm,")
        command.append(heightMm)
        command.append(" mm\r\n")
        command.append("GAP 3 mm,0 mm\r\n")
        command.append("DIRECTION 0,0\r\n")
        command.append("REFERENCE 0,0\r\n")
        command.append("SET TEAR ON\r\n")
        command.append("CLS\r\n")

        var y = 20
        if (content.title.isNotBlank()) {
            command.appendText(
                x = 48,
                y = y,
                xScale = 2,
                yScale = 1,
                text = content.title,
            )
            y += 38
        }

        content.lines.forEach { line ->
            command.appendText(
                x = 10,
                y = y,
                xScale = 1,
                yScale = 1,
                text = line,
            )
            y += 38
        }

        command.append("PRINT 1,1\r\n")
        return command.toString().toByteArray(chineseCharset)
    }

    fun renderStarLabel(
        content: PrintContent,
        defaultWidthMm: Int,
        defaultHeightMm: Int,
    ): Label {
        val width = (content.widthMm?.takeIf { it > 0 } ?: defaultWidthMm) * DOTS_PER_MM
        val height = (content.heightMm?.takeIf { it > 0 } ?: defaultHeightMm) * DOTS_PER_MM
        val label = Label().apply {
            reset()
            clear()
            switchLabel()
            customPageStart(width, height, 0)
        }

        var y = 20
        content.title.takeIf(String::isNotBlank)?.let { title ->
            label.customPrintText(
                title,
                ((width - TITLE_DOT_WIDTH * title.length) / 2).toInt(),
                y,
                24,
                1,
                0,
                0,
                0,
                STAR_FONT_TYPE,
                0,
                0,
            )
            y += 38
        }

        content.lines.forEach { line ->
            label.customPrintText(line, 10, y, 24, 0, 0, 0, 0, STAR_FONT_TYPE, 0, 0)
            y += 38
        }

        label.pageEnd()
        label.customPrintPage(1)
        return label
    }

    fun Label.toByteArray(): ByteArray {
        return command.toByteArray()
    }

    private fun StringBuilder.appendText(
        x: Int,
        y: Int,
        xScale: Int,
        yScale: Int,
        text: String,
    ) {
        append("TEXT ")
        append(x)
        append(',')
        append(y)
        append(",\"TSS24.BF2\",0,")
        append(xScale)
        append(',')
        append(yScale)
        append(",\"")
        append(text.escapeTsplText())
        append("\"\r\n")
    }

    private fun String.escapeTsplText(): String {
        return replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private fun Vector<Byte>.toByteArray(): ByteArray {
        val bytes = ByteArray(size)
        forEachIndexed { index, value ->
            bytes[index] = value
        }
        return bytes
    }

    private const val DOTS_PER_MM = 8
    private const val TITLE_DOT_WIDTH = 36.0
    private const val STAR_FONT_TYPE = 9
}
