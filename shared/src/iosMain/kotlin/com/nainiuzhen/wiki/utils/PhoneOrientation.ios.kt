package com.nainiuzhen.wiki.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.Foundation.NSUserDefaults

/**
 * iOS 实现：方向由「Info.plist 声明支持方向（含横屏）+ Swift 壳 AppDelegate 动态锁向」共同决定。
 *
 * - Info.plist 的 `UISupportedInterfaceOrientations` 声明**能力上限**（含左右横屏）；
 * - 实际允许的方向由 `iosApp.swift` 的 AppDelegate
 *   `application(_:supportedInterfaceOrientationsFor:)` 按 NSUserDefaults 键
 *   `allowPhoneLandscape` 返回（`.allButUpsideDown` / `.portrait`）；
 * - 本函数负责把 Compose 侧的开关值实时同步进 NSUserDefaults（键名与 Swift 侧约定一致），
 *   系统在设备旋转/布局变化时会重新查询，切换即刻生效。
 *
 * iPad（TARGETED_DEVICE_FAMILY 含 2）Info.plist 声明全方向且不受此键限制，恒自由旋转。
 */
@Composable
actual fun ApplyPhoneOrientation(allowLandscape: Boolean) {
    LaunchedEffect(allowLandscape) {
        NSUserDefaults.standardUserDefaults.setBool(allowLandscape, forKey = "allowPhoneLandscape")
    }
}
