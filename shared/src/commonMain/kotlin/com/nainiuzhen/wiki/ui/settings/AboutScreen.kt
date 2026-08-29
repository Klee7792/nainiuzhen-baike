// 奶牛镇百科 · 关于子页（对照 miuix demo AboutPage 重写）
//
// 变更点：
// - 用真实 app 图标（assets/ic_launcher.png，经 LocalSpriteRepository.getAssetImage 解码）替换临时"奶"字；不可走 composeResources(Res.*)，否则 APK 无该资源会崩溃。
// - 标题加 textureBlur + DstIn 前景模糊，呈现 demo 同款发光/玻璃文字效果。
// - 改用 LazyColumn + logoSpacer 实现滚动与吸附顶栏；原 Column.verticalScroll 导致整体一起滚动。
// - 卡片使用 ColorBlendToken 玻璃混合参数，并对齐 demo 的 blurRadius/noise。
// - 降低整体顶部留白，使图标/标题视觉居中；背景随滚动淡出为 surface 纯色。
// - 保留 #16 崩溃修复：backdrop 仅由 BgEffectBackground 内 Spacer 捕获一次，卡片/标题是兄弟节点而非子节点。

package com.nainiuzhen.wiki.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode as ComposeBlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
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
import com.nainiuzhen.wiki.ui.settings.about.ColorBlendToken
import com.nainiuzhen.wiki.utils.APP_VERSION_CODE
import com.nainiuzhen.wiki.utils.APP_VERSION_NAME
import com.nainiuzhen.wiki.utils.LocalAppSettings
import com.nainiuzhen.wiki.utils.showToast
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import com.nainiuzhen.wiki.ui.nav.LocalSpriteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
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
    val scrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()

    val scrollProgress by remember {
        derivedStateOf {
            when {
                lazyListState.firstVisibleItemIndex > 0 -> 1f
                else -> {
                    val spacer = lazyListState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.key == "logoSpacer" }
                    if (spacer != null && spacer.size > 0) {
                        (lazyListState.firstVisibleItemScrollOffset.toFloat() / spacer.size)
                            .coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                }
            }
        }
    }

    val collapsed by remember { derivedStateOf { scrollProgress >= 0.999f } }
    val blurActive by remember(backdrop) { derivedStateOf { backdrop != null && scrollProgress >= 0.999f } }

    Scaffold(
        topBar = {
            val barColor = if (blurActive) {
                Color.Transparent
            } else {
                if (collapsed) surface else Color.Transparent
            }
            val titleColor = MiuixTheme.colorScheme.onSurface.copy(
                alpha = ((scrollProgress - 0.35f) / 0.65f).coerceIn(0f, 1f),
            )
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
                active = blurActive,
            ) {
                SmallTopAppBar(
                    title = "关于",
                    scrollBehavior = scrollBehavior,
                    color = barColor,
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
                .then(if (backdrop != null) Modifier.background(surface) else Modifier),
        ) {
            BgEffectBackground(
                dynamicBackground = true,
                isDark = isDark,
                surface = surface,
                modifier = Modifier.fillMaxSize(),
                bgModifier = if (backdrop != null) {
                    Modifier.layerBackdrop(backdrop)
                } else {
                    Modifier
                },
                alpha = { 1f - scrollProgress },
            ) {
                AboutContent(
                    innerPadding = innerPadding,
                    lazyListState = lazyListState,
                    scrollBehavior = scrollBehavior,
                    scrollProgressProvider = { scrollProgress },
                    backdrop = backdrop,
                    isDark = isDark,
                )
            }
        }
    }
}

