package com.nainiuzhen.wiki.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 左（列表）栏宽度占窗口宽度的比例（文档 §6.3）。 */
private const val LIST_PANE_WIDTH_FRACTION = 0.34f

/** 左（列表）栏宽度下限：840dp 屏恰好落在此值。 */
private val LIST_PANE_MIN_WIDTH = 300.dp

/** 左（列表）栏宽度上限：1200dp 及以上封顶在此值。 */
private val LIST_PANE_MAX_WIDTH = 380.dp

/**
 * 是否启用「列表-详情」双栏（设计文档 §6.1）。
 *
 * 启用条件：**宽 ≥ 840dp 且 高 ≥ 480dp**。只判宽度不够 —— 手机横屏（如 915×393dp）
 * 够宽但太矮，必须保持单栏 + 弹窗，故再叠加 [isShortWindow] 的取反。
 *
 * - 手机竖屏（726dp 宽）→ false（单栏 + 弹窗）
 * - 手机横屏（915×393dp，高不足）→ false（单栏 + 弹窗）
 * - 平板 / 桌面窗口 / 模拟器横屏（1105×726dp）→ true（双栏）
 *
 * 说明：实现时先取一次 [rememberWindowClass] 存进局部变量，避免重复调用。
 */
@Composable
fun rememberUseDualPane(): Boolean {
    val windowClass = rememberWindowClass()
    val wideEnough = windowClass == WindowClass.EXPANDED || windowClass == WindowClass.LARGE
    return wideEnough && !isShortWindow()
}

/**
 * 大屏双栏容器：左列表栏 + 右详情面板。
 *
 * 左栏宽度 = 窗口宽 × [LIST_PANE_WIDTH_FRACTION]，再夹紧到
 * `[LIST_PANE_MIN_WIDTH, LIST_PANE_MAX_WIDTH]`；右栏 `weight(1f)` 吃满剩余空间：
 * - 840dp 屏 → 左 300dp（触底）
 * - 1200dp 及以上 → 左 380dp（封顶）
 *
 * 不加任何分割线 / 底色 / 装饰，保持本项目「简洁无多余装饰」的卡片风格。
 * 调用方需自行用 [rememberUseDualPane] 判断是否调用本组件。
 *
 * @param modifier 外层修饰。
 * @param list 左栏内容（列表）；入参为分配给它自身的修饰符（`fillMaxSize`）。
 * @param detail 右栏内容（详情）；入参同上。
 */
/**
 * 大屏「主页-子页分栏」下，弹窗卡片需要水平偏移的量。
 *
 * 右栏会 provide 为「左栏宽 ÷ 2」，把 [top.yukonga.miuix.kmp.overlay.OverlayDialog] 卡片的
 * 视觉中心从「窗口中线」挪到「右栏中线」；手机单栏时保持 [0.dp]（等同原行为）。
 *
 * **为什么 offset 只挪卡片、不挪遮罩**：miuix `DialogContentLayout.kt` 里遮罩是
 * `:218-227` 独立的 `fillMaxSize` Box，而传进来的 `modifier` 只挂在 `:243` 的
 * `contentModifier`（即卡片本体）上。所以 `OverlayDialog(modifier = Modifier.offset(x = …))`
 * 恰好是「卡片居中到右栏、遮罩仍盖满窗口」——正是分栏下想要的观感。
 */
val LocalDialogHorizontalOffset = compositionLocalOf { 0.dp }

/**
 * 左（列表）栏宽度：窗口宽 × 0.34，夹紧到 [300.dp, 380.dp]。
 * 顶栏限宽与双栏分栏共用本函数，保证两者算出的宽度严格一致。
 */
@Composable
fun rememberListPaneWidth(): Dp {
    val windowWidth = LocalWindowInfo.current.containerDpSize.width
    return (windowWidth * LIST_PANE_WIDTH_FRACTION).coerceIn(LIST_PANE_MIN_WIDTH, LIST_PANE_MAX_WIDTH)
}

@Composable
fun ListDetailPanes(
    modifier: Modifier = Modifier,
    list: @Composable (Modifier) -> Unit,
    detail: @Composable (Modifier) -> Unit,
) {
    val listPaneWidth = rememberListPaneWidth()
    Row(modifier = modifier.fillMaxSize()) {
        Box(Modifier.width(listPaneWidth).fillMaxHeight()) {
            list(Modifier.fillMaxSize())
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            detail(Modifier.fillMaxSize())
        }
    }
}

/**
 * 右栏空态：居中提示文案。用于「主页-子页分栏」下左栏还没点开任何板块时的占位。
 *
 * 文字颜色沿用工程内既有的次级说明文字色 [MiuixTheme.colorScheme.onSurfaceVariantSummary]
 * （与物品 / 配方 / NPC 详情页的次要文字保持一致）。
 *
 * @param text 提示文案，默认「暂无内容」。
 * @param modifier 外层修饰。
 */
@Composable
fun DetailPaneEmptyHint(
    modifier: Modifier = Modifier,
    text: String = "暂无内容",
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

/**
 * 双栏下拦截返回键：右栏有选中项时清空右栏，而不是退出当前列表页。
 *
 * 详情从弹窗改为内嵌面板后，miuix [top.yukonga.miuix.kmp.overlay.OverlayDialog] 内部的返回
 * 处理器不再存在，本组件补上这个消费点。
 *
 * 实现与 miuix 源码 `miuix-ui/.../layout/DialogContentLayout.kt:192-200` 完全一致，直接复用
 * `androidx.navigationevent` 的 compose 返回处理器（`:shared` 已在 commonMain 依赖
 * `androidx.navigationevent:navigationevent-compose`）。
 *
 * 仲裁规则为「最后组合且启用的处理器优先」，因此本处理器在 `enabled = false` 时不消费返回键，
 * 会自然让位给 `NavDisplay` 的路由返回；而筛选弹窗等后组合的弹窗处理器优先级高于本处理器。
 *
 * @param enabled 是否消费返回键（通常传「右栏是否有选中项」）。
 * @param onBack 消费返回键时的回调（通常清空右栏选中项）。
 */
@Composable
fun DualPaneBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val navigationEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = enabled,
        onBackCancelled = {},
        onBackCompleted = { onBack() },
    )
}
