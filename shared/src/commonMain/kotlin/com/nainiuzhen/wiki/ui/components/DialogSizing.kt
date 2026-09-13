package com.nainiuzhen.wiki.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import kotlin.math.min

/**
 * 弹窗高度上限工具（#29 / H0）。
 *
 * 背景：miuix `OverlayDialog` 在「小屏」分支**不做**限高
 * （miuix `layout/DialogContentLayout.kt:351`：`heightIn(max = if (isLargeScreen) windowHeight * 2 / 3 else Dp.Unspecified)`），
 * 而工程内各弹窗为绕开 pit #1（OverlayDialog 内容拿到的最大高度约束不可用，直接 `verticalScroll` 会崩）
 * 各自硬编码了 480 / 600 / 640 dp 的上限。手机横屏可用高仅约 393dp，硬编码上限远超窗口高度
 * → 弹窗比窗口还高，底部按钮被挤出屏幕（NPC「日程」因此点不到）。
 *
 * 解法：统一按**当前窗口高度**取上限，保证任何屏高下弹窗都装得下。
 *
 * @param max 各弹窗原有的期望上限（作为「不超过」的天花板，保留其设计意图）。
 * @param fraction 占窗口高度的比例，默认 0.9f（上下各留约 5% 呼吸空间）。
 */
@Composable
fun rememberDialogMaxHeight(max: Dp, fraction: Float = 0.9f): Dp {
    val windowHeight = LocalWindowInfo.current.containerDpSize.height.value
    return if (windowHeight.isFinite() && windowHeight > 0f) {
        Dp(min(max.value, windowHeight * fraction))
    } else {
        max
    }
}
