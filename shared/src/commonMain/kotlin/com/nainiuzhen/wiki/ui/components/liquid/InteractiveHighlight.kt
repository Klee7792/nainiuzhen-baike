// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// 移植自 miuix 示例 component.animation.InteractiveHighlight（用于 iOS 液态玻璃底栏）。
// 仅修改 package；逻辑与 demo 保持一致。

package com.nainiuzhen.wiki.ui.components.liquid

// Adapted from Kyant0/AndroidLiquidGlass — https://github.com/Kyant0/AndroidLiquidGlass (Apache 2.0).

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.RuntimeShader
import top.yukonga.miuix.kmp.blur.asBrush
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported

internal class InteractiveHighlight(
    private val animationScope: CoroutineScope,
    private val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset },
) {

    private val pressProgressAnimationSpec = spring(0.5f, 300f, 0.001f)
    private val positionAnimationSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)

    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val positionAnimation = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)
    private var startPosition = Offset.Zero

    val modifier: Modifier = Modifier.drawWithContent {
        val progress = pressProgressAnimation.value
        if (progress > 0f) {
            drawRect(
                color = Color.White.copy(alpha = 0.06f * progress),
                blendMode = BlendMode.Plus,
            )
            val pos = position(size, positionAnimation.value)
            val radius = (size.minDimension * 1.2f).coerceAtLeast(1f)
            val center = Offset(
                x = pos.x.coerceIn(0f, size.width),
                y = pos.y.coerceIn(0f, size.height),
            )
            // 与预热（ShaderWarmup）调用同一份光斑绘制实现，shader 字符串只此一处。
            drawPressSpot(progress = progress, center = center, radius = radius)
        }
        drawContent()
    }

    val gestureModifier: Modifier = Modifier.pointerInput(animationScope) {
        inspectDragGestures(
            onDragStart = { down ->
                startPosition = down.position
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
                    launch { positionAnimation.snapTo(startPosition) }
                }
            },
            onDragEnd = { release() },
            onDragCancel = { release() },
        ) { change, _ ->
            animationScope.launch { positionAnimation.snapTo(change.position) }
        }
    }

    // On release the spot springs back to the press-down point while the press fades out.
    private fun release() {
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
        }
    }
}

// Press spot: solid within half the radius, smoothstep falloff toward the edge.
private const val SPOT_SHADER = """
    layout(color) uniform half4 color;
    uniform float radius;
    uniform float2 position;

    half4 main(float2 coord) {
        float dist = distance(coord, position);
        float intensity = smoothstep(radius, radius * 0.5, dist);
        return color * intensity;
    }"""

/**
 * 光斑 RuntimeShader（文件级单例）：shader 字符串即缓存 key，全实例共享同一份（避免每实例各编译一次）；
 * uniform 每次绘制前重设，无状态残留。[drawPressSpot] 与 [InteractiveHighlight] 共用，不复制 shader 字符串。
 */
private val spotShader: RuntimeShader? =
    if (isRuntimeShaderSupported()) RuntimeShader(SPOT_SHADER) else null

/**
 * 绘制按压光斑（高亮按下位置）。抽为 internal 顶层函数，供 [InteractiveHighlight.modifier]
 * 与预热（ShaderWarmup）调用**同一实现**——shader 字符串只此一份，绝不复制，否则缓存 key 漂移。
 *
 * @param progress 按压强度 0..1（决定光斑透明度）。
 * @param center 光斑中心。
 * @param radius 光斑半径。
 */
internal fun DrawScope.drawPressSpot(progress: Float, center: Offset, radius: Float) {
    val spotColor = Color.White.copy(alpha = 0.12f * progress)
    if (spotShader != null) {
        spotShader.setColorUniform("color", spotColor)
        spotShader.setFloatUniform("radius", radius)
        spotShader.setFloatUniform("position", center.x, center.y)
        drawRect(brush = spotShader.asBrush(), blendMode = BlendMode.Plus)
    } else {
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to spotColor,
                    0.5f to spotColor,
                    1.0f to Color.White.copy(alpha = 0f),
                ),
                center = center,
                radius = radius,
            ),
            blendMode = BlendMode.Plus,
        )
    }
}
