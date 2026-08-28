package com.harvesttown.encyclopedia.ui.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode as ComposeBlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harvesttown.encyclopedia.ui.nav.LocalNavigator
import com.harvesttown.encyclopedia.ui.nav.LocalSpriteRepository
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.PI
import kotlin.math.sin

/**
 * 关于子页（变更点 #37）：全屏独立页面，背景采用 OS3 风格动态海浪渐变（紫色 → 蓝 → 青），
 * 缓慢起伏流动；标题应用 OS3 Foreground Blur（前景模糊，默认参数），呈现毛玻璃质感。
 * 顶栏透明，使海浪背景透出。由设置页「关于」经 [com.harvesttown.encyclopedia.ui.nav.Route.About] 进入。
 *
 * 注意：Foreground Blur 依赖运行时着色器（Android 12+）。不支持的设备回退为纯色标题文本。
 */
@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    val sprite = LocalSpriteRepository.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberLayerBackdrop()
    val shaderSupported = isRuntimeShaderSupported()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "关于",
                color = Color.Transparent,
                titleColor = Color.White,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
                            tint = Color.White,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 背景层：被 backdrop 捕获，供前景模糊采样
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (shaderSupported) Modifier.layerBackdrop(backdrop) else Modifier),
            ) {
                Os3WaveBackground()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (shaderSupported) {
                    // OS3 前景模糊标题：字形内透出模糊后的海浪背景
                    Text(
                        text = "奶牛镇百科",
                        style = MiuixTheme.textStyles.title1,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .textureBlur(
                                backdrop = backdrop,
                                shape = RectangleShape,
                                blurRadiusX = 20f,
                                blurRadiusY = 20f,
                                colors = BlurDefaults.blurColors(),
                                contentBlendMode = ComposeBlendMode.DstIn,
                            ),
                    )
                } else {
                    Text(
                        text = "奶牛镇百科",
                        style = MiuixTheme.textStyles.title1,
                        color = Color.White,
                    )
                }
                Text(
                    text = "版本 v1.0.0 (${sprite.currentVersion()})",
                    style = MiuixTheme.textStyles.body1,
                    color = Color.White,
                )
                Text(
                    text = "奶牛镇物语全图鉴查询工具，支持物品、配方、NPC 资料与日程检索。",
                    style = MiuixTheme.textStyles.body2,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

/**
 * OS3 风格动态海浪背景：底层竖直渐变 + 三层正弦波随时间缓慢漂移。
 * 纯 Canvas 绘制，无需运行时着色器，所有设备均可显示。
 */
@Composable
private fun Os3WaveBackground() {
    val transition = rememberInfiniteTransition(label = "wave")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "waveT",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // 底层竖直渐变
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF6A3DE8),
                    Color(0xFF3D6CFF),
                    Color(0xFF19C6FF),
                ),
            ),
            size = size,
        )
        // 三层漂移正弦波
        val layers = listOf(
            WaveLayer(color = Color.White.copy(alpha = 0.10f), baseY = 0.50f, amp = 0.05f, freq = 1.5f, phase = 0.0f),
            WaveLayer(color = Color.White.copy(alpha = 0.08f), baseY = 0.62f, amp = 0.04f, freq = 1.2f, phase = 1.7f),
            WaveLayer(color = Color(0xFFB388FF).copy(alpha = 0.14f), baseY = 0.74f, amp = 0.06f, freq = 1.8f, phase = 3.1f),
        )
        layers.forEach { layer ->
            val phase = t * 2 * PI.toFloat() + layer.phase * PI.toFloat()
            val amp = h * layer.amp
            val yBase = h * layer.baseY
            val path = Path().apply {
                moveTo(0f, yBase)
                var x = 0f
                while (x <= w) {
                    val y = yBase + sin(x / w * 2 * PI.toFloat() * layer.freq + phase) * amp
                    lineTo(x, y)
                    x += 12f
                }
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(path = path, color = layer.color)
        }
    }
}

private data class WaveLayer(
    val color: Color,
    val baseY: Float,
    val amp: Float,
    val freq: Float,
    val phase: Float,
)
