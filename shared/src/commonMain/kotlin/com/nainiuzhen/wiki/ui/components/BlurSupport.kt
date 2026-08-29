package com.nainiuzhen.wiki.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.Scaffold
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
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
 * 模糊顶栏容器：把 [backdrop] 以高斯或渐进模糊形式铺到顶栏背景上。
 * 内容（真正的 TopAppBar）通过 [content] 传入。
 */
@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    scrollBehavior: ScrollBehavior? = null,
    active: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val appState = LocalAppSettings.current
    val progressive = appState.topAppBarBlurStyle == 1
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
                        gradient = ProgressiveBlur.Bottom.copy(curve = 2.2f),
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
    bottomContent: @Composable () -> Unit = {},
) {
    val appState = LocalAppSettings.current
    if (!appState.showTopAppBar) return
    BlurredBar(backdrop = backdrop, scrollBehavior = scrollBehavior) {
        val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface
        // 副标题（如列表计数「共 XX 个」）直接转发给 miuix 原生 subtitle 参数：
        // - 顶栏展开时作为大标题的第二行、左对齐（紧贴标题，标题位置不受影响）；
        // - 顶栏收起时居中显示在顶栏内；
        // - 搜索框等仍走 bottomContent，位于副标题之下。
        if (largeTitle != null) {
            TopAppBar(
                title = title,
                largeTitle = largeTitle,
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
 * @param content 内容区 lambda，接收 [PaddingValues]（顶栏占位）。
 */
@Composable
fun AppSubPageScaffold(
    title: String,
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    subtitle: String = "",
    bottomContent: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val backdrop = rememberAppBlurBackdrop()
    Scaffold(
        topBar = {
            AppTopAppBar(
                title = title,
                largeTitle = title,
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                navigationIcon = navigationIcon,
                actions = actions,
                subtitle = subtitle,
                bottomContent = bottomContent,
                modifier = modifier,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
        ) {
            content(innerPadding)
        }
    }
}
