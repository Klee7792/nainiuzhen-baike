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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nainiuzhen.wiki.ui.adaptive.DetailPaneEmptyHint
import com.nainiuzhen.wiki.ui.adaptive.DualPaneBackHandler
import com.nainiuzhen.wiki.ui.adaptive.ListDetailPanes
import com.nainiuzhen.wiki.ui.adaptive.LocalDetailOverlay
import com.nainiuzhen.wiki.ui.adaptive.rememberDetailOverlayState
import com.nainiuzhen.wiki.ui.adaptive.rememberUseDualPane
import com.nainiuzhen.wiki.ui.components.AppTopAppBar
import com.nainiuzhen.wiki.ui.components.liquid.IosLiquidGlassNavigationBar
import com.nainiuzhen.wiki.ui.components.rememberAppBlurBackdrop
import com.nainiuzhen.wiki.ui.items.ItemListScreen
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.ui.nav.Navigator
import com.nainiuzhen.wiki.ui.nav.Route
import com.nainiuzhen.wiki.ui.npc.NpcListScreen
import com.nainiuzhen.wiki.ui.npc.NpcScheduleScreen
import com.nainiuzhen.wiki.ui.recipe.RecipeListScreen
import com.nainiuzhen.wiki.ui.settings.AboutScreen
import com.nainiuzhen.wiki.ui.settings.ImageScaleSettingsScreen
import com.nainiuzhen.wiki.ui.settings.SettingsContent
import com.nainiuzhen.wiki.ui.settings.about.LicenseScreen
import com.nainiuzhen.wiki.utils.LocalAppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
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
import top.yukonga.miuix.kmp.nav.core.NavBackStack
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
 * 底栏宿主：2 个标签页——「主页」（3 个图鉴板块）与「设置」（完整设置页）。
 * 通过 HorizontalPager 支持左右滑动切换（不依赖底栏，子页不参与）。
 * 子页（物品 / 配方 / NPC / 日程 / 关于 / 协议 / 图片倍率）通过
 * [com.nainiuzhen.wiki.ui.nav.Navigator.push] 展示——**手机单栏时全屏覆盖，
 * 大屏分栏时落在右侧栏内**（see [MainScreen]）。
 */
@Composable
fun MainScreen() {
    val useDualPane = rememberUseDualPane()
    val windowNavigator = LocalNavigator.current

    // 右栏自己的一套返回栈：栈底恒为 [Route.Main]（=「尚未打开任何子页」→ 右栏空态）。
    // 刻意让它与窗口栈**语义完全一致**，于是：
    //   · `Navigator.push`  = 右栏打开一个子页；
    //   · `Navigator.pop`   = 退回上一层（栈底保留 ⇒ 退无可退时自动回到空态）。
    // 结果就是各业务页面里现成的 `navigator.push(...)` / `navigator.pop()` 一行都不用改。
    // `rememberNavBackStack` 自带 saver，旋转 / 配置变更后右栏内容不丢。
    val paneBackStack = rememberNavBackStack<Route>(Route.Main)
    val paneNavigator = remember(paneBackStack) { Navigator(paneBackStack) }

    // 分栏时把 [LocalNavigator] 换成「右栏自己的」：左栏点板块、右栏内点返回箭头、
    // 详情里点「日程」，全部经由现有页面的 navigator 调用，自动落在右栏栈上。
    val activeNavigator = if (useDualPane) paneNavigator else windowNavigator

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

    CompositionLocalProvider(LocalNavigator provides activeNavigator) {
        if (useDualPane) {
            CompositionLocalProvider(LocalDetailOverlay provides detailOverlay) {
                ListDetailPanes(
                    list = { modifier ->
                        Box(modifier) {
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
                    detail = { modifier -> DetailPaneHost(modifier, paneBackStack) },
                )
            }
            // 右栏有子页时，系统返回键先退右栏；右栏回到空态后本处理器 enabled=false
            // 自动让位给 NavDisplay 的路由返回（仲裁规则：最后组合且启用者优先）。
            DualPaneBackHandler(enabled = paneBackStack.size > 1) { paneNavigator.pop() }
        } else {
            MainPaneShell()
        }
    }
}

/**
 * 右栏：按右栏返回栈栈顶渲染对应子页；栈顶为 [Route.Main]（= 没打开任何子页）时显示空态。
 *
 * 这里渲染的都是**原本为整屏设计的子页**，它们各自带 `AppSubPageScaffold` 顶栏，
 * 因此右栏天然有独立顶栏、随自身内容滚动折叠，与左栏顶栏互不干扰。
 * 各子页顶栏的返回箭头调用的 `navigator.pop()` 会弹右栏栈（[LocalNavigator] 已在此换成
 * 右栏实例），无需为分栏额外写返回逻辑。
 *
 * @param modifier 由 [ListDetailPanes] 分配的右栏修饰符（`fillMaxSize`）。
 * @param backStack 右栏自己的返回栈。
 */
@Composable
private fun DetailPaneHost(
    modifier: Modifier,
    backStack: NavBackStack,
) {
    // 不需要为弹窗做任何"以右栏为基准居中"的处理：子页自带 AppSubPageScaffold（miuix Scaffold），
    // 而 OverlayDialog 默认 renderInRootScaffold = true ⇒ 弹窗与遮罩会渲染进这个 Scaffold，
    // 天然以右栏为基准居中、遮罩也只盖右栏。实测卡片中心 x=1518px = 右栏中心，左栏亮度不变。
    Box(modifier) {
        when (val route = backStack.lastOrNull()) {
            null, Route.Main -> DetailPaneEmptyHint()
            Route.ItemList -> ItemListScreen()
            Route.RecipeList -> RecipeListScreen()
            Route.NpcList -> NpcListScreen()
            is Route.NpcSchedule -> NpcScheduleScreen(npcId = route.npcId)
            Route.About -> AboutScreen()
            Route.License -> LicenseScreen()
            Route.ImageScaleSettings -> ImageScaleSettingsScreen()
        }
    }
}

/**
 * 单栏主体：顶栏 + 底栏 + 「主页 / 设置」两页 HorizontalPager。
 * 手机铺满整屏；大屏分栏时作为**左栏**（宽度由 [ListDetailPanes] 决定）。
 */
@Composable
private fun MainPaneShell() {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val appState = LocalAppSettings.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberAppBlurBackdrop()

    val currentPage = pagerState.currentPage // 0 = 主页，1 = 设置

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
                    if (isIos) {
                        // iOS 液态玻璃底栏：折射 / 高光 / 按住拖动切换 / 选中果冻弹跳（移植自 miuix demo）
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
                            modifier = if (blurActive) {
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
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
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
                        0 -> HomeContent(innerPadding, scrollBehavior)
                        else -> SettingsContent(innerPadding, scrollBehavior)
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
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
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

