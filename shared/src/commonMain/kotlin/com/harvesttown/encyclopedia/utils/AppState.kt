package com.harvesttown.encyclopedia.utils

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 全局应用设置状态。由 [com.harvesttown.encyclopedia.App] 持有并通过 CompositionLocal 下发；
 * 设置页（[com.harvesttown.encyclopedia.ui.settings.SettingsScreen]）通过
 * [LocalUpdateAppSettings] 修改。
 *
 * @param isDark 是否深色模式。
 * @param enableBlur 是否启用顶栏模糊（预留开关，顶栏默认用实色 surface）。
 * @param enableSquircle 是否启用 squircle 圆角。
 * @param navTransitionStyle 导航转场样式（0=Miuix 默认，1=AOSP）。
 * @param enableSwipeBack 是否启用滑动返回手势。
 */
data class AppState(
    val isDark: Boolean = false,
    val enableBlur: Boolean = true,
    val enableSquircle: Boolean = true,
    val navTransitionStyle: Int = 0,
    val enableSwipeBack: Boolean = true,
)

val LocalAppSettings = compositionLocalOf { AppState() }

val LocalUpdateAppSettings = staticCompositionLocalOf<(AppState) -> Unit> {
    error("No AppSettings updater provided!")
}
