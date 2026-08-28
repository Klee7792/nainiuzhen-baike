// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
//
// 从 miuix 示例 `component.effect.BgEffectModifier` 移植（移除 DeviceType / isFullSize）。
// 在节点的 draw 中绘制 OS3 动态背景 brush；每帧用 withFrameNanos 推进 animTime。
// 关于页 isFullSize=true（占满 80% 高度），alpha 固定 1f。

package com.nainiuzhen.wiki.ui.settings.about

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 将 [painter] 的 OS3 brush 画到节点上：先铺 [surface] 兜底色，再叠加 brush，
 * 最后 `drawContent()` 让上层（如 `layerBackdrop` 之后的内容）正常绘制。
 */
internal fun Modifier.bgEffectDraw(
    painter: BgEffectPainter,
    preset: BgEffectConfig,
    isDark: Boolean,
    surface: Color,
    playing: Boolean,
    colorStage: () -> Float,
): Modifier = this then BgEffectElement(
    painter = painter,
    preset = preset,
    isDark = isDark,
    surface = surface,
    playing = playing,
    colorStage = colorStage,
)

private data class BgEffectElement(
    val painter: BgEffectPainter,
    val preset: BgEffectConfig,
    val isDark: Boolean,
    val surface: Color,
    val playing: Boolean,
    val colorStage: () -> Float,
) : ModifierNodeElement<BgEffectNode>() {

    override fun create(): BgEffectNode = BgEffectNode(
        painter = painter,
        preset = preset,
        isDark = isDark,
        surface = surface,
        playing = playing,
        colorStage = colorStage,
    )

    override fun update(node: BgEffectNode) {
        node.update(
            painter = painter,
            preset = preset,
            isDark = isDark,
            surface = surface,
            playing = playing,
            colorStage = colorStage,
        )
    }
}

private class BgEffectNode(
    private var painter: BgEffectPainter,
    private var preset: BgEffectConfig,
    private var isDark: Boolean,
    private var surface: Color,
    private var playing: Boolean,
    private var colorStage: () -> Float,
) : Modifier.Node(),
    DrawModifierNode {

    private var animationJob: Job? = null
    private var animTime: Float = 0f
    private var startOffset: Float = 0f

    override fun onAttach() {
        if (playing) startAnimation()
    }

    override fun onDetach() {
        animationJob?.cancel()
        animationJob = null
    }

    fun update(
        painter: BgEffectPainter,
        preset: BgEffectConfig,
        isDark: Boolean,
        surface: Color,
        playing: Boolean,
        colorStage: () -> Float,
    ) {
        this.painter = painter
        this.preset = preset
        this.isDark = isDark
        this.surface = surface
        this.colorStage = colorStage

        if (this.playing != playing) {
            this.playing = playing
            if (playing) {
                startAnimation()
            } else {
                animationJob?.cancel()
                animationJob = null
            }
        }
        invalidateDraw()
    }

    private fun startAnimation() {
        animationJob?.cancel()
        startOffset = animTime
        animationJob = coroutineScope.launch {
            val minDeltaNanos = 1_000_000_000L / 60L
            val origin = withFrameNanos { it }
            var lastEmit = origin
            while (isActive) {
                val now = withFrameNanos { it }
                if (now - lastEmit < minDeltaNanos) continue
                lastEmit = now
                animTime = startOffset + (now - origin) / 1_000_000_000f
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        // 1) 铺底色
        drawRect(surface)
        // 2) 动态 OS3 brush（isFullSize=true → 占用 80% 高度居中）
        val drawHeight = size.height * 0.8f
        painter.updateResolution(size.width, size.height)
        painter.updateBoundIfNeeded(drawHeight, size.height, size.width)
        painter.updatePresetIfNeeded(isDark)
        painter.updateColors(preset, colorStage())
        painter.updateAnimTime(animTime)
        painter.updatePointsAnim(animTime, preset)
        drawRect(painter.brush, alpha = 1f)
        // 3) 放行子内容
        drawContent()
    }
}
