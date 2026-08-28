package com.harvesttown.encyclopedia.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import com.harvesttown.encyclopedia.ui.components.AppTopAppBar
import com.harvesttown.encyclopedia.ui.components.rememberAppBlurBackdrop
import com.harvesttown.encyclopedia.ui.settings.SettingsContent
import com.harvesttown.encyclopedia.utils.LocalAppSettings
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.FloatingToolbar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LocalNavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs

/**
 * 底栏宿主：2 个标签页——「主页」（3 个图鉴板块）与「设置」（完整设置页）。
 * 子页（物品 / 配方 / NPC / 日程 / 关于）通过 [navigator.push] 全屏覆盖在本宿主之上。
 *
 * v6 变更：
 * - 顶栏改为 [TopAppBar]（带 largeTitle），支持展开/折叠 + 高斯/渐进模糊。
 * - 底栏支持普通 [NavigationBar] 与 [FloatingNavigationBar] 胶囊，并跟随 Style/Position 设置。
 * - 悬浮工具栏 / 悬浮按钮根据 Position 设置显示在底部不同位置。
 * - 内容区挂载 [Modifier.layerBackdrop] 供顶栏模糊采样。
 */
@Composable
fun MainScreen() {
    var tab by remember { mutableStateOf(0) } // 0 = 主页，1 = 设置
    val appState = LocalAppSettings.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberAppBlurBackdrop()

    Scaffold(
        topBar = {
            AppTopAppBar(
                title = if (tab == 0) "奶牛镇百科" else "设置",
                largeTitle = if (tab == 0) "奶牛镇百科" else "设置",
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
            )
        },
        bottomBar = {
            if (appState.showNavigationBar) {
                val homeSelected = tab == 0
                val settingsSelected = tab == 1
                val badge: (@Composable () -> Unit)? =
                    if (appState.showNavigationBadge) ({ NavigationBadgeDot() }) else null
                if (appState.useFloatingNavigationBar) {
                    val style = appState.floatingNavigationBarStyle
                    val alignment = when (appState.floatingNavigationBarPosition) {
                        1 -> Alignment.Start
                        2 -> Alignment.End
                        else -> Alignment.CenterHorizontally
                    }
                    FloatingNavigationBar(
                        horizontalAlignment = alignment,
                        cornerRadius = if (style == 1) 28.dp else 50.dp,
                        horizontalOutSidePadding = if (style == 1) 28.dp else 36.dp,
                    ) {
                        FloatingNavigationBarItem(
                            selected = homeSelected,
                            onClick = { tab = 0 },
                            icon = MiuixIcons.Home,
                            label = "主页",
                            badge = badge,
                        )
                        FloatingNavigationBarItem(
                            selected = settingsSelected,
                            onClick = { tab = 1 },
                            icon = MiuixIcons.Settings,
                            label = "设置",
                            badge = badge,
                        )
                    }
                } else {
                    NavigationBar {
                        NavigationBarItem(
                            selected = homeSelected,
                            onClick = { tab = 0 },
                            icon = MiuixIcons.Home,
                            label = "主页",
                            badge = badge,
                        )
                        NavigationBarItem(
                            selected = settingsSelected,
                            onClick = { tab = 1 },
                            icon = MiuixIcons.Settings,
                            label = "设置",
                            badge = badge,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        val mode = NavigationBarDisplayMode.entries.getOrElse(
            abs(appState.navigationBarMode).coerceIn(0, 2),
        ) { NavigationBarDisplayMode.IconAndText }
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalNavigationBarDisplayMode provides mode,
                ) {
                    when (tab) {
                        0 -> HomeContent(innerPadding, scrollBehavior)
                        1 -> SettingsContent(innerPadding, scrollBehavior)
                    }
                }
            }
            // 悬浮工具栏 / 悬浮按钮（独立于模糊采样层之外）
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
                            imageVector = MiuixIcons.Basic.ArrowRight,
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
                            imageVector = MiuixIcons.Basic.ArrowRight,
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
