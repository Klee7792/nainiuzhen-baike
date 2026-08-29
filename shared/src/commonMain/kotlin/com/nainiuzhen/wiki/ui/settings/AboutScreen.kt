// 奶牛镇百科 · 关于子页（v15 参考 miuix demo AboutPage 重写）
//
// 变更点：
// - 返回箭头移入顶栏（Scaffold topBar 内），不再被全屏背景遮挡 → 可点击返回设置页。
// - 四个条目卡片加 textureBlur 毛玻璃（参考 demo 参数：blurRadius / noiseCoefficient / blend），
//   低端机（无 RuntimeShader）回退为 surfaceContainer 实色，避免纯白。
// - 滚动逻辑搬自 demo：scrollProgress 由 scrollState 推导，BgEffectBackground alpha = 1 - progress，
//   上拉→背景淡出变纯色列表页，下拉→恢复 OS3 动态背景；顶栏收起时变实色。
// - 「第三方开源协议」点击 push Route.License 子页；其余三项点击弹 toast「还没做」。

package com.nainiuzhen.wiki.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nainiuzhen.wiki.ui.components.BlurredBar
import com.nainiuzhen.wiki.ui.components.rememberAppBlurBackdrop
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.ui.nav.Route
import com.nainiuzhen.wiki.ui.settings.about.BgEffectBackground
import com.nainiuzhen.wiki.utils.APP_VERSION_CODE
import com.nainiuzhen.wiki.utils.APP_VERSION_NAME
import com.nainiuzhen.wiki.utils.LocalAppSettings
import com.nainiuzhen.wiki.utils.showToast
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    val appState = LocalAppSettings.current

    val systemDark = isSystemInDarkTheme()
    val isDark = when (appState.colorMode) {
        1 -> true
        2 -> false
        else -> systemDark
    }

    val surface = MiuixTheme.colorScheme.surface
    val backdrop = rememberAppBlurBackdrop()
    val scrollState = rememberScrollState()
    val scrollBehavior = MiuixScrollBehavior()

    // 滚动进度：从顶部滚动过 ~280dp 后视为完全收起（与 demo 的 logoSpacer 思路一致）。
    val densityScale = LocalDensity.current.density
    val collapseThresholdPx = 280f * densityScale
    val scrollProgressProvider = { (scrollState.value / collapseThresholdPx).coerceIn(0f, 1f) }
    val collapsed by remember { derivedStateOf { scrollProgressProvider() >= 0.999f } }
    val blurActive by remember(backdrop) { derivedStateOf { backdrop != null && scrollProgressProvider() >= 0.999f } }

    Scaffold(
        topBar = {
            // 收起或 OS3 背景淡出时，顶栏变为实色（普通列表页观感）；顶部展开并 OS3 可见时透明。
            val barColor = if (blurActive) {
                Color.Transparent
            } else {
                if (collapsed) surface else Color.Transparent
            }
            BlurredBar(backdrop = backdrop, scrollBehavior = scrollBehavior, active = blurActive) {
                SmallTopAppBar(
                    title = "关于",
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = "返回",
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    },
                )
            }
        },
    ) { _ ->
        // 底层实色 surface：OS3 背景(alpha→0)淡出后露出，即「上拉变纯色」的普通列表页。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(surface)
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
        ) {
            BgEffectBackground(
                dynamicBackground = true,
                isDark = isDark,
                surface = surface,
                modifier = Modifier.fillMaxSize(),
                // 修复 v15 闪退：BgEffectBackground 内部不要再套 layerBackdrop，因为外层 Box
                // 已经通过 .layerBackdrop(backdrop) 捕获整屏内容；内层再套同一个 backdrop 会导致
                // LayerBackdropNode 在录制中尝试 beginRecording 第二次，抛出
                // "Recording currently in progress - missing #endRecording() call"。
                // 本页前景没有需要采样背景的模糊 Text，去掉内层 layerBackdrop 不影响样式。
                bgModifier = Modifier,
                alpha = { 1f - scrollProgressProvider() },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(top = 96.dp, bottom = 24.dp)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                ) {
                    // 应用图标占位（圆角矩形 + 主题色文字）
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "奶",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A90E2),
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "奶牛镇百科",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "$APP_VERSION_NAME ($APP_VERSION_CODE)",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(48.dp))

                    AboutCard(backdrop = backdrop) {
                        AboutRow(
                            title = "查看源码",
                            summary = "GitHub",
                            onClick = { showToast("还没做") },
                        )
                        HorizontalDividerToken()
                        AboutRow(
                            title = "加入群组",
                            summary = "Telegram",
                            onClick = { showToast("还没做") },
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    AboutCard(backdrop = backdrop) {
                        AboutRow(
                            title = "开源协议",
                            summary = "Apache-2.0",
                            onClick = { showToast("还没做") },
                        )
                        HorizontalDividerToken()
                        AboutRow(
                            title = "第三方开源协议",
                            summary = null,
                            onClick = { navigator.push(Route.License) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutCard(
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop?,
    content: @Composable () -> Unit,
) {
    // 参考 demo AboutPage 的卡片参数：textureBlur 毛玻璃；无 RuntimeShader（低端机）时退回实色，
    // 避免「纯白」——与 demo 的 cardBlend / surfaceContainer 兜底一致。
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (backdrop != null) {
                    Modifier.textureBlur(
                        backdrop = backdrop,
                        shape = RoundedCornerShape(16.dp),
                        blurRadius = 60f,
                        noiseCoefficient = BlurDefaults.NoiseCoefficient,
                        colors = BlurDefaults.blurColors(
                            blendColors = listOf(
                                BlendColorEntry(
                                    color = MiuixTheme.colorScheme.surface.copy(alpha = 0.6f),
                                ),
                            ),
                        ),
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.defaultColors(
            if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer,
            Color.Transparent,
        ),
    ) {
        content()
    }
}

@Composable
private fun AboutRow(
    title: String,
    summary: String?,
    onClick: () -> Unit,
) {
    BasicComponent(
        title = title,
        summary = null,
        endActions = {
            if (summary != null) {
                Text(
                    text = summary,
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(end = 2.dp),
                )
            }
            Icon(
                imageVector = MiuixIcons.Basic.ArrowRight,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        },
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun HorizontalDividerToken() {
    HorizontalDivider(
        color = MiuixTheme.colorScheme.dividerLine,
        thickness = 0.5.dp,
    )
}
