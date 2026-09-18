// 奶牛镇百科 · 关于子页（100% 复刻 miuix demo AboutPage）
//
// 与 demo 的差异（按用户要求）：
// - 标题 "Miuix for Compose" → "奶牛镇百科"
// - 图标走 app 自有图标（assets/ic_launcher.png，经 LocalSpriteRepository 解码），非 demo 的 Res.drawable.ic_launcher
// - 版本号走 LocalAppVersion（Android 端经 BuildConfig 注入），格式与 demo 一致："vX.Y.Z (N)"
// - 4 个选项文字中文化：查看源码 / 加入群组 / 开源协议 / 第三方开源协议
// 其余滚动联动、波浪背景、顶栏渐显、OS2/OS3 弹窗、滚动条等均对齐 demo。

@file:OptIn(ExperimentalScrollBarApi::class)

package com.nainiuzhen.wiki.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode as ComposeBlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nainiuzhen.wiki.ui.components.BlurredBar
import com.nainiuzhen.wiki.ui.components.rememberAppBlurBackdrop
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.ui.nav.Route
import com.nainiuzhen.wiki.ui.settings.about.BgEffectBackground
import com.nainiuzhen.wiki.ui.settings.about.ColorBlendToken
import com.nainiuzhen.wiki.utils.LocalAppVersion
import com.nainiuzhen.wiki.utils.LocalAppSettings
import com.nainiuzhen.wiki.ui.nav.LocalSpriteRepository
import com.nainiuzhen.wiki.utils.IoDispatcher
import com.nainiuzhen.wiki.utils.showToast
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

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

    var isOs3Effect by remember { mutableStateOf(true) }
    var showTextureSet by remember { mutableStateOf(false) }
    var dynamicBackground by remember { mutableStateOf(isRuntimeShaderSupported()) }
    var isFullScreenBackground by remember { mutableStateOf(true) }

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

    val collapsed by remember { derivedStateOf { scrollProgress == 1f } }
    val blurActive by remember(backdrop) { derivedStateOf { backdrop != null && scrollProgress == 1f } }

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
                    defaultWindowInsetsPadding = false,
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
            modifier = if (backdrop != null) {
                Modifier.layerBackdrop(backdrop)
            } else {
                Modifier
            },
        ) {
            AboutContent(
                innerPadding = innerPadding,
                scrollBehavior = scrollBehavior,
                lazyListState = lazyListState,
                scrollProgressProvider = { scrollProgress },
                isDark = isDark,
                isOs3Effect = isOs3Effect,
                onOs3EffectChange = { isOs3Effect = it },
                dynamicBackground = dynamicBackground,
                onDynamicBackgroundChange = { dynamicBackground = it },
                isFullScreenBackground = isFullScreenBackground,
                onFullScreenBackgroundChange = { isFullScreenBackground = it },
                showTextureSet = showTextureSet,
                onShowTextureSetChange = { showTextureSet = it },
            )
        }
    }
}

