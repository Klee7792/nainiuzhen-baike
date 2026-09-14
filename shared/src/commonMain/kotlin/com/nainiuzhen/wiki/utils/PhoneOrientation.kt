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
 * Android 16/17 起，在 `≥sw600dp` 的大屏上系统会忽略该方向请求 → 开关在大屏自动失效，
 * 无需在业务侧特判（详见设计文档 §3.2 / §8.2）。
 */
@Composable
expect fun ApplyPhoneOrientation(allowLandscape: Boolean)
