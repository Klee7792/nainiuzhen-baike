// 奶牛镇百科 · MainScreen（v7 重构）
//
// 主要变更（v7）：
// - 左右滑动切换：使用 HorizontalPager 在「主页」与「设置」之间左右滑切；
//   子页（物品 / 配方 / NPC / 日程 / 关于）由 navigator.push 全屏覆盖，不参与切换。
//   即使「显示底栏」关闭，pager 仍挂在内容区，滑动照样可用。
// - 底栏模式修复：把 LocalNavigationBarDisplayMode 从内容区搬到 NavigationBar
//   的 `mode` 参数，使「仅图标 / 选中显示文字」真正生效。
// - 悬浮底栏：miuix 默认风格直接复用 miuix 内置 FloatingNavigationBar + textureBlur
//   （surfaceContainer.copy(0.6f) 混合色 + GlassStrokeMiddleLight 玻璃描边），浅色模式不再像白底污渍；
//   iOS-like 走 IosLiquidGlassNavigationBar 液态玻璃底栏。
// - 切页时重置 MiuixScrollBehavior 的 heightOffset / contentOffset，使顶栏
//   在新页面回到展开状态。

package com.nainiuzhen.wiki.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nainiuzhen.wiki.ui.adaptive.DetailPaneEmptyHint
import com.nainiuzhen.wiki.ui.adaptive.ListDetailPanes
import com.nainiuzhen.wiki.ui.adaptive.LocalDetailOverlay
import com.nainiuzhen.wiki.ui.adaptive.rememberDetailOverlayState
import com.nainiuzhen.wiki.ui.adaptive.rememberUseDualPane
import com.nainiuzhen.wiki.ui.components.AppTopAppBar
import com.nainiuzhen.wiki.ui.components.liquid.IosLiquidGlassNavigationBar
import com.nainiuzhen.wiki.ui.components.rememberAppBlurBackdrop
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.ui.nav.Navigator
import com.nainiuzhen.wiki.ui.nav.Route
import com.nainiuzhen.wiki.ui.nav.SubPageNavHost
import com.nainiuzhen.wiki.ui.settings.SettingsContent
import com.nainiuzhen.wiki.utils.AppLog
import com.nainiuzhen.wiki.utils.LocalAppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random
import top.yukonga.miuix.kmp.anim.DecelerateEasing
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.FloatingToolbar
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 弹窗遮罩的时长常量 —— 全部对齐 miuix `DialogContentLayout` 的 `dimProgress`：
 * 淡入 `tween(300, DecelerateEasing(1.5f))`、淡出 `tween(250, DecelerateEasing(1.5f))`。
 * 左栏自补的这一层遮罩照抄同一套参数，两栏的明暗变化才会**同起同落**，
 * 否则会出现「右栏已经亮回来、左栏还黑着」的割裂感。
 */
private const val DIALOG_DIM_IN_MS = 300
private const val DIALOG_DIM_OUT_MS = 250

/**
 * 弹窗关闭动画的「宽限期」（毫秒）。
 *
 * 弹窗被关掉后，遮罩 / 卡片还要播一段收起动画（遮罩淡出 250ms）。这段时间内左栏继续拦截点击，
 * 避免用户的下一击「穿过」动画尾巴、在一次操作里同时完成「关弹窗 + 跳转」
 * （用户反馈的「直接就覆盖了」）。取 280ms：比淡出动画略长，收尾时两栏都已亮回来，
 * 拦截却还没撤，多出来的那一下点击也无副作用（无事可关 ⇒ `dismissTop()` 是空操作）。
 */
private const val DIALOG_CLOSE_GRACE_MS = 280

