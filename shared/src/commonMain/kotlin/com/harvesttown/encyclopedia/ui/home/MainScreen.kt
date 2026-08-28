package com.harvesttown.encyclopedia.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.harvesttown.encyclopedia.ui.settings.SettingsContent
import com.harvesttown.encyclopedia.utils.LocalAppSettings
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LocalNavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
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
 * 顶栏随内容滚动展开/折叠（变更点 #23，主页/设置默认展开）；并按设置显隐 TopAppBar / NavigationBar
 * 与底栏角标、底栏模式、浮动底栏、浮动按钮（变更点 #3）。
 */
@Composable
fun MainScreen() {
    var tab by remember { mutableStateOf(0) } // 0 = 主页，1 = 设置
    val appState = LocalAppSettings.current
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            if (appState.showTopAppBar) {
                SmallTopAppBar(
                    title = if (tab == 0) "奶牛镇百科" else "设置",
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        bottomBar = {
            if (appState.showNavigationBar) {
                NavigationBar(
                    modifier = if (appState.useFloatingNavigationBar) {
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    } else {
                        Modifier
                    },
                ) {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = MiuixIcons.Home,
                        label = "主页",
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = MiuixIcons.Settings,
                        label = "设置",
                        badge = if (appState.showNavigationBadge) {
                            {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(MiuixTheme.colorScheme.primary, CircleShape),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        },
        floatingActionButton = {
            if (appState.showFloatingActionButton) {
                FloatingActionButton(onClick = {}) {
                    Icon(
                        imageVector = MiuixIcons.Basic.ArrowRight,
                        contentDescription = "快捷操作",
                        tint = MiuixTheme.colorScheme.onPrimary,
                        modifier = Modifier.rotate(-90f),
                    )
                }
            }
        },
    ) { innerPadding ->
        val mode = NavigationBarDisplayMode.entries.getOrElse(
            abs(appState.navigationBarMode).coerceIn(0, 2),
        ) { NavigationBarDisplayMode.IconAndText }
        androidx.compose.runtime.CompositionLocalProvider(LocalNavigationBarDisplayMode provides mode) {
            when (tab) {
                0 -> HomeContent(innerPadding, scrollBehavior)
                1 -> SettingsContent(innerPadding, scrollBehavior)
            }
        }
    }
}
