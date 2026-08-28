package com.harvesttown.encyclopedia.utils

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 全局应用设置状态。由 [com.harvesttown.encyclopedia.App] 持有并通过 CompositionLocal 下发；
 * 设置页（[com.harvesttown.encyclopedia.ui.settings.SettingsScreen]）通过
 * [LocalUpdateAppSettings] 修改，并经由 [com.harvesttown.encyclopedia.utils.AppSettingsStore] 落盘。
 *
 * v5 变更（相对 v4）：
 * - 移除 `isDark`，新增 `colorMode`（0=系统/Auto、1=深色、2=浅色）与 `monet`（Monet 取色开关），
 *   二者组合成 6 态主题（前 3 态 Monet 关、后 3 态 Monet 开）。
 * - 新增 14 个开关（#3），全部在此集中声明并由 [AndroidAppSettingsStore] 持久化读写。
 */
data class AppState(
    // —— 主题（取代旧 isDark）——
    val colorMode: Int = 0, // 0=系统(Auto) 1=深色 2=浅色
    val monet: Boolean = false, // Monet 取色开关（与 colorMode 组合成 6 态）
    // —— v4 沿用 ——
    val enableBlur: Boolean = true,
    val enableSquircle: Boolean = true,
    val navTransitionStyle: Int = 0, // 0=MiuixDefault 1=Modal（pit#4，无 AOSP）
    val enableSwipeBack: Boolean = true,
    // —— v5 新增 14 开关（#3，全部真正接线）——
    val enableCornerClip: Boolean = true, // Enable Corner Clip
    val scrollEndHaptic: Boolean = false, // Enable Scroll End Haptic
    val pageUserScroll: Boolean = true, // Enable Page User Scroll
    val showTopAppBar: Boolean = true, // Show TopAppBar
    val topAppBarBlurStyle: Int = 0, // TopAppBar Blur Style (0=Gaussian 1=Progressive)
    val showNavigationBar: Boolean = true, // Show NavigationBar
    val showNavigationBadge: Boolean = true, // Show Navigation Badge
    val navigationBarMode: Int = 0, // NavigationBar Mode (0=IconAndText 1=IconOnly 2=IconWithSelectedLabel)
    val useFloatingNavigationBar: Boolean = false, // Use FloatingNavigationBar
    val showFloatingToolbar: Boolean = false, // Show FloatingToolbar
    val showFloatingActionButton: Boolean = false, // Show FloatingActionButton
    val enableDim: Boolean = false, // Enable Dim
    val blockInputDuringTransition: Boolean = false, // Block Input During Transition
)

val LocalAppSettings = compositionLocalOf { AppState() }

val LocalUpdateAppSettings = staticCompositionLocalOf<(AppState) -> Unit> {
    error("No AppSettings updater provided!")
}
