package com.nainiuzhen.wiki.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nainiuzhen.wiki.data.model.NewlineSpan
import com.nainiuzhen.wiki.data.model.TextSpan
import com.nainiuzhen.wiki.richtext.RichTextParser
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 富文本渲染组件：解析 [raw] 中的 `/#RRGGBB#内容/#`、`/#RRGGBB#SIZE#内容/#`、`\n`，
 * 转换为带颜色 / 字号的 [AnnotatedString] 并用 [BasicText] 渲染。
 *
 * @param raw 富文本原始串（可能为 null，此时渲染空）。
 * @param defaultColor 默认文本色（无颜色标记时生效）。
 * @param defaultFontSize 默认字号（无字号标记时生效）。
 * @param textAlign 文本对齐。
 */
@Composable
fun RichText(
    raw: String?,
    modifier: Modifier = Modifier,
    defaultColor: Color = MiuixTheme.colorScheme.onSurface,
    defaultFontSize: TextUnit = MiuixTheme.textStyles.body2.fontSize,
    textAlign: TextAlign = TextAlign.Start,
) {
    val annotated: AnnotatedString = remember(raw, defaultColor, defaultFontSize, textAlign) {
        buildAnnotatedString {
            RichTextParser.parse(raw).forEach { span ->
                when (span) {
                    is TextSpan -> {
                        val start = length
                        append(span.text)
                        addStyle(
                            style = SpanStyle(
                                color = span.color ?: defaultColor,
                                fontSize = (span.fontSizeSp?.sp) ?: defaultFontSize,
                            ),
                            start = start,
                            end = length,
                        )
                    }
                    is NewlineSpan -> append("\n")
                }
            }
        }
    }
    BasicText(text = annotated, modifier = modifier, style = TextStyle(textAlign = textAlign))
}

/**
 * 富文本描述区：在固定高度（约 [maxLines] 行）内显示，超出则内部滚动。
 * 不再提供「展开 / 收起」按钮——避免 `verticalScroll` 在 `OverlayDialog` 的 `Infinity`
 * 最大高度约束下崩溃（`IllegalStateException: scrollable measured with infinity max height`）。
 *
 * 关键：根容器用 `heightIn(max = 有限值)` 把约束收敛为有限，嵌套的 `verticalScroll` 才能正常测量。
 *
 * @param raw 富文本原始串（可为 null）。
 * @param modifier 外层修饰。
 * @param maxLines 最多显示的行数（高度上限），超出滚动（默认 8）。
 */
@Composable
fun ExpandableRichText(
    raw: String?,
    modifier: Modifier = Modifier,
    maxLines: Int = 8,
) {
    val density = LocalDensity.current
    val fontSize = MiuixTheme.textStyles.body2.fontSize
    val minHeight: Dp = with(density) { fontSize.value.toDp() }
    val maxHeight: Dp = with(density) { (fontSize.value * 1.4f * maxLines).dp }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight, max = maxHeight)
            .verticalScroll(rememberScrollState()),
    ) {
        RichText(raw = raw)
    }
}
