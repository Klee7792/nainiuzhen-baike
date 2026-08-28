// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
//
// 从 miuix 示例 `component.effect.BgEffectBackground` 移植（仅 Phone、仅 OS3）。
// 关于页专用：OS3 动态背景 + 由 [bgModifier]（如 `Modifier.layerBackdrop(backdrop)`）
// 捕获到 backdrop，供前景模糊 Text 采样。

package com.nainiuzhen.wiki.ui.settings.about

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import kotlin.math.floor

/**
 * OS3 风格动态背景容器。
 *
 * @param dynamicBackground 是否启用颜色阶段动画（与 demo3 "Dynamic Background=on" 对应）。
 * @param isDark 是否使用暗色预设。
 * @param surface 兜底色（不支持 shader 或过渡瞬间显示）。
 * @param modifier 整体 Modifier（建议 `fillMaxSize`）。
 * @param bgModifier 背景层 Modifier——通常传 `Modifier.layerBackdrop(backdrop)`，
 *   让背景被前景模糊 Text 采样。
 * @param content 前景内容（居中显示的前景模糊标题等）。
 */
@Composable
internal fun BgEffectBackground(
    dynamicBackground: Boolean,
    isDark: Boolean,
    surface: Color,
    modifier: Modifier = Modifier,
    bgModifier: Modifier = Modifier,
    content: @Composable (BoxScope.() -> Unit),
) {
    if (!isRuntimeShaderSupported()) {
        Box(modifier = modifier, content = content)
        return
    }
    val painter = remember { BgEffectPainter() }
    val preset = remember(isDark) { BgEffectConfigs.getOS3Phone(isDark) }
    val colorStage = remember { Animatable(0f) }

    LaunchedEffect(dynamicBackground, preset) {
        if (!dynamicBackground) return@LaunchedEffect
        // OS3 预设 4 组调色板各不相同 → 启用颜色阶段插值动画
        val animatesColors = preset.colors1 !== preset.colors2 || preset.colors2 !== preset.colors3
        if (!animatesColors) return@LaunchedEffect

        var targetStage = floor(colorStage.value) + 1f
        while (isActive) {
            delay((preset.colorInterpPeriod * 500).toLong())
            colorStage.animateTo(
                targetValue = targetStage,
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 35f),
            )
            targetStage += 1f
        }
    }

    Box(
        modifier = modifier,
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .then(bgModifier)
                .bgEffectDraw(
                    painter = painter,
                    preset = preset,
                    isDark = isDark,
                    surface = surface,
                    playing = dynamicBackground,
                    colorStage = { colorStage.value },
                ),
        )
        content()
    }
}
