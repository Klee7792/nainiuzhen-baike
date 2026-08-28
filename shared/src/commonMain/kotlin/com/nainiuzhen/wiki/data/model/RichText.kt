package com.nainiuzhen.wiki.data.model

import androidx.compose.ui.graphics.Color

/**
 * 富文本节点（sealed）。
 *
 * - [TextSpan]：一段带可选颜色 / 字号（sp）的文本。
 * - [NewlineSpan]：一个换行标记（对应原始串中的 `\n`）。
 */
sealed interface RichSpan

/**
 * 带样式的文本片段。
 *
 * @param text 文本内容（标记已隐藏）。
 * @param color 自定义颜色；为 null 时使用默认文本色。
 * @param fontSizeSp 自定义字号（sp）；为 null 时使用默认字号。
 */
data class TextSpan(
    val text: String,
    val color: Color? = null,
    val fontSizeSp: Float? = null,
) : RichSpan

/** 换行标记。 */
data object NewlineSpan : RichSpan
