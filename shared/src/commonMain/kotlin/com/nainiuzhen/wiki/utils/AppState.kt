package com.nainiuzhen.wiki.utils

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 全局应用设置状态。由 [com.nainiuzhen.wiki.App] 持有并通过 CompositionLocal 下发；
 * 设置页（[com.nainiuzhen.wiki.ui.settings.SettingsScreen]）通过
 * [LocalUpdateAppSettings] 修改，并经由 [com.nainiuzhen.wiki.utils.AppSettingsStore] 落盘。
 *
 * v5 变更（相对于 v4）：
 * - 移除 `isDark`，新增 `colorMode`（0=系统/Auto、1=深色、2=浅色）与 `monet`（Monet 取色开关），
 *   两者组合成 6 套主题（前 3 套 Monet 关、后 3 套 Monet 开）。
 * - 新增 14 个开关（#3），全部在此集中声明并由 [AndroidAppSettingsStore] 持久化读写。
 */
data class AppState(
    // —— 主主题（取代旧 isDark）—— //
    val colorMode: Int = 0, // 0=系统(Auto) 1=深色 2=浅色
    val monet: Boolean = false, // Monet 取色开关（与 colorMode 组合成 6 套）
    // —— v4 沿用 —— //
    val enableBlur: Boolean = true,
    val enableSquircle: Boolean = true,
    val navTransitionStyle: Int = 0, // 0=MiuixDefault 1=Modal（git#4，无 AOSP）
    val enableSwipeBack: Boolean = true,
    // —— v5 新增 14 开关（#3，全部真正接续）—— //
    val enableCornerClip: Boolean = true, // Enable Corner Clip
    val scrollEndHaptic: Boolean = false, // Enable Scroll End Haptic
    val pageUserScroll: Boolean = true, // Enable Page User Scroll
    val showTopAppBar: Boolean = true, // Show TopAppBar
    val topAppBarBlurStyle: Int = 0, // TopAppBar Blur Style (0=Gaussian 1=Progressive)
    val showNavigationBar: Boolean = true, // Show NavigationBar
    val showNavigationBadge: Boolean = false, // Show Navigation Badge（默认关闭，避免遮住底栏 icon）
    val navigationBarMode: Int = 0, // NavigationBar Mode (0=IconAndText 1=IconOnly 2=IconWithSelectedLabel)
    val useFloatingNavigationBar: Boolean = false, // Use FloatingNavigationBar
    val showFloatingToolbar: Boolean = false, // Show FloatingToolbar
    val floatingToolbarPosition: Int = 0, // FloatingToolbar Position (0=End 1=Start 2=Center)
    val showFloatingActionButton: Boolean = false, // Show FloatingActionButton
    val floatingActionButtonPosition: Int = 0, // FAB Position (0=End 1=Start 2=Center)
    val enableDim: Boolean = false, // Enable Dim
    val blockInputDuringTransition: Boolean = false, // Block Input During Transition
    // —— v6 新增 —— //
    val floatingNavigationBarStyle: Int = 0, // FloatingNavigationBar Style (0=Default/Miuix 1=iOS)
    val floatingNavigationBarPosition: Int = 0, // FloatingNavigationBar Position (0=Center 1=Start 2=End)
)

/** 应用版本展示名（与 build.ps1 的 build number 同步：build N -> v1.(N/10).(N%10)）。 */
const val APP_VERSION_NAME = "v1.1.6"

/** 应用版本号（与 build.ps1 的 build number 同步）。 */
const val APP_VERSION_CODE = 16

val LocalAppSettings = compositionLocalOf { AppState() }

val LocalUpdateAppSettings = staticCompositionLocalOf<(AppState) -> Unit> {
    error("No AppSettings updater provided!")
}
