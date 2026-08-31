// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
//
// 从 miuix 示例 `component.effect.BgEffectBackground` 移植（OS2/OS3、FullSize、alpha 驱动）。
// 关于页专用：OS3 动态背景 + 由 [bgModifier]（如 `Modifier.layerBackdrop(backdrop)`）
// 捕获到 backdrop，供前景模糊 Text 采样。
// 低端机（Android < 12，无 RuntimeShader）回退：用 Compose 动画渐变呈现 OS3
// 「色彩流动」背景，使关于页背景在 Redmi K40(API30？实际 API30 无 shader）等设备上也能看到变色。

package com.nainiuzhen.wiki.ui.settings.about

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.isSystemInDarkTheme
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.nainiuzhen.wiki.utils.LocalAppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.floor

/**
 * OS3 风格动态背景容器。
 *
 * @param dynamicBackground 是否启用颜色阶段动画。
 * @param isDark 是否使用暗色预设。
 * @param surface 兜底色（不支持 shader 或过渡瞬间显示）。
 * @param modifier 整体 Modifier（建议 `fillMaxSize`）。
 * @param bgModifier 背景层 Modifier——通常传 `Modifier.layerBackdrop(backdrop)`。
 * @param isFullSize 是否占满 80% 高度（否则 50%）。
 * @param effectBackground 是否绘制动态背景（false 时仅铺 surface）。
 * @param isOs3Effect 是否使用 OS3（否则 OS2）。
 * @param alpha 背景不透明度（draw 阶段读取，不触发重组）。传入 `{ 1f - scrollProgress }`
 *   即可实现「上拉变纯色、下拉恢复 OS3」：滚动时背景淡出、露出底层纯色 surface。
 *   content（前景内容）不受 alpha 影响。
 * @param content 前景内容（居中显示的前景模糊标题等）。
 */
@Composable
internal fun BgEffectBackground(
    dynamicBackground: Boolean,
    modifier: Modifier = Modifier,
    bgModifier: Modifier = Modifier,
    isFullSize: Boolean = false,
    effectBackground: Boolean = true,
    isOs3Effect: Boolean = true,
    alpha: () -> Float = { 1f },
    content: @Composable BoxScope.() -> Unit,
) {
    val appState = LocalAppSettings.current
    val isDark = when (appState.colorMode) {
        1 -> true
        2 -> false
        else -> isSystemInDarkTheme()
    }
    val surface = MiuixTheme.colorScheme.surface
    val deviceType = DeviceType.PHONE

    if (!isRuntimeShaderSupported()) {
        // 低端机（Android < 12，无 RuntimeShader）回退：用 Compose 动画渐变呈现 OS3
        // 「色彩流动」背景，使关于页背景在 Redmi K40 等设备上也能看到波浪变色。
        AnimatedGradientBox(modifier = modifier, isDark = isDark, alpha = alpha, content = content)
        return
    }
    val painter = remember(isOs3Effect) { BgEffectPainter(isOs3Effect) }
    val preset = remember(deviceType, isDark, isOs3Effect) {
        BgEffectConfig.get(deviceType, isDark, isOs3Effect)
    }
    val colorStage = remember { Animatable(0f) }

    LaunchedEffect(dynamicBackground, preset) {
        if (!dynamicBackground) return@LaunchedEffect
        // OS3 预设 4 组调色板各不相同 → 启用颜色阶段插值动画；OS2 三色相同 → 跳过。
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
                    deviceType = deviceType,
                    isDarkTheme = isDark,
                    surface = surface,
                    effectBackground = effectBackground,
                    isFullSize = isFullSize,
                    playing = dynamicBackground,
                    colorStage = { colorStage.value },
                    alpha = alpha,
                ),
        )
        content()
    }
}

/**
 * 不支持 RuntimeShader 时的「色彩流动」背景兜底：随时间循环偏移色相的线性渐变，
 * 让关于页背景在低端机上也呈现波浪变色（与 miuix demo 的 OS3 观感一致）。
 */
@Composable
private fun AnimatedGradientBox(
    modifier: Modifier,
    isDark: Boolean,
    alpha: () -> Float = { 1f },
    content: @Composable BoxScope.() -> Unit,
) {
    val phase = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (isActive) {
            phase.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 6000, easing = LinearEasing),
            )
            phase.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 6000, easing = LinearEasing),
            )
        }
    }
    val hue = (phase.value * 360f) % 360f
    val light = if (isDark) 0.4f else 0.62f
    val colors = listOf(
        Color.hsl(hue, 0.55f, light),
        Color.hsl((hue + 60f) % 360f, 0.55f, light),
        Color.hsl((hue + 120f) % 360f, 0.55f, light),
        Color.hsl((hue + 210f) % 360f, 0.55f, light),
    )
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(colors = colors))
                .graphicsLayer { this.alpha = alpha() },
        )
        content()
    }
}
