package com.nainiuzhen.wiki.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 上下箭头三态分段偏好控件（变更点 #1）：基于 [BasicComponent] + 上 / 下 [IconButton]，
 * 在 [options] 三个选项中循环切换（向上 = 索引 +1、向下 = 索引 -1，均取模循环），选中态以
 * `summary` 高亮当前值。专用于「色彩模式」三段（系统 / 深色 / 浅色，绑定 `appState.colorMode` 0/1/2；
 * Monet 开启时文案切换为 Monet 系统 / Monet 深色 / Monet 浅色）。
 *
 * @param title 标题。
 * @param options 三个选项文案（顺序对应索引 0 / 1 / 2）。
 * @param selectedIndex 当前选中索引（0..2）。
 * @param onSelectedIndexChange 切换回调。
 */
@Composable
fun ArrowSegmentedPreference(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(options.size == 3) { "ArrowSegmentedPreference 需要恰好 3 个选项" }
    val count = options.size
    BasicComponent(
        modifier = modifier,
        title = title,
        summary = options.getOrNull(selectedIndex) ?: options.firstOrNull(),
        endActions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val next = (selectedIndex - 1 + count) % count
                        onSelectedIndexChange(next)
                    },
                ) {
                    Icon(
                        imageVector = MiuixIcons.ExpandLess,
                        contentDescription = "上一选项",
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }
                IconButton(
                    onClick = {
                        val next = (selectedIndex + 1) % count
                        onSelectedIndexChange(next)
                    },
                ) {
                    Icon(
                        imageVector = MiuixIcons.ExpandMore,
                        contentDescription = "下一选项",
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }
            }
        },
    )
}
