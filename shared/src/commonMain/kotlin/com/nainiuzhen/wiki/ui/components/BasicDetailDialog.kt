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

    // ⚠️ 大屏「主页-子页分栏」下**不要**给本弹窗加任何水平偏移。
    // miuix 的 `OverlayDialog` 默认 `renderInRootScaffold = true`，会把弹窗（含遮罩）渲染进
    // **最近的 miuix `Scaffold`**。分栏时子页跑在右栏、自带 `AppSubPageScaffold`，
    // 于是弹窗天然「以右栏为基准左右居中、遮罩只盖右栏」，无需任何额外代码。
    // 实测（模拟器横屏 1105×726dp，右栏 770..2266px）：卡片中心 x=1518px，恰等于右栏中心；
    // 左栏背景亮度 247 不变，右栏被压到 172~178 ⇒ 遮罩确实只在右栏。
    // 曾经加过 offset 想"手动居中"，结果把卡片推向右栏右侧、超出屏幕。

    // 强制底部贴合：miuix 在大屏（宽≥840dp 且 高≥480dp）会改为居中，导致横屏底部留白过大。
    // 显式传 largeScreen = false，使横屏与竖屏观感一致（均贴底）。
    OverlayDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        largeScreen = false,
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
