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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.nainiuzhen.wiki.ui.components.AppTopAppBar
import com.nainiuzhen.wiki.ui.components.liquid.IosLiquidGlassNavigationBar
import com.nainiuzhen.wiki.ui.components.rememberAppBlurBackdrop
import com.nainiuzhen.wiki.ui.settings.SettingsContent
import com.nainiuzhen.wiki.utils.LocalAppSettings
import kotlinx.coroutines.launch
import kotlin.math.abs
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
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 底栏宿主：2 个标签页——「主页」（3 个图鉴板块）与「设置」（完整设置页）。
 * 通过 HorizontalPager 支持左右滑动切换（不依赖底栏，子页不参与）。
 * 子页（物品 / 配方 / NPC / 日程 / 关于）通过 [com.nainiuzhen.wiki.ui.nav.Navigator.push] 全屏覆盖在本宿主之上。
 */
@Composable
fun MainScreen() {
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

