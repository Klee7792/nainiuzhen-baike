package com.nainiuzhen.wiki.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 卡片名称跑马灯的**子页面级同步基准**：当前列表里最长那条名称的排版宽度（px）。
 *
 * 存在意义：v35 起跑马灯改成「首尾相连滚一圈」，如果每张卡片各按自己的文字宽度滚，
 * 长名卡与短名卡的行程不同 ⇒ 同样速度下耗时不同 ⇒ 同屏滚动参差不齐。
 * 于是由子页面统一算一个最长宽度，所有卡片都按它补齐（尾部补空格）⇒ 行程、速度、耗时全部一致。
 *
 * 取值 `0` 表示「未提供」（例如宿主页面没包 [ProvideCardMarqueeWidth]），
 * 此时 [CardNameCapsule] 退化为「各滚各的旧行为」。
 */
val LocalCardMarqueeMaxWidth = compositionLocalOf { 0 }

/**
 * 计算当前列表最长名称宽度并向下提供（供 [CardNameCapsule] 读取）。
 *
 * 只做「按字符数取最长的一条 → 量一次」：字符数比较是 O(n) 且极廉价，
 * 真正昂贵的 `measure` 只发生 1 次（不是 3900 次），结果随 [names] / [fontSize] 变化重算。
 *
 * @param names 当前列表（已筛选）的全部名称。
 * @param fontSize 名称字号，必须与 [CardNameCapsule] 收到的 [fontSize] 一致，否则宽度基准对不上。
 * @param style 名称文字样式，必须与 [CardNameCapsule] 收到的 [style] 一致（NPC 用 `body2` 而非 `main`）。
 */
@Composable
fun ProvideCardMarqueeWidth(
    names: List<String>,
    fontSize: TextUnit = TextUnit.Unspecified,
    style: TextStyle = MiuixTheme.textStyles.main,
    content: @Composable () -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val baseStyle = style
    val maxPx = remember(names, fontSize, baseStyle) {
        val longest = names.maxByOrNull { it.length } ?: return@remember 0
        measurer.measure(
            text = AnnotatedString(longest),
            style = baseStyle.copy(fontSize = fontSize),
            maxLines = 1,
            softWrap = false,
        ).size.width
    }
    CompositionLocalProvider(LocalCardMarqueeMaxWidth provides maxPx, content = content)
}

/** [CardNameCapsule] 内部用：量一段文本的排版宽度（单行、不换行），单位 px。 */
internal fun measureTextWidthPx(
    measurer: androidx.compose.ui.text.TextMeasurer,
    text: String,
    style: TextStyle,
): Int = measurer.measure(
    text = AnnotatedString(text),
    style = style,
    maxLines = 1,
    softWrap = false,
).size.width
