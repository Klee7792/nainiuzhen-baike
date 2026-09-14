package com.nainiuzhen.wiki.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.Scaffold
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nainiuzhen.wiki.utils.LocalAppSettings
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 根据当前设置创建用于顶栏/背景模糊的 [LayerBackdrop]。
 * 只有在「启用模糊」开启且运行支持 RuntimeShader（Android 12+）时才返回非 null。
 */
@Composable
fun rememberAppBlurBackdrop(): LayerBackdrop? {
    val appState = LocalAppSettings.current
    if (!appState.enableBlur || !isRuntimeShaderSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * 与 [rememberAppBlurBackdrop] 类似，但**不绑定**「设置页 → 启用模糊」开关：
 * 只要运行环境支持 RuntimeShader（Android 12+）即返回非 null。
 * 用于所有 dialog 左右侧栏的边缘高斯模糊（变更点 #27-A：统一加高斯模糊、通透度略提高、
 * 不再跟随「启用模糊」开关，仅受运行环境着色器能力约束；不支持时回落到渐变兜底）。
 */
@Composable
fun rememberDialogSideBlurBackdrop(): LayerBackdrop? {
    if (!isRuntimeShaderSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * 模糊顶栏容器：把 [backdrop] 以高斯或渐进模糊形式铺到顶栏背景上。
 * 内容（真正的 TopAppBar）通过 [content] 传入。
 */
@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    scrollBehavior: ScrollBehavior? = null,
    active: Boolean? = null,
    forceUniformBlur: Boolean = false,
    content: @Composable () -> Unit,
) {
    val appState = LocalAppSettings.current
    val progressive = !forceUniformBlur && appState.topAppBarBlurStyle == 1
    val blurActive = active ?: (backdrop != null)
    val bd = backdrop
    Box(
        modifier = if (blurActive && !progressive && bd != null) {
            Modifier.textureBlur(
                backdrop = bd,
                shape = RectangleShape,
                blurRadius = 25f,
                colors = BlurDefaults.blurColors(
                    blendColors = listOf(
                        BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    ),
                ),
            )
        } else {
            Modifier
        },
    ) {
        if (blurActive && progressive && bd != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = if (active != null) {
                            if (active) 1f else 0f
                        } else {
                            scrollBehavior?.state
                                ?.let { (-it.contentOffset / 48.dp.toPx()).coerceIn(0f, 1f) }
                                ?: 1f
                        }
                    }
                    .progressiveTextureBlur(
                        backdrop = bd,
                        shape = RectangleShape,
                        // Top：模糊在顶部（状态栏）最强、向下渐隐——既保留「渐进」观感，
                        // 又让状态栏区域有完整模糊（Bottom 会在状态栏处渐隐到 0 → 透明，是 bug 根因）。
                        gradient = ProgressiveBlur.Top.copy(curve = 2.2f),
                        blurRadius = 10f,
                        colors = BlurDefaults.blurColors(
                            blendColors = listOf(
                                BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(alpha = 0.3f)),
                            ),
                        ),
                    ),
            )
        }
        content()
    }
}

/**
 * 应用统一顶栏：根据设置决定显示/隐藏，并支持大标题展开/折叠 + 背景模糊。
 *
 * @param title 折叠后的小标题。
 * @param scrollBehavior 滚动行为。
 * @param largeTitle 非 null 时使用 [TopAppBar]（初始显示左侧大标题，滚动后切换居中标题）；
 *   为 null 时使用 [SmallTopAppBar]。
 * @param largeTitleCentered 仅对大标题模式生效：展开态将大标题与副标题整体水平居中（默认 false，居左）。
 */
@Composable
fun AppTopAppBar(
    title: String,
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
    largeTitle: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    subtitle: String = "",
    largeTitleCentered: Boolean = false,
    forceUniformBlur: Boolean = false,
    bottomContent: @Composable () -> Unit = {},
) {
    val appState = LocalAppSettings.current
    if (!appState.showTopAppBar) return
    BlurredBar(
        backdrop = backdrop,
        scrollBehavior = scrollBehavior,
        forceUniformBlur = forceUniformBlur,
    ) {
        val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface
        // 副标题（如列表计数「共 XX 个」）直接转发给 miuix 原生 subtitle 参数：
        // - 展开时作为大标题的第二行；largeTitleCentered=true 时整体水平居中（契合「Y 轴居中、左右对称」）；
        // - 收起时由 miuix 原生 smallSubtitle 居中显示（本就居中，无需额外处理）；
        // - 搜索框等仍走 bottomContent，位于副标题之下。
        if (largeTitle != null) {
            TopAppBar(
                title = title,
                largeTitle = largeTitle,
                largeTitleCentered = largeTitleCentered,
                subtitle = subtitle,
                scrollBehavior = scrollBehavior,
                color = barColor,
                navigationIcon = navigationIcon,
                actions = actions,
                bottomContent = bottomContent,
                modifier = modifier,
            )
        } else {
            SmallTopAppBar(
                title = title,
                subtitle = subtitle,
                scrollBehavior = scrollBehavior,
                color = barColor,
                navigationIcon = navigationIcon,
                actions = actions,
                bottomContent = bottomContent,
                modifier = modifier,
            )
        }
    }
}

/**
 * 通用子页脚手架：统一带模糊顶栏（高斯 / 渐进跟随设置）、大标题展开→折叠（默认未滚动时标题居左），
 * 内容区挂载 [Modifier.layerBackdrop] 供顶栏模糊采样（变更点 #30：把主页/设置页的顶栏模糊扩展到子页/日程页）。
 *
 * 调用方需自行创建并持有 [scrollBehavior]，并把它同时传给本脚手架与内部可滚动容器的
 * `nestedScroll(scrollBehavior.nestedScrollConnection)`，以使顶栏随内容滚动折叠。
 *
 * @param largeTitleCentered 透传给 [AppTopAppBar]：展开态大标题+计数是否水平居中（默认 false）。
 * @param topBarWidth 顶栏宽度上限；传非 null 时顶栏只占该宽度（大屏双栏把顶栏归属左栏、不跨栏），
 *   为 null（默认）时铺满整屏。
 * @param content 内容区 lambda，接收 [PaddingValues]（顶栏占位）。
 */
@Composable
fun AppSubPageScaffold(
    title: String,
    largeTitle: String? = title,
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    subtitle: String = "",
    largeTitleCentered: Boolean = false,
    forceUniformBlur: Boolean = false,
    /** 顶栏宽度上限；为 null 时铺满整屏（默认）。大屏双栏时传「左栏宽度」，避免顶栏横跨到右栏之上。 */
    topBarWidth: Dp? = null,
    bottomContent: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val backdrop = rememberAppBlurBackdrop()
    Scaffold(
        topBar = {
            AppTopAppBar(
                title = title,
                largeTitle = largeTitle,
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                navigationIcon = navigationIcon,
                actions = actions,
                subtitle = subtitle,
                largeTitleCentered = largeTitleCentered,
                forceUniformBlur = forceUniformBlur,
                bottomContent = bottomContent,
                modifier = if (topBarWidth != null) modifier.width(topBarWidth) else modifier,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
        ) { content(innerPadding) }
    }
}
