package com.harvesttown.encyclopedia.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.harvesttown.encyclopedia.ui.settings.SettingsContent
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings

/**
 * 底栏宿主：2 个标签页——「主页」（3 个图鉴板块）与「设置」（完整设置页）。
 * 子页（物品 / 配方 / NPC / 日程）通过 [navigator.push] 全屏覆盖在本宿主之上。
 */
@Composable
fun MainScreen() {
    var tab by remember { mutableStateOf(0) } // 0 = 主页，1 = 设置
    Scaffold(
        topBar = {
            SmallTopAppBar(title = if (tab == 0) "奶牛镇百科" else "设置")
        },
        bottomBar = {
            NavigationBar {
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
                )
            }
        },
    ) { innerPadding ->
        when (tab) {
            0 -> HomeContent(innerPadding)
            1 -> SettingsContent(innerPadding)
        }
    }
}
