// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// 移植自 miuix 示例 component.liquid.LiquidGlassNavigationBar（iOS 液态玻璃底栏）。
// 已改为本工程 package，并将 demo 的 ui.isInDarkTheme 替换为 Compose 的 isSystemInDarkTheme。
// 依赖（同 package）：DampedDragAnimation / InteractiveHighlight / CombinedBackdrop / InnerShadow。

package com.nainiuzhen.wiki.ui.components.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.colorControls
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.sensor.rememberDeviceTilt
import com.nainiuzhen.wiki.ui.components.liquid.lens
import com.nainiuzhen.wiki.utils.LocalAppSettings
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.Platform
import top.yukonga.miuix.kmp.utils.platform
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

private val LocalIosTabScale = staticCompositionLocalOf { { 1f } }

private val iosIndicatorSpecular: Highlight = Highlight(
    width = 1.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.12f),
        innerBlurRadius = 2.0.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            color = Color.White,
            intensity = 0.4f,
        ),
        dualPeak = true,
    ),
)

// Mirrors HighlightStyle.kt's LIGHT_REF — keep in sync.
private const val LIGHT_REF_X = 0.5f
private const val LIGHT_REF_Y = 0.7f
private const val GRAVITY_DIR_THRESHOLD_SQ = 0.01f // |g_xy| > 0.1, ≈ 6° tilt

// 3° quantization step for the gravity direction: finer changes are imperceptible.
private const val GRAVITY_ANGLE_STEP_RAD = (3.0 * PI / 180.0).toFloat()

/**
 * 单个底栏选项的宽度。底栏本体宽度 = 选项数 × 该值 + 左右各 4dp 内边距，
 * 随选项数变化、不再撑满宿主宽度（对齐 miuix 悬浮底栏 / KernelSU 的观感）。
 */
private val IOS_TAB_WIDTH = 76.dp

/**
 * In-screen-plane gravity direction angle (radians, quantized to 3° steps).
 *
 * Returned as [State] so the read can be deferred to the draw phase: the sensor writes tilt
 * state unthrottled (~50Hz), and a composition-time read would recompose the whole caller
 * scope on every tick. The derivedStateOf equality check then drops draw invalidations to
 * quantization-step crossings.
 */
@Composable
private fun rememberQuantizedGravityAngle(): State<Float> {
    val tiltState = rememberDeviceTilt()
    return remember(tiltState) {
        derivedStateOf {
            val tilt = tiltState.value
            val gx = tilt.gravityX
            val gy = tilt.gravityY
            val gMagSq = gx * gx + gy * gy
            if (gMagSq > GRAVITY_DIR_THRESHOLD_SQ) {
                (atan2(gy, gx) / GRAVITY_ANGLE_STEP_RAD).roundToInt() * GRAVITY_ANGLE_STEP_RAD
            } else {
                // Near-flat: the in-plane gravity direction is unstable, pin to (0, -1).
                (-PI / 2).toFloat()
            }
        }
    }
}

/**
 * [base] with its `dualPeak` primary light rotated to the gravity angle plus [extraDegrees].
 * Read `.value` only at draw time (see [rememberQuantizedGravityAngle]); the rotated copy is
 * cached, re-allocating only when the angle crosses a quantization step.
 */
@Composable
private fun rememberGravityRotatedHighlight(
    base: Highlight,
    extraDegrees: Float,
): State<Highlight> {
    val gravityAngle = rememberQuantizedGravityAngle()
    return remember(gravityAngle, base, extraDegrees) {
        derivedStateOf {
            val baseStyle = base.style as BloomStroke
            val basePrimary = baseStyle.primaryLight
            val rad = gravityAngle.value + (extraDegrees * PI / 180.0).toFloat()
            base.copy(
                style = baseStyle.copy(
                    primaryLight = basePrimary.copy(
                        position = LightPosition(
                            x = LIGHT_REF_X + cos(rad),
                            y = LIGHT_REF_Y + sin(rad),
                            z = basePrimary.position.z,
                        ),
                    ),
                ),
            )
        }
    }
}

