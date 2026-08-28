package com.harvesttown.encyclopedia.utils

import androidx.compose.runtime.Composable

/**
 * 跨平台设置状态栏图标颜色：
 * - `light = true`：状态栏背景为浅色 → 图标显示为深色（用于浅色主题）。
 * - `light = false`：状态栏背景为深色 → 图标显示为浅色（用于深色主题）。
 *
 * Android 端实际修改 [android.view.Window] 的 appearance；非 Android 端为空实现。
 */
@Composable
expect fun SetStatusBarLightIcons(light: Boolean)
