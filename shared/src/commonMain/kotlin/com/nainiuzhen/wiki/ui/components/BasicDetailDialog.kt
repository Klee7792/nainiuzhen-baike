package com.nainiuzhen.wiki.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.overlay.OverlayDialog

/**
 * 通用详情弹窗骨架（变更点 #21 / #22）：去标题栏，内容区可滚动（受 [maxHeight] 约束收敛
 * [OverlayDialog] 的 `Infinity` 约束，避免嵌套 `verticalScroll` 抛异常，pit #1），底部固定按钮行
 * （[buttons]，建议两按钮各 `Modifier.fillMaxWidth(0.49f)` + `Arrangement.SpaceBetween`）。
 *
 * 物品 / 配方 / NPC 三个详情弹窗统一复用，确保「按钮无需滚动即可点击」。
 *
 * @param show 是否显示。
 * @param onDismissRequest 关闭回调。
 * @param maxHeight 内容区最大高度（默认 640.dp）。
 * @param buttons 底部按钮行内容（在 [RowScope] 内布局）。
 * @param content 可滚动内容。
 */
@Composable
fun BasicDetailDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 640.dp,
    buttons: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    OverlayDialog(
        show = show,
        onDismissRequest = onDismissRequest,
    ) {
        Column(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                content()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                buttons()
            }
        }
    }
}
