// 奶牛镇百科 · MainScreen（v7 重构）
//
// 主要变更（v7）：
// - 左右滑动切换：使用 HorizontalPager 在「主页」与「设置」之间左右滑切；
//   子页（物品 / 配方 / NPC / 日程 / 关于）由 navigator.push 全屏覆盖，不参与切换。
//   即使「显示底栏」关闭，pager 仍挂在内容区，滑动照样可用。
// - 底栏模式修复：把 LocalNavigationBarDisplayMode 从内容区搬到 NavigationBar
//   的 `mode` 参数，使「仅图标 / 选中显示文字」真正生效。
// - 悬浮底栏：自绘 AppFloatingNavigationBar（miuix 默认 = 模糊胶囊，
//   iOS-like = 模糊圆角 + 图标+文字），通过 miuix drawBackdrop 加模糊，
//   不再依赖 miuix FloatingNavigationBar 的纯色背景。
// - 切页时重置 MiuixScrollBehavior 的 heightOffset / contentOffset，使顶栏
//   在新页面回到展开状态。

package com.nainiuzhen.wiki.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nainiuzhen.wiki.ui.components.AppTopAppBar
import com.nainiuzhen.wiki.ui.components.rememberAppBlurBackdrop
import com.nainiuzhen.wiki.ui.settings.SettingsContent
import com.nainiuzhen.wiki.utils.LocalAppSettings
import kotlinx.coroutines.launch
import kotlin.math.abs
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.FloatingToolbar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
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
                val homeSelected = currentPage == 0
                val settingsSelected = currentPage == 1
                val badge: (@Composable () -> Unit)? =
                    if (appState.showNavigationBadge) ({ NavigationBadgeDot() }) else null
                if (appState.useFloatingNavigationBar) {
                    AppFloatingNavigationBar(
                        backdrop = backdrop,
                        isIosLike = appState.floatingNavigationBarStyle == 1,
                        horizontalAlignment = when (appState.floatingNavigationBarPosition) {
                            1 -> Alignment.Start
                            2 -> Alignment.End
                            else -> Alignment.CenterHorizontally
                        },
                        items = listOf(
                            AppNavItem(
                                label = "主页",
                                icon = MiuixIcons.Home,
                                selected = homeSelected,
                                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                                badge = badge,
                            ),
                            AppNavItem(
                                label = "设置",
                                icon = MiuixIcons.Settings,
                                selected = settingsSelected,
                                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                                badge = badge,
                            ),
                        ),
                    )
                } else {
                    // 非悬浮底栏：把 mode 真正传给 NavigationBar，修复"仅图标/选中显示文字"无效问题
                    val mode = NavigationBarDisplayMode.entries.getOrElse(
                        abs(appState.navigationBarMode).coerceIn(0, 2),
                    ) { NavigationBarDisplayMode.IconAndText }
                    NavigationBar(mode = mode) {
                        NavigationBarItem(
                            selected = homeSelected,
                            onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                            icon = MiuixIcons.Home,
                            label = "主页",
                            badge = badge,
                        )
                        NavigationBarItem(
                            selected = settingsSelected,
                            onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                            icon = MiuixIcons.Settings,
                            label = "设置",
                            badge = badge,
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
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(MiuixTheme.colorScheme.primary, CircleShape),
    )
}

// —— 悬浮底栏（自绘，支持 miuix 默认风格 + 模糊 / iOS-like 风格）——

/** 悬浮底栏条目。 */
private data class AppNavItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
    val badge: (@Composable () -> Unit)? = null,
)

/** 未选中图标 / 文字的透明度。 */
private const val UNSELECTED_ALPHA: Float = 0.55f

/**
 * 应用悬浮底栏：替代 miuix `FloatingNavigationBar` 以支持背景模糊与 iOS-like 样式。
 * - 默认（[isIosLike] = false）：胶囊形（cornerRadius=50dp），仅图标 + 角标；开启模糊时背景采样 [backdrop] 形成毛玻璃。
 * - iOS-like（[isIosLike] = true）：圆角矩形（cornerRadius=28dp），图标 + 文字，等宽 items；同样支持模糊。
 */
@Composable
private fun AppFloatingNavigationBar(
    backdrop: LayerBackdrop?,
    isIosLike: Boolean,
    horizontalAlignment: Alignment.Horizontal,
    items: List<AppNavItem>,
) {
    val blurSupported = isRuntimeShaderSupported()
    val showBlur = backdrop != null && blurSupported
    val shape = if (isIosLike) RoundedCornerShape(28.dp) else RoundedCornerShape(50.dp)
    val barColor = MiuixTheme.colorScheme.surfaceContainer
    val translucent = barColor.copy(alpha = if (showBlur) 0.55f else 1f)
    val blurPx = with(LocalDensity.current) { 20.dp.toPx() }
    val hOutSide = if (isIosLike) 28.dp else 36.dp
    val navBarBottomPadding = WindowInsets.navigationBars
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()
        .calculateBottomPadding()
    val bottomInset = if (navBarBottomPadding > 0.dp) 26.dp + navBarBottomPadding else 36.dp
    val minHeight = if (isIosLike) 56.dp else 52.dp
    val innerHPadding = if (isIosLike) 8.dp else 12.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (horizontalAlignment == Alignment.Start) hOutSide else 0.dp,
                end = if (horizontalAlignment == Alignment.End) hOutSide else 0.dp,
            ),
    ) {
        Row(
            modifier = Modifier
                .selectableGroup()
                .padding(bottom = bottomInset)
                .defaultMinSize(minHeight = minHeight)
                .then(
                    if (backdrop != null && blurSupported) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { shape },
                            effects = {
                                blur(blurPx, blurPx)
                            },
                            onDrawSurface = { drawRect(translucent) },
                        )
                    } else {
                        Modifier.background(translucent, shape)
                    },
                )
                .dropShadow(
                    shape = shape,
                    shadow = Shadow(radius = 10.dp, color = Color.Black, alpha = 0.2f),
                )
                .padding(horizontal = innerHPadding)
                .align(horizontalAlignment)
                .pointerInput(Unit) {
                    detectTapGestures { /* 消费空白处点击，避免穿透到下层内容 */ }
                },
            horizontalArrangement = if (isIosLike) Arrangement.SpaceEvenly else Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val baseColor = MiuixTheme.colorScheme.onSurfaceContainer
                val tint = if (item.selected) baseColor else baseColor.copy(alpha = UNSELECTED_ALPHA)
                if (isIosLike) {
                    // iOS-like：图标 + 文字，等宽
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(
                                selected = item.selected,
                                onClick = item.onClick,
                                role = Role.Tab,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            )
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        BadgedBox(badge = { item.badge?.invoke() }) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = tint,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = tint,
                            maxLines = 1,
                        )
                    }
                } else {
                    // 默认：仅图标 + 角标，content-sized（胶囊视觉）
                    Column(
                        modifier = Modifier
                            .selectable(
                                selected = item.selected,
                                onClick = item.onClick,
                                role = Role.Tab,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            )
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        BadgedBox(badge = { item.badge?.invoke() }) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = tint,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
