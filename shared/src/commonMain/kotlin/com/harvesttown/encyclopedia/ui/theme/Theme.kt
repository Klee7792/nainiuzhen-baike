package com.harvesttown.encyclopedia.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 应用主题封装：沿用 miuix 主题体系，仅根据深浅色切换 [ColorSchemeMode]，
 * 确保严格符合 miuix 设计语言。
 */
@Composable
fun AppTheme(
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    val controller = remember(isDark) {
        if (isDark) ThemeController(ColorSchemeMode.Dark) else ThemeController(ColorSchemeMode.Light)
    }
    MiuixTheme(controller = controller, content = content)
}
