// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
//
// 从 miuix 示例 `component.effect.BgEffectBackground` 移植（仅 Phone、仅 OS3）。
// 关于页专用：OS3 动态背景 + 由 [bgModifier]（如 `Modifier.layerBackdrop(backdrop)`）
// 捕获到 backdrop，供前景模糊 Text 采样。

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
 * @param alpha 背景不透明度（draw 阶段读取，不触发重组）。传入 `{ 1f - scrollProgress }`
 *   即可实现「上拉变纯色、下拉恢复 OS3」：滚动时背景淡出、露出底层纯色 surface。
 *   content（前景内容）不受 alpha 影响。
 * @param content 前景内容（居中显示的前景模糊标题等）。
 */
@Composable
internal fun BgEffectBackground(
    dynamicBackground: Boolean,
    isDark: Boolean,
    surface: Color,
    modifier: Modifier = Modifier,
    bgModifier: Modifier = Modifier,
    alpha: () -> Float = { 1f },
    content: @Composable (BoxScope.() -> Unit),
) {
    if (!isRuntimeShaderSupported()) {
        // 低端机（Android < 12，无 RuntimeShader）回退：用 Compose 动画渐变呈现 OS3
        // 「色彩流动」背景，使关于页背景在 Redmi K40(API30) 等设备上也能看到波浪变色，
        // 而非纯白（v12 修复：此前此处直接返回空 Box → 背景纯白，仅文字有色彩）。
        AnimatedGradientBox(modifier = modifier, isDark = isDark, alpha = alpha, content = content)
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
                )
                .graphicsLayer { this.alpha = alpha() },
        )
        content()
    }
}

/**
 * 不支持 RuntimeShader 时的「色彩流动」背景兜底：随时间循环偏移色相的线性渐变，
 * 让关于页背景在低端机上也呈现波浪变色（与 miuix demo 的 OS3 观感一致）。
 *
 * 仅在 [BgEffectBackground] 判定 `!isRuntimeShaderSupported()` 时调用；支持 RuntimeShader
 * 的设备走 OS3 shader 路径（[bgEffectDraw]），不会走到这里。
 */
@Composable
private fun AnimatedGradientBox(
    modifier: Modifier,
    isDark: Boolean,
    alpha: () -> Float = { 1f },
    content: @Composable BoxScope.() -> Unit,
) {
    // 用 Animatable + 往返循环模拟 InfiniteTransition 的「色相往返」效果，
    // 避开各 Compose 版本间 animateFloat 扩展函数签名不一致的坑（v12 修复）。
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
    // 色相随时间在 0~360 间往返循环 → 背景像波浪一样连续变色。
    val hue = (phase.value * 360f) % 360f
    val light = if (isDark) 0.4f else 0.62f
    val colors = listOf(
        Color.hsl(hue, 0.55f, light),
        Color.hsl((hue + 60f) % 360f, 0.55f, light),
        Color.hsl((hue + 120f) % 360f, 0.55f, light),
        Color.hsl((hue + 210f) % 360f, 0.55f, light),
    )
    // 背景层（渐变）与前景 content 拆分为两层：alpha 只作用于背景层，
    // 滚动淡出背景时（alpha→0）露出底层纯色 surface，前景内容（图标/标题/卡片）保持可见。
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
