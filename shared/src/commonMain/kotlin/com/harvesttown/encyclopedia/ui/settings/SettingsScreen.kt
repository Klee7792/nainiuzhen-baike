package com.harvesttown.encyclopedia.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.harvesttown.encyclopedia.ui.nav.LocalSpriteRepository
import com.harvesttown.encyclopedia.utils.LocalAppSettings
import com.harvesttown.encyclopedia.utils.LocalUpdateAppSettings
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 设置页（底栏第 2 页「设置」的主体，无自身顶栏/底栏，由 [com.harvesttown.encyclopedia.ui.home.MainScreen] 包裹）。
 *
 * 选项（中文，沿用 miuix 设计语言；偏好经 [LocalAppSettings]/[LocalUpdateAppSettings] 持久化，
 * 深色模式等重启后保留）：
 * - 通用：深色模式 / 启用圆角 / 启用模糊
 * - 导航：过渡动画（Miuix / AOSP）
 * - 数据：清理缓存 / 版本
 */
@Composable
fun SettingsContent(innerPadding: PaddingValues) {
    val sprite = LocalSpriteRepository.current
    val appState = LocalAppSettings.current
    val updateAppState = LocalUpdateAppSettings.current
    var cacheSize by remember { mutableStateOf(sprite.cacheSizeBytes()) }
    val version = sprite.currentVersion()

    LazyColumn(
        modifier = Modifier.fillMaxHeight(),
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
                SwitchPreference(
                    title = "启用圆角",
                    summary = "使用 squircle 圆角风格",
                    checked = appState.enableSquircle,
                    onCheckedChange = { updateAppState(appState.copy(enableSquircle = it)) },
                )
                SwitchPreference(
                    title = "启用模糊",
                    summary = "顶栏模糊效果",
                    checked = appState.enableBlur,
                    onCheckedChange = { updateAppState(appState.copy(enableBlur = it)) },
                )
            }

            SmallTitle(text = "导航")
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                OverlayDropdownPreference(
                    items = listOf("Miuix", "AOSP"),
                    selectedIndex = appState.navTransitionStyle,
                    title = "过渡动画",
                    onSelectedIndexChange = { updateAppState(appState.copy(navTransitionStyle = it)) },
                )
            }

            SmallTitle(text = "数据")
            Card(
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                ArrowPreference(
                    title = "清理缓存",
                    summary = "切片缓存 ${formatSize(cacheSize)}",
                    onClick = {
                        sprite.clearCache()
                        cacheSize = sprite.cacheSizeBytes()
                    },
                )
                BasicComponent(
                    title = "版本",
                    summary = "v1.0.0 ($version)",
                )
            }
        }
    }
}

/** 缓存字节数格式化为 KB / MB 文本。 */
private fun formatSize(bytes: Long): String {
    val mb = bytes / 1024.0 / 1024.0
    return if (mb < 1) "${(bytes / 1024).toInt()} KB" else "%.2f MB".format(mb)
}
