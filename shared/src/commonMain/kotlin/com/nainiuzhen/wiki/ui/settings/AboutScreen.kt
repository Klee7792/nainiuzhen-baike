// 奶牛镇百科 · 关于子页（v7 重写）
//
// 参照 miuix demo3「Foreground Blur Miuix Demo」：
//   Effect Variant = OS3
//   BlendMode = Logo Blend
//   Dynamic Background = on
//   Blur Radius = 200
//   Noise = 0.0044
//   Brightness = 0
//   Contrast = 1
//   Saturation = 1
//   中央文字 = "奶牛镇百科"
//
// 实现：OS3 动态背景（runtime shader，4 个动画点 + 调色板循环）→ 由
// `Modifier.layerBackdrop(backdrop)` 捕获到 LayerBackdrop → 居中文字
// 通过 `Modifier.textureBlur(..., contentBlendMode = DstIn)` 取该 backdrop
// 的强烈模糊，以文字字形作为蒙版呈现"磨砂玻璃字"效果。
//
// 顶栏透明 + 白字，使 OS3 背景透出。不支持 RuntimeShader 的设备回退为
// 纯色背景 + 白字（见 `BgEffectBackground` 与 `textureBlur` 自身对
// `isRuntimeShaderSupported()` 的判断）。

package com.nainiuzhen.wiki.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.ui.nav.LocalSpriteRepository
import com.nainiuzhen.wiki.ui.settings.about.BgEffectBackground
import com.nainiuzhen.wiki.utils.LocalAppSettings
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.graphics.BlendMode as ComposeBlendMode

@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    val sprite = LocalSpriteRepository.current
    val appState = LocalAppSettings.current

    // 与 App.kt 一致：根据色彩模式解析当前是否为深色（用于选择 OS3 预设）
    val systemDark = isSystemInDarkTheme()
    val isDark = when (appState.colorMode) {
        1 -> true
        2 -> false
        else -> systemDark
    }

    val backdrop = rememberLayerBackdrop()
    val surface = MiuixTheme.colorScheme.surface

    // "Logo Blend" 调色板（来自 miuix 示例 ForegroundBlurDemo）：3 个 BlendColorEntry，
    // ColorDodge/Burn + LinearLight + Lab，按深浅主题二选一。
    val logoBlend = if (isDark) {
        listOf(
            BlendColorEntry(Color(0xe6a1a1a1), BlurBlendMode.ColorDodge),
            BlendColorEntry(Color(0x4de6e6e6), BlurBlendMode.LinearLight),
            BlendColorEntry(Color(0xff1af500), BlurBlendMode.Lab),
        )
    } else {
        listOf(
            BlendColorEntry(Color(0xcc4a4a4a), BlurBlendMode.ColorBurn),
            BlendColorEntry(Color(0xff4f4f4f), BlurBlendMode.LinearLight),
            BlendColorEntry(Color(0xff1af200), BlurBlendMode.Lab),
        )
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "关于",
                color = Color.Transparent,
                titleColor = Color.White,
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
        BgEffectBackground(
            dynamicBackground = true,
            isDark = isDark,
            surface = surface,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            bgModifier = Modifier.layerBackdrop(backdrop),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // 前景模糊标题：text 字形作为 DstIn 蒙版，透出强烈模糊（200dp）
                    // 的 OS3 背景 + Logo Blend 调色。
                    Text(
                        text = "奶牛镇百科",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .textureBlur(
                                backdrop = backdrop,
                                shape = RectangleShape,
                                blurRadiusX = 200f,
                                blurRadiusY = 200f,
                                noiseCoefficient = 0.0044f,
                                colors = BlurDefaults.blurColors(
                                    blendColors = logoBlend,
                                    brightness = 0f,
                                    contrast = 1f,
                                    saturation = 1f,
                                ),
                                contentBlendMode = ComposeBlendMode.DstIn,
                            ),
                    )
                    Spacer(Modifier.height(20.dp))
                    // 版本信息（非模糊，白色半透明，保留信息）
                    Text(
                        text = "版本 v1.0.0 (${sprite.currentVersion()})",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}
