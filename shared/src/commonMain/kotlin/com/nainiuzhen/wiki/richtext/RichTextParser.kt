package com.nainiuzhen.wiki.richtext

import androidx.compose.ui.graphics.Color
import com.nainiuzhen.wiki.data.model.NewlineSpan
import com.nainiuzhen.wiki.data.model.RichSpan
import com.nainiuzhen.wiki.data.model.TextSpan

/**
 * 富文本解析器。
 *
 * 语法（标记隐藏，内容按颜色 / 字号渲染）：
 * - 单色：`/#RRGGBB#内容/#`
 * - 色 + 字号：`/#RRGGBB#SIZE#内容/#`（`SIZE` 为 sp 数值）
 * - 换行：原始串中的 `\n`（转义后的 `\\n`）
 *
 * 未知颜色 / 字号一律降级为默认（不崩溃）。
 */
object RichTextParser {
    /** 解析富文本原始串为 [RichSpan] 列表；空串返回空列表。 */
    fun parse(raw: String?): List<RichSpan> {
        if (raw.isNullOrEmpty()) return emptyList()
        val spans = mutableListOf<RichSpan>()
        var i = 0
        val len = raw.length
        while (i < len) {
            val marker = raw.indexOf("/#", i)
            if (marker < 0) {
                appendPlain(spans, raw.substring(i, len))
                break
            }
            if (marker > i) appendPlain(spans, raw.substring(i, marker))
            val parsed = tryParseMarker(raw, marker)
            if (parsed != null) {
                spans.add(parsed.span)
                i = parsed.next
            } else {
                spans.add(TextSpan("/#"))
                i = marker + 2
            }
        }
        return spans
    }

    private fun appendPlain(spans: MutableList<RichSpan>, text: String) {
        var from = 0
        while (true) {
            val nl = text.indexOf("\\n", from)
            if (nl < 0) {
                if (from < text.length) spans.add(TextSpan(text.substring(from, text.length)))
                break
            }
            if (nl > from) spans.add(TextSpan(text.substring(from, nl)))
            spans.add(NewlineSpan)
            from = nl + 2
        }
    }

    /**
     * 尝试从 [start]（`/#` 位置）解析一个富文本标记。
     * 成功返回 [SpanResult]，失败（非合法标记 / 未闭合）返回 null。
     */
    private fun tryParseMarker(raw: String, start: Int): SpanResult? {
        var idx = start + 2 // 跳过 "/#"
        if (idx + 6 > raw.length) return null
        val colorHex = raw.substring(idx, idx + 6)
        val color = runCatching { colorHex.toLong(radix = 16) }.getOrNull() ?: return null
        idx += 6
        if (idx >= raw.length || raw[idx] != '#') return null
        idx += 1 // 跳过分隔 '#'

        var fontSizeSp: Float? = null
        // 可选字号：连续数字后跟 '#'
        if (idx < raw.length && raw[idx].isDigit()) {
            val numEnd = raw.indexOf('#', idx)
            if (numEnd < 0) return null
            fontSizeSp = raw.substring(idx, numEnd).toFloatOrNull()
            idx = numEnd + 1
        }

        val end = raw.indexOf("/#", idx)
        if (end < 0) return null
        val content = raw.substring(idx, end)
        val textColor = Color(0xFF000000L or (color and 0xFFFFFFL))
        return SpanResult(TextSpan(content, textColor, fontSizeSp), end + 2)
    }

    private data class SpanResult(val span: RichSpan, val next: Int)
}
