package com.harvesttown.encyclopedia.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.harvesttown.encyclopedia.ui.nav.LocalNavigator
import com.harvesttown.encyclopedia.ui.nav.LocalSpriteRepository
import com.harvesttown.encyclopedia.utils.LocalAppSettings
import com.harvesttown.encyclopedia.utils.LocalUpdateAppSettings
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 设置页：深色模式开关、切片缓存清理（点击后清缓存并刷新显示大小）、版本与缓存大小展示。
 */
@Composable
fun SettingsScreen() {
    val sprite = LocalSpriteRepository.current
    val appState = LocalAppSettings.current
    val updateAppState = LocalUpdateAppSettings.current
    val navigator = LocalNavigator.current
    val scrollBehavior = MiuixScrollBehavior()
    var cacheSize by remember { mutableStateOf(sprite.cacheSizeBytes()) }
    val version = sprite.currentVersion()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "设置",
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding(), bottom = 12.dp),
        ) {
            item(key = "general") {
                SmallTitle(text = "通用")
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    SwitchPreference(
                        title = "深色模式",
                        checked = appState.isDark,
                        onCheckedChange = { updateAppState(appState.copy(isDark = it)) },
                    )
                    ArrowPreference(
                        title = "清理缓存",
                        summary = "切片缓存 ${formatSize(cacheSize)}",
                        onClick = {
                            sprite.clearCache()
                            cacheSize = sprite.cacheSizeBytes()
                        },
                    )
                }

                SmallTitle(text = "关于")
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    BasicComponent(
                        title = "版本",
                        summary = "v1.0.0 ($version)",
                    )
                }
            }
        }
    }
}

/** 缓存字节数格式化为 KB / MB 文本。 */
private fun formatSize(bytes: Long): String {
    val mb = bytes / 1024.0 / 1024.0
    return if (mb < 1) "${(bytes / 1024).toInt()} KB" else "%.2f MB".format(mb)
}
