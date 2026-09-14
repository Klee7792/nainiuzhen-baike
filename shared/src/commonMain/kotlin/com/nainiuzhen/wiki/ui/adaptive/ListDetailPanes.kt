package com.nainiuzhen.wiki.ui.adaptive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.anim.DecelerateEasing
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 左（列表）栏宽度占窗口宽度的比例（文档 §6.3）。 */
private const val LIST_PANE_WIDTH_FRACTION = 0.34f

/** 左（列表）栏宽度下限：840dp 屏恰好落在此值。 */
private val LIST_PANE_MIN_WIDTH = 300.dp

/** 左（列表）栏宽度上限：1200dp 及以上封顶在此值。 */
private val LIST_PANE_MAX_WIDTH = 380.dp

/**
 * 「单栏 ↔ 双栏」分割比例的补间时长（毫秒）。
 *
 * 横竖屏切换时，本容器不做任何结构性重建 —— 只是把分割比例 `0f ↔ 1f` 插值，
 * 于是「左栏的出现 / 消失」天然变成一段平滑滑动，而不是硬切。
 * 320ms 对齐 miuix 导航过渡的量级（`NavTransitions.MiuixDefault`），
 * 使左栏滑动与右栏内子页的 NavDisplay 过渡看起来是同一次动作的两半。
 */
private const val PANE_SPLIT_MS = 320

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
 *
 * ⚠️ 本函数只决定**布局形态**，不再决定「谁来渲染子页」——渲染方永远是同一个
 * `NavDisplay`（见 `MainScreen` / `SubPageNavHost`）。横竖屏切换时它会在 true / false
 * 之间翻转，而翻转只带来尺寸与位置的变化，不会销毁任何 composition。
 */
@Composable
fun rememberUseDualPane(): Boolean {
    val windowClass = rememberWindowClass()
    val wideEnough = windowClass == WindowClass.EXPANDED || windowClass == WindowClass.LARGE
    return wideEnough && !isShortWindow()
}

/**
 * 左（列表）栏宽度：窗口宽 × 0.34，夹紧到 [300.dp, 380.dp]。
 * 顶栏限宽与双栏分栏共用本函数，保证两者算出的宽度严格一致。
 */
@Composable
fun rememberListPaneWidth(): Dp {
    val windowWidth = LocalWindowInfo.current.containerDpSize.width
    return (windowWidth * LIST_PANE_WIDTH_FRACTION).coerceIn(LIST_PANE_MIN_WIDTH, LIST_PANE_MAX_WIDTH)
}

/**
 * 「列表层 / 详情层」**双层常驻**容器（大屏适配 §6、横竖屏状态保持）。
 *
 * 本容器永远是同一段代码路径、同一棵子树结构，只有两个子槽位：
 *
 * ```
 * Layout {
 *     list(Modifier.fillMaxSize())    // 槽位 0：列表层（主页 + 设置）
 *     detail(Modifier.fillMaxSize())  // 槽位 1：详情层（唯一返回栈的 NavDisplay）
 * }
 * ```
 *
 * 由归一化分割比例 `t`（0 = 单栏，1 = 双栏）驱动两个槽位的位置与宽度：
 *
 * | | 列表层 | 详情层 |
 * |---|---|---|
 * | `t = 0`（单栏） | `x=0`，满宽 | `x=0`，满宽（叠在列表层上） |
 * | `t = 1`（双栏） | `x=0`，宽 [rememberListPaneWidth] | `x=左栏宽`，宽 = 余下 |
 *
 * `t ∈ (0,1)` 即过渡中途，两层平滑滑动。
 *
 * ### 为什么不用 `Row` + `weight(1f)`
 *
 * `Row { Box(width); Box(weight(1f)) }` 在「单栏 / 双栏」之间切换时**分支结构不同**，
 * 会让 Compose 把整棵子树拆掉重建 —— 于是横竖屏一换，子页的滚动位置、搜索词、筛选、
 * 正在展示的弹窗全部丢失（用户反馈的「转屏就刷新」）。改成**恒定结构 + 只动尺寸**后，
 * 两个槽位在组件树中的位置永远不变，composition 原地保留。
 *
 * ### 命中测试
 *
 * 详情层在单栏（`t = 0`）时满宽叠在列表层上方，而 miuix `NavDisplay` 的每个 entry 根节点
 * 都带「命中测试不透明」的指针节点 —— 因此**谁在上层**必须由调用方显式用 `Modifier.zIndex`
 * 决定（见 `MainScreen` 里 `detailOnTop` 的推导）。本容器自身不干预层级。
 *
 * ### 性能
 *
 * 补间比例只在下面的 measure 块里读取 ⇒ 一次动画只触发**重新测量/布局**，
 * 不会让上层及其子树每帧重组。
 *
 * ### 约定
 *
 * `list` / `detail` 两个 lambda 各自必须**恰好发射一个根节点**（本容器按 `measurables[0]` /
 * `measurables[1]` 取用）。
 *
 * @param dual 是否双栏。旋转会翻转它，但不会重建任何 composition。
 * @param modifier 外层修饰。
 * @param list 列表层内容；入参为分配给它自身的修饰符（`fillMaxSize`）。
 * @param detail 详情层内容；入参同上。
 */
@Composable
fun ListDetailPanes(
    dual: Boolean,
    modifier: Modifier = Modifier,
    list: @Composable (Modifier) -> Unit,
    detail: @Composable (Modifier) -> Unit,
) {
    val listPaneWidth = rememberListPaneWidth()
    // 在 composition 期换算成 px：布局期不再需要 Density，measure 块里纯粹做算术。
    val listPanePx = with(LocalDensity.current) { listPaneWidth.roundToPx() }
    val split by animateFloatAsState(
        targetValue = if (dual) 1f else 0f,
        animationSpec = tween(durationMillis = PANE_SPLIT_MS, easing = DecelerateEasing(1.5f)),
        label = "paneSplit",
    )

    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            list(Modifier.fillMaxSize())
            detail(Modifier.fillMaxSize())
        },
    ) { measurables, constraints ->
        val fullWidth = constraints.maxWidth
        val height = constraints.maxHeight
        val t = split.coerceIn(0f, 1f)
        // t=0 ⇒ 列表满宽、详情满宽且不偏移；t=1 ⇒ 列表收缩到左栏、详情让到右栏。
        val listWidth = (fullWidth + (listPanePx - fullWidth) * t).roundToInt().coerceIn(0, fullWidth)
        val detailX = (listPanePx * t).roundToInt().coerceIn(0, fullWidth)
        val detailWidth = fullWidth - detailX

        val listPlaceable = measurables[0].measure(Constraints.fixed(listWidth, height))
        val detailPlaceable = measurables[1].measure(Constraints.fixed(detailWidth, height))

        layout(fullWidth, height) {
            listPlaceable.place(0, 0)
            detailPlaceable.place(detailX, 0)
        }
    }
}

/**
 * 右栏空态：铺主题页面底色 + 居中提示文案。用于「主页-子页分栏」下左栏还没点开任何板块时的占位。
 *
 * ⚠️ 必须自己铺一层 [MiuixTheme.colorScheme.background]：详情层（`NavDisplay`）的每个 entry
 * 根节点都不带底色，空态若也透明，透出来的就是 **Activity 主题的 windowBackground**
 * （`Theme.Encyclopedia` 是浅色 Material 主题 ⇒ `#FAFAFA`）。于是深色模式下会出现
 * 「左栏深色、右栏一大块近白」的割裂感（真机实测均值：左区≈28，右区 250 且 std 0.15）。
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
    Box(
        modifier.fillMaxSize().background(MiuixTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}
