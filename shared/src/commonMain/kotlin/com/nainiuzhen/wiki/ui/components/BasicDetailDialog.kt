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
 * 通用详情弹窗骨架（变更点 #21 / #22，H0 三分区重构）：去标题栏，**固定底部按钮行** + 可滚动内容区。
 *
 * H0 修复（横屏按钮被挤出屏幕）：
 * 1. 高度上限不再只写死 [maxHeight]，而是经 [rememberDialogMaxHeight] 再按**窗口高度**收敛 ——
 *    手机横屏可用高仅约 393dp，写死 640dp 会让弹窗高过窗口、底部按钮落到屏幕外。
 * 2. 内容列改用 `weight(1f, fill = false)`：Column 先测量**非加权**的按钮行（拿到自然高度），
 *    剩余空间才分给内容列；`fill = false` 保证内容短时弹窗按内容自适应收窄而非被撑满。
 * 3. 按钮行始终在滚动区**之外**，结构上不可能被滚走或挤出。
 *
 * 物品 / 配方两个详情弹窗统一复用，确保「按钮无需滚动即可点击」。
 *
 * @param show 是否显示。
 * @param onDismissRequest 关闭回调。
 * @param maxHeight 内容区最大高度上限（默认 640.dp，实际取「不超过窗口高度 90%」）。
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
    val resolvedMaxHeight = rememberDialogMaxHeight(maxHeight)

    OverlayDialog(
        show = show,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = resolvedMaxHeight),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
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
