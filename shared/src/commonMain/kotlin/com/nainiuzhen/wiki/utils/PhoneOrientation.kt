package com.nainiuzhen.wiki.utils

import androidx.compose.runtime.Composable

/**
 * 按「手机横屏」设置应用屏幕方向（大屏适配方案 §8.3）。
 *
 * - `allowLandscape = false`（默认）：请求锁定竖屏 → 手机竖直持握不会因传感器翻成横屏。
 * - `allowLandscape = true`：请求跟随系统（UNSPECIFIED）→ 恢复自由旋转。
 *
 * 生效时机由 [androidx.compose.runtime.DisposableEffect] 保证：
 * 首次进入组合（App 启动）执行一次；[allowLandscape] 变化（用户切换开关）时**立即**再执行一次。
 *
 * **仅 Android 端有实现**（`setRequestedOrientation` 为平台 API）；非 Android 端为空实现。
 *
 * **「大屏自动失效」由业务侧显式判定，不依赖系统**：大屏设备（`smallestScreenWidthDp ≥ 600`，
 * 即 Android 判定 `sw600dp` 的那个值）恒请求 `UNSPECIFIED`，不锁方向。
 *
 * > ⚠️ 不要改回「靠系统忽略方向请求」的写法。实测（MuMu 模拟器 sw=726dp）系统**并未**忽略
 * > `SCREEN_ORIENTATION_PORTRAIT`，应用被硬锁竖屏，连强制旋转都无效 —— 大屏横屏布局因此完全无法触达。
 * > 详见设计文档 §8.2。
 */
@Composable
expect fun ApplyPhoneOrientation(allowLandscape: Boolean)
