package com.nainiuzhen.wiki.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 应用主题封装：沿用 miuix 主题体系，根据 `colorMode` + `monet` 映射 6 态 [ColorSchemeMode]
 * （前 3 态 Monet 关：系统/深色/浅色；后 3 态 Monet 开：Monet系统/Monet深色/Monet浅色），
 * 严格符合 miuix 设计语言。
 *
 * @param colorMode 0=系统(Auto) 1=深色 2=浅色。
 * @param monet 是否启用 Monet 动态取色（与 [colorMode] 叠加成 6 态）。
 * @param monetSeed 手动取色种子（ARGB）；0 = 不指定 —— Android 走系统壁纸动态取色，
 *   iOS 无壁纸取色 API、回落 miuix 默认种子。非 0 时两端都经 material-color-utilities
 *   从该种子生成整套色板（见 miuix ThemeController.keyColor）。
 */
@Composable
fun AppTheme(
    colorMode: Int,
    monet: Boolean,
    monetSeed: Int = 0,
    content: @Composable () -> Unit,
) {
    val colorSchemeMode = when {
        monet && colorMode == 1 -> ColorSchemeMode.MonetDark
        monet && colorMode == 2 -> ColorSchemeMode.MonetLight
        monet -> ColorSchemeMode.MonetSystem
        colorMode == 1 -> ColorSchemeMode.Dark
        colorMode == 2 -> ColorSchemeMode.Light
        else -> ColorSchemeMode.System
    }
    val keyColor = if (monetSeed != 0) Color(monetSeed) else null
    val controller = remember(colorMode, monet, keyColor) {
        ThemeController(colorSchemeMode = colorSchemeMode, keyColor = keyColor)
    }
    MiuixTheme(controller = controller, content = content)
}
