// 奶牛镇百科 · 关于子页（v8 重写）
//
// 参照 miuix demo「AboutPage」：
//   - 顶栏默认隐藏（透明），背景 OS3 动态模糊透出；
//   - 上滑时背景渐隐为纯色、顶栏（标题"关于"）渐显（bug-v7 #4）。
//   - 中央磨砂标题「奶牛镇百科」随滚动淡出，下方卡片上滑贴顶栏。
//
// 实现：LazyColumn + logoSpacer 计算 scrollProgress；BlurredBar 在 scrollProgress==1f 时才模糊
// （新增 active 闸门，避免顶部就糊住 OS3）；纯色 surface 层随滚动淡入覆盖 OS3。

package com.nainiuzhen.wiki.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nainiuzhen.wiki.ui.components.BlurredBar
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.ui.nav.LocalSpriteRepository
import com.nainiuzhen.wiki.ui.settings.about.BgEffectBackground
import com.nainiuzhen.wiki.utils.APP_VERSION_NAME
import com.nainiuzhen.wiki.utils.LocalAppSettings
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
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

    val systemDark = isSystemInDarkTheme()
    val isDark = when (appState.colorMode) {
        1 -> true
        2 -> false
        else -> systemDark
    }

    val scrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()
    val backdrop = rememberLayerBackdrop()
    val surface = MiuixTheme.colorScheme.surface

    // scrollProgress：logoSpacer 滚出比例（0=顶部，1=完全收起）
    val scrollProgress by remember {
        derivedStateOf {
            when {
                lazyListState.firstVisibleItemIndex > 0 -> 1f
                else -> {
                    val spacer = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "logoSpacer" }
                    if (spacer != null && spacer.size > 0) {
                        (lazyListState.firstVisibleItemScrollOffset.toFloat() / spacer.size).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                }
            }
        }
    }
    val collapsed by remember { derivedStateOf { scrollProgress == 1f } }
    // 仅在完全收起时才模糊顶栏（避免顶部就糊住 OS3 背景）
    val blurActive by remember(backdrop) { derivedStateOf { backdrop != null && scrollProgress == 1f } }

    val logoAlpha = (1f - scrollProgress).coerceIn(0f, 1f)
    val solidAlpha = ((scrollProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
    val titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = solidAlpha)

    // "Logo Blend" 调色板（来自 miuix 示例 ForegroundBlurDemo）
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
            BlurredBar(backdrop, scrollBehavior, active = blurActive) {
                SmallTopAppBar(
                    title = "关于",
                    scrollBehavior = scrollBehavior,
                    color = if (blurActive) Color.Transparent else (if (collapsed) surface else Color.Transparent),
                    titleColor = titleColor,
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
        ) {
            // OS3 动态背景（滚动时淡出）
            // 注意：不要在这里再套一层 layerBackdrop(backdrop) —— 外层 Box 已经在录制同一份
            // backdrop，重复录制会触发 miuix 的 backdrop 重入异常，导致进入本页闪退（bug-v9 #4）。
            BgEffectBackground(
                dynamicBackground = true,
                isDark = isDark,
                surface = surface,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(logoAlpha),
            ) {}

            // 纯色背景（滚动时淡入，覆盖 OS3 → 顶栏显纯色）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(surface)
                    .alpha(solidAlpha),
            )

            // 中央磨砂标题 + 版本（滚动时淡出）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 90.dp)
                    .alpha(logoAlpha),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
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
                    Text(
                        text = "版本 $APP_VERSION_NAME (${sprite.currentVersion()})",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }

            // 滚动内容：logoSpacer 提供滚动锚点；下方卡片上滑贴顶栏
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                item(key = "logoSpacer") {
                    Spacer(Modifier.height(200.dp))
                }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Text(
                                text = "奶牛镇百科",
                                style = MiuixTheme.textStyles.title4,
                                color = MiuixTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "版本 $APP_VERSION_NAME (${sprite.currentVersion()})",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "《奶牛镇》游戏图鉴查询工具，涵盖物品大全、配方查询与 NPC 资料。",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
            }
        }
    }
}