@Composable
fun IosLiquidGlassNavigationBar(
    items: List<NavigationItem>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
    backdrop: LayerBackdrop?,
    isBlurActive: Boolean,
    modifier: Modifier = Modifier,
    badge: (Int) -> (@Composable () -> Unit)? = { null },
) {
    val appState = LocalAppSettings.current
    // ⚠️ 阴影 / 胶囊明暗必须跟随「应用主题」，而不是「系统主题」：
    // miuix demo 用的是应用主题（ui.isInDarkTheme），移植时误换成了 isSystemInDarkTheme()。
    // 当 App 被设为浅色（colorMode=2）而系统处于深色时，旧写法会走去 0.2f 的深阴影，
    // 在浅色界面上糊成一团灰（真机截图实测：等效黑透明度 0.20，而浅色主题应为 0.10）。
    // 口径与 MainScreen 里 miuix 悬浮底栏的 isBarDark 保持一致。
    val isDark = when (appState.colorMode) {
        0 -> isSystemInDarkTheme()
        1 -> true
        else -> false
    }
    val pillShape = remember { CircleShape }
    val accentColor = MiuixTheme.colorScheme.primary
    val tabContentColor = MiuixTheme.colorScheme.onSurface
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    val containerColor = if (isBlurActive) surfaceContainer.copy(alpha = 0.4f) else surfaceContainer

    val tabsBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val tabsCount = items.size

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { 4.dp.toPx() }
    val panelOffset by remember(rubberBandPx) {
        derivedStateOf {
            if (totalWidthPx == 0f) {
                0f
            } else {
                val fraction = (offsetAnimation.value / totalWidthPx).coerceIn(-1f, 1f)
                rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }
    }

    // 选项之间的水平内边距（与下方 Row 的 .padding(4.dp) 保持一致）：
    // 选中指示器的左缘应对齐到「4dp + index × 单选项宽」，否则指示器会整体左偏一截。
    val startPadPx = with(density) { 4.dp.toPx() }

    // 选中指示器水平位移：基础 = 4dp 起始内边距 + 进度 × 单选项宽；
    // 用 coerceIn 夹在 [首选项左缘, 末选项左缘] 之间，拖动/橡皮筋越界时指示器永不跑出底栏。
    // ⚠️ 有意**不**按放大倍数（scale）再夹一层：miuix demo 就是纯 progressOffset（见 demo
    // LiquidGlassNavigationBar.kt:506/553，无 coerceIn），按下期胶囊以中心对称放大、允许轻微越出
    // 底栏边缘 —— 这是该组件的既定观感。此前「压扁 / 超出底栏」的真因是交互期离屏 padding 被降到
    // 13dp，裁掉了每侧 14.93dp 的放大溢出；现 padding 恒 ≥ 40dp，无需也不应再动位移。
    // RTL 保持原公式不动，避免引入未经验证的镜像偏差。
    fun pillTranslationX(value: Float, offset: Float): Float {
        val progressOffset = value * tabWidthPx
        return if (isLtr) {
            (startPadPx + progressOffset + offset).coerceIn(
                startPadPx,
                startPadPx + (tabsCount - 1) * tabWidthPx,
            )
        } else {
            -progressOffset + offset
        }
    }

    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    val onItemClickUpdated by rememberUpdatedState(onItemClick)

    fun indexAt(positionX: Float): Int {
        if (tabWidthPx == 0f) return currentIndex
        val horizontalPaddingPx = with(density) { 4.dp.toPx() }
        val logicalX = if (isLtr) positionX else totalWidthPx - positionX
        return ((logicalX - horizontalPaddingPx) / tabWidthPx)
            .toInt()
            .coerceIn(0, tabsCount - 1)
    }

    val dampedDrag = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { position ->
                position.x in 0f..totalWidthPx
            },
            onDragStarted = { position ->
                updateValue(indexAt(position.x).toFloat())
            },
            onDragStopped = {
                val targetIndex = targetValue.roundToInt().coerceIn(0, tabsCount - 1)
                if (currentIndex != targetIndex) {
                    currentIndex = targetIndex
                    onItemClickUpdated(targetIndex)
                }
                updateValue(targetIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDragCancelled = {
                updateValue(currentIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0f && dragAmount.x != 0f) {
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .coerceIn(0f, (tabsCount - 1).toFloat()),
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            },
        )
    }

    LaunchedEffect(selectedIndex) {
        if (currentIndex != selectedIndex) {
            currentIndex = selectedIndex
            dampedDrag.animateToValue(selectedIndex.toFloat())
        }
    }

    fun activateTab(index: Int) {
        if (currentIndex != index) {
            currentIndex = index
            onItemClickUpdated(index)
        }
        dampedDrag.animateToValue(index.toFloat())
    }

    // Keyed on dampedDrag: the position lambda captures it; a stale capture would freeze the press spot.
    val interactiveHighlight = remember(animationScope, isLtr, dampedDrag) {
        InteractiveHighlight(
            animationScope = animationScope,
            position = { layerSize, _ ->
                Offset(
                    x = if (isLtr) {
                        startPadPx + (dampedDrag.value + 0.5f) * tabWidthPx + panelOffset
                    } else {
                        layerSize.width - startPadPx - (dampedDrag.value + 0.5f) * tabWidthPx + panelOffset
                    },
                    y = layerSize.height / 2f,
                )
            },
        )
    }

    // Read .value only inside highlight lambdas (draw phase), never in composition.
    val baseHighlight = rememberGravityRotatedHighlight(iosIndicatorSpecular, extraDegrees = -45f)
    val pillHighlight = rememberGravityRotatedHighlight(iosIndicatorSpecular, extraDegrees = 90f)

    val combinedBackdrop = backdrop?.let { rememberCombinedBackdrop(it, tabsBackdrop) }

    val navBarBottomPadding = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    val bottomPaddingValue = when (platform()) {
        // iOS 原为 20dp：真机实测底栏下缘到屏幕底达 ≈71dp（Android 同场景 ≈60dp），
        // 再叠加 iOS 更胖的阴影，底部整体显得过重。收窄到 8dp 与 Android 观感对齐
        //（仍留 ≈60dp 余量，不会碰到 home indicator）。
        Platform.IOS -> 8.dp

        else -> {
            if (navBarBottomPadding != 0.dp) 8.dp + navBarBottomPadding else 36.dp
        }
    }

    val tabsContent: @Composable RowScope.() -> Unit = {
        val tabScale = LocalIosTabScale.current
        items.forEachIndexed { index, item ->
            Column(
                modifier = Modifier
                    .semantics(mergeDescendants = true) {
                        selected = index == currentIndex
                        role = Role.Tab
                        onClick {
                            activateTab(index)
                            true
                        }
                    }
                    .onKeyEvent { event ->
                        val isActivationKey = event.key == Key.Enter ||
                            event.key == Key.NumPadEnter ||
                            event.key == Key.Spacebar
                        if (isActivationKey) {
                            if (event.type == KeyEventType.KeyUp) activateTab(index)
                            true
                        } else {
                            false
                        }
                    }
                    .focusable()
                    // 固定宽（而非 weight）：让整条底栏按「选项数 × 单选项宽」收缩，
                    // 与 miuix 悬浮底栏一致，不再撑满主页宽度（KernelSU 风格）。
                    .width(IOS_TAB_WIDTH)
                    .fillMaxHeight()
                    .graphicsLayer {
                        val s = tabScale()
                        scaleX = s
                        scaleY = s
                    },
                verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                horizontalAlignment = CenterHorizontally,
            ) {
                BadgedBox(badge = { badge(index)?.invoke() }) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        imageVector = item.icon,
                        // Decorative: the adjacent label names the item; avoids TalkBack double-read.
                        contentDescription = null,
                    )
                }
                Text(
                    text = item.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    // 外层仍占满宽度以便水平居中，底栏本体（下面的 Box）按内容宽度收缩。
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = bottomPaddingValue, start = 28.dp, end = 28.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            CompositionLocalProvider(LocalContentColor provides tabContentColor) {
                Row(
                    modifier = Modifier
                        .selectableGroup()
                        .onSizeChanged { coords ->
                            totalWidthPx = coords.width.toFloat()
                            val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                            tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                        }
                        .graphicsLayer { translationX = panelOffset }
                        .dropShadow(
                            shape = pillShape,
                            shadow = Shadow(
                                // iOS 端 dropShadow 的实际扩散明显大于 Android（同一 radius 下真机实测：
                                // iOS 阴影自底栏下缘再向下延伸 ≈44dp、且前 20dp 维持满强度；Android ≈26dp
                                // 且立即衰减），故 iOS 取更小 radius，收敛到与 Android 同量级的贴边浅影。
                                radius = if (platform() == Platform.IOS) 6.dp else 8.dp,
                                color = Color.Black,
                                // Lighter in light theme to avoid a visible gray fringe.
                                alpha = if (isDark) 0.2f else 0.1f,
                            ),
                        )
                        .then(
                            if (isBlurActive && backdrop != null) {
                                Modifier.drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { pillShape },
                                effects = {
                        // 40dp = 24dp lens refraction + 16dp press-scale reach，来自 miuix demo L424，勿降。
                        // 离屏 padding 下限恒为 40dp：按下期胶囊放大到 78dp，上下各溢出 (78-56)/2 = 11dp，
                        // 需 ≥ 其半高+溢出+折射取样 的离屏层才不被裁 —— 13dp/24dp 都不够，被裁的溢出
                        // 会表现为指示器「上下压扁」。故 padding 与是否交互无关，恒定 ≥ 40dp。
                        padding = maxOf(padding, 40.dp.toPx())
                        vibrancy()
                        blur(
                            4.dp.toPx(),
                            4.dp.toPx(),
                        )
                        // glassInteractionDegrade 的降级语义 = 交互期仅「跳过 lens 折射 pass」（少一趟 GPU pass），
                        // 不再靠缩小 padding 实现（缩小 padding 在拖动放大期必然裁切溢出的指示器）。
                        val interacting = appState.glassInteractionDegrade && dampedDrag.pressProgress > 0.01f
                        if (!interacting) {
                            lens(
                                refractionHeight = 24.dp.toPx(),
                                refractionAmount = 24.dp.toPx(),
                            )
                        }
                    },
                                    highlight = { baseHighlight.value.copy(alpha = 0.75f) },
                                    layerBlock = {
                                        val width = size.width.coerceAtLeast(1f)
                                        val s = lerp(1f, 1f + 16.dp.toPx() / width, dampedDrag.pressProgress)
                                        scaleX = s
                                        scaleY = s
                                    },
                                    onDrawSurface = { drawRect(containerColor) },
                                )
                            } else {
                                Modifier
                                    .background(containerColor, pillShape)
                            },
                        )
                        .then(
                            if (isBlurActive) {
                                interactiveHighlight.modifier.then(interactiveHighlight.gestureModifier)
                            } else {
                                Modifier
                            },
                        )
                        .then(dampedDrag.modifier)
                        .height(64.dp)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = tabsContent,
                )
            }

            if (isBlurActive && backdrop != null) {
                CompositionLocalProvider(
                    LocalIosTabScale provides { lerp(1f, 1.2f, dampedDrag.pressProgress) },
                    LocalContentColor provides accentColor,
                ) {
                    Row(
                        modifier = Modifier
                            .clearAndSetSemantics {}
                            .alpha(0f)
                            .layerBackdrop(tabsBackdrop)
                            .graphicsLayer { translationX = panelOffset }
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { pillShape },
                                effects = {
                                    // 与 miuix 示例保持一致：本层虽 alpha=0（不上屏），但它渲染进
                                    // tabsBackdrop，是滑动指示器经 combinedBackdrop 取像的**像源**。
                                    // 只留 blur 会让指示器折射到的底图缺 vibrancy/lens 两步预处理，
                                    // 胶囊内部的饱和度与边缘折射观感与 demo 不一致 ⇒ 三项都要跑。
                                    vibrancy()
                                    blur(4.dp.toPx(), 4.dp.toPx())
                                    lens(
                                        refractionHeight = 24.dp.toPx(),
                                        refractionAmount = 24.dp.toPx(),
                                    )
                                },
                                onDrawSurface = { drawRect(containerColor) },
                            )
                            .then(interactiveHighlight.modifier)
                            .height(56.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = tabsContent,
                    )
                }
            }

            if (tabWidthPx > 0f) {
                val tabWidthDp = with(density) { tabWidthPx.toDp() }
                if (isBlurActive && combinedBackdrop != null) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = pillTranslationX(dampedDrag.value, panelOffset)
                        }
                        .drawBackdrop(
                                backdrop = combinedBackdrop,
                                shape = { pillShape },
                                effects = {
                                    val progress = dampedDrag.pressProgress
                                    // 此处不做色散降级：pill 的 lens 只在按压期存在（refractionAmount = 14dp * progress，
                                    // progress=0 时整趟 lens 不产生可见效果），所以"交互期把 chromaticAberration 关掉"
                                    // 等价于永久失去这颗水滴的彩虹光晕——不是"交互期临时降级"，而是丢视觉，故保持恒定 0.5f。
                                    // 交互期省的开销来自上面主层 B3 的「跳过整趟 lens pass」，不是这里的色散。
                                    lens(
                                        refractionHeight = 10.dp.toPx() * progress,
                                        refractionAmount = 14.dp.toPx() * progress,
                                        depthEffect = true,
                                        chromaticAberration = 0.5f,
                                    )
                                },
                                highlight = { pillHighlight.value.copy(alpha = dampedDrag.pressProgress) },
                                layerBlock = {
                                    scaleX = dampedDrag.scaleX
                                    scaleY = dampedDrag.scaleY
                                    // ⚠️ 「沿拖动方向拉伸」必须乘 pressProgress 做门控（demo 无此门控）。
                                    // velocity 只在 updateValue 的收敛动画里被 updateVelocity 重写，手势结束后
                                    // 没有任何地方把它归零，于是它会永久停在非零残值上；而 scaleX /= 、scaleY *= 是
                                    // 各向异性的（cX 用 0.75、cY 用 0.25），结果就是胶囊被永久拉伸 ——
                                    // 实测：主页 scaleX≈1.24/scaleY≈0.92（上下压扁），设置 scaleX≈0.84/scaleY≈1.05
                                    // （左右压扁）。pressProgress 在抬手后必回 0，乘上它可保证静止态
                                    // 严格 scaleX == scaleY == 1，同时保留拖动期的原有弹性观感。
                                    val press = dampedDrag.pressProgress
                                    val v = dampedDrag.velocity / 10f * press
                                    scaleX /= 1f - (v * 0.75f).coerceIn(-0.2f, 0.2f)
                                    scaleY *= 1f - (v * 0.25f).coerceIn(-0.2f, 0.2f)
                                },
                                onDrawSurface = {
                                    val progress = dampedDrag.pressProgress
                                    drawRect(
                                        color = if (!isDark) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f),
                                        alpha = 1f - progress,
                                    )
                                    drawRect(Color.Black.copy(alpha = 0.03f * progress))
                                },
                            )
                            .innerShadow(shape = pillShape) {
                                InnerShadow(
                                    radius = 8.dp * dampedDrag.pressProgress,
                                    color = Color.Black.copy(alpha = 0.15f),
                                    alpha = dampedDrag.pressProgress,
                                )
                            }
                            .height(56.dp)
                            .width(tabWidthDp),
                    )
                } else {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = pillTranslationX(dampedDrag.value, panelOffset)
                        }
                        .clip(pillShape)
                            .background(accentColor.copy(alpha = 0.15f), pillShape)
                            .height(56.dp)
                            .width(tabWidthDp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        CompositionLocalProvider(LocalContentColor provides accentColor) {
                            Row(
                                modifier = Modifier
                                    .clearAndSetSemantics {}
                                    .wrapContentWidth(align = Alignment.Start, unbounded = true)
                                    .requiredWidth(with(density) { (totalWidthPx - 8.dp.toPx()).toDp() })
                                    .height(56.dp)
                                    .graphicsLayer {
                                        val progressOffset = dampedDrag.value * tabWidthPx
                                        translationX = if (isLtr) -progressOffset else progressOffset
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                content = tabsContent,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 轻量 vibrancy 近似，移植自 miuix 示例 `component.liquid.Vibrancy`。
 * 提升饱和度，使底栏内容在模糊层上更「通透」。仅依赖公开的 [colorControls]
 * （miuix `BackdropEffectScope` 扩展），不依赖示例模块的内部 shader API。
 */
private fun BackdropEffectScope.vibrancy() {
    colorControls(
        brightness = 0f,
        contrast = 1f,
        saturation = 1.5f,
    )
}