@Composable
private fun AboutContent(
    innerPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    lazyListState: LazyListState,
    scrollProgressProvider: () -> Float,
    isDark: Boolean,
    isOs3Effect: Boolean,
    onOs3EffectChange: (Boolean) -> Unit,
    dynamicBackground: Boolean,
    onDynamicBackgroundChange: (Boolean) -> Unit,
    isFullScreenBackground: Boolean,
    onFullScreenBackgroundChange: (Boolean) -> Unit,
    showTextureSet: Boolean,
    onShowTextureSetChange: (Boolean) -> Unit,
) {
    val navigator = LocalNavigator.current
    val appState = LocalAppSettings.current
    val uriHandler = LocalUriHandler.current
    val density = LocalDensity.current

    val backdrop = rememberAppBlurBackdrop()
    val appVersion = LocalAppVersion.current

    val cardBlend = if (isDark) {
        ColorBlendToken.Overlay_Thin_Light
    } else {
        ColorBlendToken.Pured_Regular_Light
    }
    // 命名注意：ColorBlendToken 的 *Light / *Dark 后缀沿用 miuix demo 语义（指"用在哪种底上"），
    // 与"当前是否深色模式"相反 —— 深色模式取 *Light，浅色模式取 *Dark。
    // 内容已与上面原内联列表逐项一致，像素不变（已核对 miuix example/AboutPage.kt:200-206）。
    val logoBlend = if (isDark) ColorBlendToken.TitleLight else ColorBlendToken.TitleDark

    val horizontalPadding = 24.dp
    val topAppBarPad = innerPadding.calculateTopPadding()
    val scrollPadding = PaddingValues(
        top = topAppBarPad,
        start = horizontalPadding,
        end = horizontalPadding,
        bottom = innerPadding.calculateBottomPadding() + 24.dp,
    )
    // demo：logoPadding 比 scrollPadding 多 extraTop=40.dp，Column 内再 +52.dp
    val logoPaddingTop = topAppBarPad + 92.dp

    var logoHeightDp by remember { mutableStateOf(300.dp) }

    val spriteRepo = LocalSpriteRepository.current
    val logoBitmap by produceState<ImageBitmap?>(initialValue = null, spriteRepo) {
        value = withContext(IoDispatcher) { spriteRepo.getAssetImage("ic_launcher.png") }
    }

    BgEffectBackground(
        dynamicBackground = dynamicBackground,
        modifier = Modifier.fillMaxSize(),
        bgModifier = if (backdrop != null) {
            Modifier.layerBackdrop(backdrop)
        } else {
            Modifier
        },
        isFullSize = isFullScreenBackground,
        effectBackground = true,
        isOs3Effect = isOs3Effect,
        alpha = { 1f - scrollProgressProvider() },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = logoPaddingTop,
                        start = horizontalPadding,
                        end = horizontalPadding,
                    )
                    .onSizeChanged { size ->
                        with(density) { logoHeightDp = size.height.toDp() }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
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
                ) {
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        bitmap = logoBitmap ?: spriteRepo.placeholder(),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
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
                    text = "v" + appVersion.name + " (" + appVersion.code + ")",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val p = ((scrollProgressProvider() - 0.05f) / 0.15f)
                                .coerceIn(0f, 1f)
                            alpha = 1 - p
                            scaleX = 1 - (p * 0.05f)
                            scaleY = 1 - (p * 0.05f)
                        },
                )
            }

            // Scrollable content
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .then(if (appState.scrollEndHaptic) Modifier.scrollEndHaptic() else Modifier),
                contentPadding = scrollPadding,
            ) {
                item(key = "logoSpacer") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(
                                logoHeightDp + 52.dp + (logoPaddingTop - topAppBarPad) + 126.dp,
                            )
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    onShowTextureSetChange(true)
                                }
                            },
                        contentAlignment = Alignment.TopCenter,
                        content = { },
                    )
                }

                item(key = "about") {
                    Box {
                        Spacer(Modifier.fillParentMaxHeight())
                        Column(
                            modifier = Modifier.padding(bottom = scrollPadding.calculateBottomPadding()),
                        ) {
                            Card(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
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
                                    onClick = { uriHandler.openUri("https://github.com/compose-miuix-ui/miuix") },
                                )
                                ArrowPreference(
                                    title = "加入群组",
                                    endActions = {
                                        Text(
                                            text = "Telegram",
                                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                        )
                                    },
                                    onClick = { showToast("暂时没有群组") },
                                )
                            }

                            Card(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .padding(top = 12.dp)
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
                                    onClick = { uriHandler.openUri("https://www.apache.org/licenses/LICENSE-2.0.txt") },
                                )
                                ArrowPreference(
                                    title = "第三方开源协议",
                                    onClick = { navigator.push(Route.License) },
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            VerticalScrollBar(
                adapter = rememberScrollBarAdapter(lazyListState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                trackPadding = scrollPadding,
            )
        }

        OverlayBottomSheet(
            show = showTextureSet,
            title = "背景效果",
            onDismissRequest = {
                onShowTextureSetChange(false)
            },
            insideMargin = DpSize(0.dp, 0.dp),
        ) {
            LazyColumn {
                item {
                    val effectVariantOptions = listOf("OS2", "OS3")
                    OverlayDropdownPreference(
                        title = "效果样式",
                        items = effectVariantOptions,
                        selectedIndex = if (isOs3Effect) 1 else 0,
                        onSelectedIndexChange = { onOs3EffectChange(it == 1) },
                    )

                    SwitchPreference(
                        title = "动态背景",
                        checked = dynamicBackground,
                        onCheckedChange = {
                            onDynamicBackgroundChange(it)
                        },
                    )

                SwitchPreference(
                    title = "全屏背景",
                    checked = isFullScreenBackground,
                    onCheckedChange = {
                        onFullScreenBackgroundChange(it)
                    },
                )
            }
            // 本页是独立 Scaffold + 自绘 LazyColumn，不经 AppSubPageScaffold，
            // 底部安全区避让仍需自带 spacer（勿删）。
                item {
                    Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
                }
            }
        }
    }
}
