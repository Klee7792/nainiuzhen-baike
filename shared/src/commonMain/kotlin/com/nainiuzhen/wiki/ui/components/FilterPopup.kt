package com.nainiuzhen.wiki.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 类型多选筛选弹窗（变更点 #24 / #25）：基于 [OverlayDialog] 的类型多选列表。
 * 点击某选项即时切换其选中态（[selected]），并通过 [onSelectedChange] 回传；空集合表示「全部」。
 *
 * 根容器 `heightIn(max = 600.dp)` 收敛 [OverlayDialog] 的 `Infinity` 约束（pit #1）。
 *
 * @param show 是否显示。
 * @param onDismissRequest 关闭回调。
 * @param title 标题（如「筛选类别」）。
 * @param options 全部可选项（类别 / 类型标签）。
 * @param selected 当前已选集合（空 = 全部）。
 * @param onSelectedChange 选中集合变化回调（即时过滤）。
 */
@Composable
fun FilterPopup(
    show: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    options: List<String>,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    OverlayDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        title = title,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            options.forEach { option ->
                val isSelected = option in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectedChange(
                                if (isSelected) selected - option else selected + option,
                            )
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = option,
                        style = MiuixTheme.textStyles.body1,
                        color = if (isSelected) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}