@Composable
private fun AboutContent(
    innerPadding: PaddingValues,
    lazyListState: LazyListState,
    scrollBehavior: ScrollBehavior,
    scrollProgressProvider: () -> Float,
    backdrop: LayerBackdrop?,
    isDark: Boolean,
) {
    val navigator = LocalNavigator.current
    val density = LocalDensity.current

    var logoColumnHeightPx by remember { mutableIntStateOf(0) }
    val logoColumnHeightDp = with(density) { logoColumnHeightPx.toDp() }

    val logoBlend = remember(isDark) {
        if (isDark) {
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
    }

    val cardBlend = if (isDark) {
        ColorBlendToken.Overlay_Thin_Light
    } else {
        ColorBlendToken.Pured_Regular_Light
    }

    val horizontalPadding = 24.dp
    val topPadding = innerPadding.calculateTopPadding() + 120.dp
    val titleBottomGap = 40.dp

    val spriteRepo = LocalSpriteRepository.current
    val logoBitmap by produceState<ImageBitmap?>(initialValue = null, spriteRepo) {
        value = withContext(Dispatchers.IO) { spriteRepo.getAssetImage("ic_launcher.png") }
    }

    // 固定在上方的图标/标题/版本：不随列表滚动。
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.onSizeChanged { logoColumnHeightPx = it.height },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .graphicsLayer {
                            val iconProgress = ((scrollProgressProvider() - 0.35f) / 0.15f)
                                .coerceIn(0f, 1f)
                            clip = true
                            shape = RoundedCornerShape(24.dp)
                            alpha = 1 - iconProgress
                            scaleX = 1 - (iconProgress * 0.05f)
                            scaleY = 1 - (iconProgress * 0.05f)
                        }
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        modifier = Modifier.size(74.dp),
                        bitmap = logoBitmap ?: spriteRepo.placeholder(),
                        contentDescription = null,
                    )
                }

                Text(
                    text = "奶牛镇百科",
                    fontSize = 35.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 5.dp)
                        .graphicsLayer {
                            val p = ((scrollProgressProvider() - 0.20f) / 0.15f)
                                .coerceIn(0f, 1f)
                            alpha = 1 - p
                            scaleX = 1 - (p * 0.05f)
                            scaleY = 1 - (p * 0.05f)
                        }
                        .then(
                            if (backdrop != null) {
                                Modifier.textureBlur(
                                    backdrop = backdrop,
                                    shape = RoundedCornerShape(16.dp),
                                    blurRadius = 150f,
                                    noiseCoefficient = BlurDefaults.NoiseCoefficient,
                                    colors = BlurDefaults.blurColors(blendColors = logoBlend),
                                    contentBlendMode = ComposeBlendMode.DstIn,
                                )
                            } else {
                                Modifier
                            },
                        ),
                )

                Text(
                    text = "$APP_VERSION_NAME ($APP_VERSION_CODE)",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        val p = ((scrollProgressProvider() - 0.05f) / 0.15f)
                            .coerceIn(0f, 1f)
                        alpha = 1 - p
                        scaleX = 1 - (p * 0.05f)
                        scaleY = 1 - (p * 0.05f)
                    },
                )
            }
        }

        Spacer(Modifier.height(titleBottomGap))
    }

    // 可滚动内容：与上方固定区域重叠，通过 logoSpacer 让卡片从标题下方升起。
    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(
            top = topPadding,
            start = horizontalPadding,
            end = horizontalPadding,
            bottom = 24.dp,
        ),
    ) {
        item(key = "logoSpacer") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(logoColumnHeightDp + titleBottomGap),
                contentAlignment = Alignment.TopCenter,
            ) { }
        }

        item(key = "about") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (backdrop != null) {
                            Modifier.textureBlur(
                                backdrop = backdrop,
                                shape = RoundedCornerShape(16.dp),
                                blurRadius = 60f,
                                noiseCoefficient = BlurDefaults.NoiseCoefficient,
                                colors = BlurDefaults.blurColors(blendColors = cardBlend),
                            )
                        } else {
                            Modifier
                        },
                    ),
                colors = CardDefaults.defaultColors(
                    color = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer,
                    contentColor = MiuixTheme.colorScheme.onSurface,
                ),
            ) {
                ArrowPreference(
                    title = "查看源码",
                    endActions = {
                        Text(
                            text = "GitHub",
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    },
                    onClick = { showToast("还没做") },
                )
                HorizontalDivider(color = MiuixTheme.colorScheme.dividerLine)
                ArrowPreference(
                    title = "加入群组",
                    endActions = {
                        Text(
                            text = "Telegram",
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    },
                    onClick = { showToast("还没做") },
                )
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (backdrop != null) {
                            Modifier.textureBlur(
                                backdrop = backdrop,
                                shape = RoundedCornerShape(16.dp),
                                blurRadius = 60f,
                                noiseCoefficient = BlurDefaults.NoiseCoefficient,
                                colors = BlurDefaults.blurColors(blendColors = cardBlend),
                            )
                        } else {
                            Modifier
                        },
                    ),
                colors = CardDefaults.defaultColors(
                    color = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer,
                    contentColor = MiuixTheme.colorScheme.onSurface,
                ),
            ) {
                ArrowPreference(
                    title = "开源协议",
                    endActions = {
                        Text(
                            text = "Apache-2.0",
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    },
                    onClick = { showToast("还没做") },
                )
                HorizontalDivider(color = MiuixTheme.colorScheme.dividerLine)
                ArrowPreference(
                    title = "第三方开源协议",
                    onClick = { navigator.push(Route.License) },
                )
            }
        }
    }
}
