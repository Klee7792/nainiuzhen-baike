package com.nainiuzhen.wiki.ui.components

import androidx.compose.runtime.Composable
import com.nainiuzhen.wiki.utils.LocalAppSettings
import top.yukonga.miuix.kmp.basic.TextFieldColors
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 顶栏模糊生效时，搜索框背景的透明度（越低越能透出顶栏的模糊）。 */
const val SEARCH_FIELD_BLUR_ALPHA = 0.45f

/**
 * 搜索框配色：顶栏模糊生效时改为半透明背景，让顶栏的模糊透出；模糊关闭或设备不支持时
 * 保持默认实心外观（否则会出现"透明背景 + 无模糊"导致的文字难读）。
 *
 * 三大板块（物品 / 配方 / NPC）**共用此实现**，以保证搜索框视觉一致——不要在各页面
 * 再各自写一份 `searchFieldColors()` 或直接用 `Color.Transparent`。
 *
 * 判定条件与顶栏取 backdrop 的条件严格一致，见 [rememberAppBlurBackdrop]
 * （`!enableBlur || !isRuntimeShaderSupported()` 时返回 null）。
 */
@Composable
fun searchFieldColors(): TextFieldColors {
    val appState = LocalAppSettings.current
    val blurActive = appState.enableBlur && isRuntimeShaderSupported()
    val backgroundColor = MiuixTheme.colorScheme.secondaryContainer
    return TextFieldDefaults.textFieldColors(
        backgroundColor = if (blurActive) {
            backgroundColor.copy(alpha = SEARCH_FIELD_BLUR_ALPHA)
        } else {
            backgroundColor
        },
    )
}
