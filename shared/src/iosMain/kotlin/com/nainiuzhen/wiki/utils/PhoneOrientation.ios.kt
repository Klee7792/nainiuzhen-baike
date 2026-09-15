package com.nainiuzhen.wiki.utils

import androidx.compose.runtime.Composable

/**
 * iOS 实现：方向由 Info.plist 的 `UISupportedInterfaceOrientations` 全局声明
 * （v38 首发仅竖屏 + 倒置竖屏），Compose 侧无需逐帧请求。
 *
 * Android 端「大屏（≥600dp）不锁方向」的语义在 iOS 上对应 iPad（TARGETED_DEVICE_FAMILY
 * 含 2 的构建），待后续版本用 size class 判定后放开 iPad 全方向。
 */
@Composable
actual fun ApplyPhoneOrientation(allowLandscape: Boolean) {
    // no-op：见类注释
}