/**
 * 应用主界面：**唯一返回栈** + 「列表层 / 详情层」双层常驻。
 *
 * ```
 * ListDetailPanes {
 *     列表层：MainPaneShell（顶栏 + 底栏 + 主页/设置 Pager）
 *     详情层：SubPageNavHost（唯一返回栈的 NavDisplay）
 * }
 * ```
 *
 * ### 为什么是「唯一栈」
 *
 * 改造前工程里有两个返回栈（窗口级单栏用、右栏级分栏用），由 `useDualPane` 决定谁生效，
 * 而 `useDualPane` 会在旋转时瞬间翻转、栈内容却从不迁移，于是产生三类幽灵状态：
 *
 * 1. 横屏看子页 → 转竖屏：切回单栏路径 ⇒ 窗口栈仍是 `[Main]` ⇒ 看到主页（子页"丢了"）；
 * 2. 竖屏点板块（窗口栈变 `[Main, ItemList]`）→ 回横屏：窗口栈顶让它**全屏**盖住分栏 ⇒ 没有左栏；
 * 3. 此时按返回：`LocalNavigator` 已换成右栏导航器 ⇒ 弹的是右栏栈 ⇒ 全屏那层不动、底下右栏却变了
 *    ⇒ 横竖「叠加」。
 *
 * 现在只留一个栈，且**永远由同一个 `SubPageNavHost` 渲染**；`useDualPane` 退化为纯粹的
 * 「布局形态」开关（只影响 [ListDetailPanes] 里两个槽位的尺寸与位置）。旋转时没有任何
 * composition 被销毁或搬家 ⇒ 子页的滚动位置、搜索词、筛选、**打开着的弹窗**全部原地保留。
 *
 * ### 详情层的层级（zIndex）
 *
 * 单栏时详情层与列表层满宽重叠，而 miuix `NavDisplay` 会给每个 entry 的根节点挂一个
 * 「命中测试不透明」的指针节点（`NavDisplay.kt` 的 `opaqueInputModifier`）——它会拦下
 * 落在自身矩形内的命中测试、不让更下层兄弟节点收到事件。而「没打开任何子页」时详情层
 * 依然存在（`entry<Route.Main>` 渲染空内容），若它压在列表层上，主页/设置页会**整个点不动**。
 *
 * 因此层级必须显式仲裁：只有「分栏」或「确实有子页」时把详情层抬上去；子页被 pop 时
 * 延后 [DETAIL_TOP_LINGER_MS] 再降下来，否则正在播放的退场动画会被列表层盖住。
 */
private const val DETAIL_TOP_LINGER_MS = 360L

/**
 * 右栏空态提示文案池（分栏下「还没点开任何板块」时显示）。
 *
 * 冷启动随机取一条；之后**只在从子页退回空白状态时**换一条（见 [MainScreen] 里的 reroll 逻辑）。
 * 语气刻意做成不同 NPC 的性格：有活泼的、慵懒的、害羞的、急性子的，配上颜文字，
 * 让「什么也没有 / 点左边看看」这类提示不那么干巴。
 */
private val EMPTY_HINTS = listOf(
    "这里空空如也，点左边的板块看看吧 (・ω・)ノ",
    "什么也没有呢…先去挑个板块吧 (￣▽￣)ﾉ",
    "啊…这里还没东西，点一下左边试试 (｡･ω･｡)",
    "空荡荡的，像我家见底的米缸一样 (´・ω・｀)",
    "选一个板块吧，我在这儿等你 (๑•̀ㅂ•́)و✧",
    "这里啥都没有，别盯着我看啦 (⁄ ⁄•⁄ω⁄•⁄ ⁄)",
    "快点选一个嘛，我快无聊死了 (╯°□°)╯",
    "嗯…此处空无一物，要不要逛逛？ („• ᴗ •„)",
    "先选个板块，我这就去给你翻资料 (๑˃̵ᴗ˂̵)و",
    "空空如也～随便点点就行啦 ( ˘ ³˘)♥",
)

