package com.nainiuzhen.wiki.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent

/**
 * iOS 实现：经 `UIApplication.statusBarStyle` 切状态栏图标深浅。
 *
 * [light] 语义与 Android 端一致 = 「背景是浅色」→ 配深色图标（DarkContent）。
 * 需要 Info.plist 设 `UIViewControllerBasedStatusBarAppearance = NO`，否则调用不生效。
 * 该 API 虽被苹果标记 deprecated，但各 iOS 版本仍然生效；本应用走 TrollStore 分发，
 * 不涉及上架审核，无弃用顾虑。
 */
@Composable
actual fun SetStatusBarLightIcons(light: Boolean) {
    DisposableEffect(light) {
        @Suppress("DEPRECATION")
        UIApplication.sharedApplication.statusBarStyle =
            if (light) UIStatusBarStyleDarkContent else UIStatusBarStyleLightContent
        onDispose { }
    }
}
