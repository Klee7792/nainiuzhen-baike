package com.nainiuzhen.wiki.utils

import androidx.compose.runtime.Composable

/**
 * iOS 实现：状态栏样式暂用 Info.plist 静态配置（`UIStatusBarStyle` =
 * `UIStatusBarStyleDefault`，跟随系统深色模式自动切换图标深浅）。
 *
 * K/N cinterop 只导出 `UIApplication.statusBarStyle` 的 getter（弃用的 setter
 * 未生成），运行时动态切换暂不可行；应用内主题与系统不一致时状态栏可能错配，
 * 待后续经可靠途径（如 VC 覆写）再实装动态切换。
 */
@Composable
actual fun SetStatusBarLightIcons(light: Boolean) {
    // no-op：样式由 Info.plist 静态决定
}