@Composable
fun MainScreen() {
    val useDualPane = rememberUseDualPane()
    // 只在 单栏↔分栏 翻转时记一次日志（此前每次重组都记，一次会话刷几百条，既刷屏又拖性能）。
    var lastLoggedDual by remember { mutableStateOf<Boolean?>(null) }
    if (lastLoggedDual != useDualPane) {
        lastLoggedDual = useDualPane
        AppLog.i("MainScreen 布局：useDualPane=$useDualPane")
    }

    // —— 唯一返回栈 ——
    // 栈底恒为 [Route.Main]（= 详情层当前没有内容）。各业务页里现成的
    // `navigator.push(...)` / `navigator.pop()` 一行都不用改：
    //   · push = 详情层打开一个子页（单栏=全屏盖住列表，分栏=落在右栏）；
    //   · pop  = 退回上一层，栈底保留 ⇒ 退无可退时自动回到空态。
    // `rememberNavBackStack` 自带 saver；更关键的是它的宿主组件永远不被销毁。
    val backStack = rememberNavBackStack<Route>(Route.Main)
    val navigator = remember(backStack) { Navigator(backStack) }
    val hasDetail = backStack.size > 1

    val detailOverlay = rememberDetailOverlayState()
    // 弹窗关闭动画期间的「宽限期」：右下角弹窗正在收起时，左栏继续拦截，避免第二下点击
    // 落在动画尾巴上、把弹窗和跳转同时发生（用户反馈的"直接覆盖"）。
    val dialogOpen = detailOverlay.isOpen
    var dismissGrace by remember { mutableStateOf(false) }
    LaunchedEffect(dialogOpen) {
        if (dialogOpen) {
            dismissGrace = true
        } else {
            delay(DIALOG_CLOSE_GRACE_MS.toLong())
            dismissGrace = false
        }
    }
    val intercepting = dialogOpen || dismissGrace

    val leftPaneAlpha by animateFloatAsState(
        targetValue = if (dialogOpen) 1f else 0f,
        animationSpec = tween(DIALOG_CLOSE_GRACE_MS),
        label = "leftPaneDim",
    )

    // 详情层是否压在列表层之上（见上方文档「详情层的层级」）。
    // 单栏 + 无子页 ⇒ false：让主页/设置页重新拿回点击。升降走同一 LaunchedEffect：
    // 上升立即生效（子页一开始入场就得在最上层），下降延后 LINGER，保护退场动画。
    val wantDetailOnTop = useDualPane || hasDetail
    var detailOnTop by remember { mutableStateOf(wantDetailOnTop) }
    LaunchedEffect(wantDetailOnTop) {
        if (wantDetailOnTop) {
            detailOnTop = true
        } else {
            delay(DETAIL_TOP_LINGER_MS)
            detailOnTop = false
        }
    }

    // —— 空态文案 ——
    // 冷启动随机取一条；之后**只在「从子页退回空白状态」时**换一条。
    // 刻意不跟着「点板块 / 栈顶变化」刷新 —— 那是主页三大板块图示的 reroll 规则，
    // 提示语若也每次点都变会显得聒噪（用户明确要求：只有退回空白才切）。
    var emptyHintIndex by remember { mutableIntStateOf(Random.nextInt(EMPTY_HINTS.size)) }
    var hadDetailBefore by remember { mutableStateOf(hasDetail) }
    LaunchedEffect(hasDetail) {
        if (hadDetailBefore && !hasDetail) {
            // 从「有子页」回到空态：换一条，且保证与当前这条不同（偏移取 1..size-1）。
            emptyHintIndex = (emptyHintIndex + (1 until EMPTY_HINTS.size).random()) % EMPTY_HINTS.size
        }
        hadDetailBefore = hasDetail
    }

    // 分栏空态提示：只在双栏时给详情层一个占位。单栏时详情层满宽叠在列表层上，
    // 若也渲染占位会把下面的主页整个盖住，所以留空（NavEntryHost 根节点无底色 ⇒ 透明）。
    val homePlaceholder: @Composable () -> Unit = remember(useDualPane, emptyHintIndex) {
        { if (useDualPane) DetailPaneEmptyHint(text = EMPTY_HINTS[emptyHintIndex]) }
    }

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalDetailOverlay provides detailOverlay,
    ) {
        ListDetailPanes(
            dual = useDualPane,
            list = { modifier ->
                Box(
                    modifier
                        // iOS 白屏修复（run 35076719999 验证通过）：identity graphicsLayer 强制
                        // 列表层成为独立离屏图层——没有它，列表层整棵子树的绘制产物不会被合成上屏。
                        // ⚠️ 这是修复的一部分，不是调试代码，勿删。
                        .graphicsLayer { },
                ) {
                    MainPaneShell()
                    // 左栏自补遮罩：miuix 遮罩只盖右栏（弹窗渲染进右栏 Scaffold），
                    // 所以左栏得自己盖一层同色遮罩，并在弹窗期间把点击解释为「先关弹窗」。
                    if (intercepting) {
                        val dim = MiuixTheme.colorScheme.windowDimming
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(dim.copy(alpha = dim.alpha * leftPaneAlpha))
                                .pointerInput(Unit) {
                                    detectTapGestures { detailOverlay.dismissTop() }
                                },
                        )
                    }
                }
            },
            detail = { modifier ->
                // clipToBounds：详情槽内容必须裁剪在本槽矩形内。miuix MiuixDefault 转场会给
                // 被覆盖层 -0.25×width 的视差位移，而 Compose 默认不裁剪子层——「暂无内容」页
                // 会随位移左移溢出详情槽、盖到左侧栏上（横屏黑块 bug）。裁剪后溢出部分不可见。
                Box(
                    modifier
                        .zIndex(if (detailOnTop) 1f else 0f)
                        .clipToBounds(),
                ) {
                    SubPageNavHost(
                        backStack = backStack,
                        navigator = navigator,
                        modifier = Modifier.fillMaxSize(),
                        homePlaceholder = homePlaceholder,
                    )
                }
            },
        )
    }
}

/**
 * iOS 液态玻璃底栏总开关。
 * 白屏根因已定位为列表层绘制合成问题（与液态玻璃无关），恢复启用；
 * 视觉不满意再考虑打磨自绘实现或降级为经典毛玻璃。
 */
private const val IOS_LIQUID_GLASS_ENABLED = true

/**
 * 列表层主体：顶栏 + 底栏 + 「主页 / 设置」两页 HorizontalPager。
 * 单栏时铺满整屏；分栏时作为**左栏**（宽度由 [ListDetailPanes] 决定）。
 */
@Composable
private fun MainPaneShell() {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val appState = LocalAppSettings.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberAppBlurBackdrop()

    val currentPage = pagerState.currentPage // 0 = 主页，1 = 设置

    // 安全区接入验证（每次会话记一次）：全屏绘制后 iOS 应读到真实的 状态栏 / 小白条 高度
    // （iPhone XR 预期 top≈47dp bottom≈34dp；读到 0 说明 insets 链路没通，顶/底延伸会失效）。
    var insetsLogged by remember { mutableStateOf(false) }
    if (!insetsLogged) {
        insetsLogged = true
        AppLog.i(
            "safe insets top=${WindowInsets.statusBars.asPaddingValues().calculateTopPadding()} " +
                "bottom=${WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()}",
        )
    }

    // 切页时把顶栏的折叠 / 滚动偏移重置为 0，使新页面顶栏始终从展开态开始
    LaunchedEffect(currentPage) {
        scrollBehavior.state.heightOffset = 0f
        scrollBehavior.state.contentOffset = 0f
    }

    Scaffold(
        topBar = {
            AppTopAppBar(
                title = if (currentPage == 0) "奶牛镇百科" else "设置",
                largeTitle = if (currentPage == 0) "奶牛镇百科" else "设置",
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
            )
        },
        bottomBar = {
            Box {
                if (appState.showNavigationBar) {
                val isIos = appState.floatingNavigationBarStyle == 1
                val navItems = listOf(
                    NavigationItem(label = "主页", icon = MiuixIcons.Home),
                    NavigationItem(label = "设置", icon = MiuixIcons.Settings),
                )
                val onNavClick: (Int) -> Unit = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                }
                val badgeProvider: (Int) -> (@Composable () -> Unit)? =
                    { if (appState.showNavigationBadge) ({ NavigationBadgeDot() }) else null }

                if (appState.useFloatingNavigationBar) {
                    if (IOS_LIQUID_GLASS_ENABLED && isIos) {
                        // iOS 液态玻璃底栏：折射 / 高光 / 按住拖动切换 / 选中果冻弹跳（移植自 miuix demo）
                        // 白屏排查期曾强制关闭，现已证实白屏与它无关，恢复启用（v41）。
                        IosLiquidGlassNavigationBar(
                            items = navItems,
                            selectedIndex = currentPage,
                            onItemClick = onNavClick,
                            backdrop = backdrop,
                            isBlurActive = backdrop != null,
                            badge = badgeProvider,
                        )
                    } else {
                        // miuix 风格悬浮底栏：对齐 miuix demo——内置 FloatingNavigationBar + textureBlur
                        // （surfaceContainer.copy(0.6f) 混合色 + GlassStrokeMiddleLight 玻璃描边），
                        // blur 激活时 color=Transparent 让毛玻璃透出，浅色模式不再像白底污渍。
                        val blurActive = backdrop != null
                        val floatingBarColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer
                        val floatingBarShape = RoundedCornerShape(FloatingToolbarDefaults.CornerRadius)
                        val isBarDark = when (appState.colorMode) {
                            0 -> isSystemInDarkTheme()
                            1 -> true
                            else -> false
                        }
                        val floatingHighlight = remember(isBarDark) {
                            if (isBarDark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight
                        }
                        FloatingNavigationBar(
                            modifier = Modifier
                                // 全屏绘制后悬浮底栏整体抬离 home indicator（iOS）/ 手势条（Android）；
                                // 液态玻璃底栏与 miuix NavigationBar 自带 insets 适配，只有这里需要外挂。
                                .windowInsetsPadding(
                                    WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
                                )
                                .then(
                                    if (blurActive) {
                                        Modifier.textureBlur(
                                            backdrop = backdrop,
                                            shape = floatingBarShape,
                                            blurRadius = 25f,
                                            colors = BlurDefaults.blurColors(
                                                blendColors = listOf(
                                                    BlendColorEntry(color = MiuixTheme.colorScheme.surfaceContainer.copy(0.6f)),
                                                ),
                                            ),
                                            highlight = floatingHighlight,
                                        )
                                    } else {
                                        Modifier
                                    },
                                ),
                            color = floatingBarColor,
                            horizontalAlignment = when (appState.floatingNavigationBarPosition) {
                                1 -> Alignment.Start
                                2 -> Alignment.End
                                else -> Alignment.CenterHorizontally
                            },
                        ) {
                            navItems.forEachIndexed { index, item ->
                                FloatingNavigationBarItem(
                                    selected = currentPage == index,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                    icon = item.icon,
                                    label = item.label,
                                    badge = badgeProvider(index),
                                )
                            }
                        }
                    }
                } else {
                    // 非悬浮底栏：把 mode 真正传给 NavigationBar，修复"仅图标/选中显示文字"无效问题
                    val mode = NavigationBarDisplayMode.entries.getOrElse(
                        abs(appState.navigationBarMode).coerceIn(0, 2),
                    ) { NavigationBarDisplayMode.IconAndText }
                    NavigationBar(mode = mode) {
                        NavigationBarItem(
                            selected = currentPage == 0,
                            onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                            icon = MiuixIcons.Home,
                            label = "主页",
                            badge = if (appState.showNavigationBadge) ({ NavigationBadgeDot() }) else null,
                        )
                        NavigationBarItem(
                            selected = currentPage == 1,
                            onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                            icon = MiuixIcons.Settings,
                            label = "设置",
                            badge = if (appState.showNavigationBadge) ({ NavigationBadgeDot() }) else null,
                        )
                    }
                }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
                // 水平安全区（横屏刘海 / 打孔）避让，公式与 miuix TopAppBar 内部一致
                // （displayCutout + navigationBars 的 Horizontal）：分栏时左栏内容不被刘海遮挡，
                // 且与自避让的顶栏内容边缘对齐。竖屏时水平 insets 恒为 0，无视觉变化。
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
            ) {
                HorizontalPager(
                    state = pagerState,
                    // 主页↔设置 的左右滑动只受「允许手动滑动翻页」(pageUserScroll) 控制。
                    // 「启用滑动返回」(enableSwipeBack) 是另一回事：管的是子页从侧边滑动
                    // 返回上一页（NavDisplay.swipeDismiss），与主页 pager 无关。
                    userScrollEnabled = appState.pageUserScroll,
                ) { page ->
                    when (page) {
                        0 -> Box(Modifier.fillMaxSize()) {
                            HomeContent(innerPadding, scrollBehavior)
                        }
                        else -> Box(Modifier.fillMaxSize()) {
                            SettingsContent(innerPadding, scrollBehavior)
                        }
                    }
                }
            }
            // 悬浮工具栏 / 悬浮按钮（选项已隐藏，但保留底层代码与逻辑，默认关闭不显示）
            val floatingAlignment = when {
                appState.showFloatingToolbar || appState.showFloatingActionButton -> {
                    when (appState.floatingToolbarPosition.takeIf { appState.showFloatingToolbar }
                        ?: appState.floatingActionButtonPosition) {
                        1 -> Alignment.BottomStart
                        2 -> Alignment.BottomCenter
                        else -> Alignment.BottomEnd
                    }
                }
                else -> Alignment.BottomEnd
            }
            val floatingPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                // 底栏被隐藏时 innerPadding.bottom=0，取 max 保证悬浮件不被小白条/手势条遮挡
                bottom = maxOf(
                    innerPadding.calculateBottomPadding(),
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ) + 16.dp,
            )
            if (appState.showFloatingToolbar) {
                Box(
                    modifier = Modifier
                        .align(floatingAlignment)
                        .padding(floatingPadding),
                ) {
                        FloatingToolbar {
                            Icon(
                                imageVector = MiuixIcons.Add,
                            contentDescription = "快捷操作",
                            tint = MiuixTheme.colorScheme.onSurfaceContainer,
                            modifier = Modifier.rotate(-90f),
                        )
                    }
                }
            } else if (appState.showFloatingActionButton) {
                Box(
                    modifier = Modifier
                        .align(floatingAlignment)
                        .padding(floatingPadding),
                ) {
                    FloatingActionButton(onClick = {}) {
                        Icon(
                            imageVector = MiuixIcons.Add,
                            contentDescription = "快捷操作",
                            tint = MiuixTheme.colorScheme.onPrimary,
                            modifier = Modifier.rotate(-90f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationBadgeDot() {
    // 红色角标，置于 icon 右上角外侧，不遮挡图标本体
    Box(
        modifier = Modifier
            .size(8.dp)
            .offset { IntOffset(6.dp.roundToPx(), (-6).dp.roundToPx()) }
            .background(MiuixTheme.colorScheme.error, CircleShape),
    )
}

