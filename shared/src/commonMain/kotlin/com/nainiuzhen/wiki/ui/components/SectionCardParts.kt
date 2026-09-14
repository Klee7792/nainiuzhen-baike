package com.nainiuzhen.wiki.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.nainiuzhen.wiki.utils.CardSection
import kotlinx.coroutines.delay
import com.nainiuzhen.wiki.utils.LocalAppSettings
import com.nainiuzhen.wiki.utils.cardBgEnabled
import com.nainiuzhen.wiki.utils.cardCapsuleEnabled
import com.nainiuzhen.wiki.utils.cardPressShadowEnabled
import com.nainiuzhen.wiki.utils.cardPressShadowShape
import com.nainiuzhen.wiki.utils.cardShape
import kotlin.math.ceil
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 「卡片外观设置」（v31）三个板块（物品 / 配方 / NPC）列表网格卡片的**公共容器部件**。
 *
 * 三个板块的网格卡片结构本来几乎逐行相同：
 * ```
 * 图标区（方块素材 + 可选底色 + 可选圆角）
 *   名称区（可选蓝胶囊 + 单行文字，超宽在胶囊内横向滚动）
 * ```
 * 差别仅在于「素材渲染器」（[SpriteImage] / [NpcPortraitImage]）与名称字号 / 样式。
 * 于是把「与外观设置相关」的三块抽到这里，让三个列表页共用同一套开关求值逻辑：
 *
 * - [CardImageBox]：素材容器（读「卡片背景」「卡片圆角」两组开关）。
 * - [CardNameCapsule]：名称行（读「文字胶囊」开关）。
 * - [rememberCardPressModifier]：按下阴影 + 点击（读「卡片圆角」总闸，默认零视觉变化）。
 *
 * 三者在内部各自通过 [LocalAppSettings] 拿到当前 [AppState]，调用点只需传 [CardSection]，
 * 无需把设置状态层层往下传。开关的求值口径见 `utils/CardAppearance.kt`。
 */

/**
 * 素材容器：把调用方给的 [modifier] 与「背景色 / 圆角裁剪」串起来。
 *
 * - 背景色：该板块「卡片背景」开启时用 [MiuixTheme.colorScheme.surfaceContainer]（略灰于页面白底），
 *   关闭时透明（默认态 = 无背景）。
 * - 圆角：该板块「卡片圆角」四个角各自求值后的实际形状；默认全 0 ⇒ 直角。
 *
 * 关键顺序：`background` / `clip` 必须接在调用方传入的 [modifier] **之后**，
 * 因为调用方会传入 `Modifier.fillMaxWidth().aspectRatio(1f)` 来定尺寸，背景与裁剪要盖在它上面。
 *
 * @param section 所属板块，决定读哪一组开关。
 * @param modifier 尺寸 / 布局修饰符（通常含 `fillMaxWidth().aspectRatio(1f)`）。
 * @param contentAlignment 内容对齐，默认居中。
 * @param content 容器内容（通常是素材图）。
 */
@Composable
fun CardImageBox(
    section: CardSection,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = LocalAppSettings.current
    val shape = state.cardShape(section)
    Box(
        modifier = modifier
            .background(
                color = if (state.cardBgEnabled(section)) {
                    MiuixTheme.colorScheme.surfaceContainer
                } else {
                    Color.Transparent
                },
                shape = shape,
            )
            .clip(shape),
        contentAlignment = contentAlignment,
        content = content,
    )
}

/**
 * 名称胶囊：一行文字，外面套一层可选蓝底满宽胶囊。
 *
 * - 胶囊开启：外层画 [MiuixTheme.colorScheme.primary] 的 50% 圆角胶囊，文字用 [MiuixTheme.colorScheme.onPrimary]。
 * - 胶囊关闭：外层无底色，文字用 [MiuixTheme.colorScheme.onSurface]（纯文字）。
 * - 无论开关，[contentPadding] 都照常应用，保证两种形态的留白一致、切换时布局不跳。
 *
 * 内层始终保留既有行为：宽度 = 胶囊内宽，**文字超宽时在该层横向滚动**，胶囊本身不动。
 *
 * ### 自动跑马灯（v35：首尾相连一圈 + 同页同步）
 *
 * 与 v34 及更早「从左对齐滚到右对齐就结束」不同，v35 的**一圈**是首尾相连的完整循环：
 *
 * ```
 * 内容 = 单元 × 2，单元 = 「文本 + 补空格（补到子页面最长宽度）+ 一屏间隔（补空格）」
 * 滚过恰好 1 个单元的宽度 = 末字从左侧消失 → 下一单元的首字从右侧进入 → 回到起点
 * ```
 *
 * - **只有超宽才滚**：本条文字宽度 > 名称区宽度才启用；不超宽的卡片完全静止、居中，与旧版一致。
 * - **圈同步**：各卡片文字长度不同 ⇒ 一圈耗时不同。由页级 [MarqueeCoordinator] 收口 ——
 *   每张卡片跑完一圈报到一次，**同屏所有超宽卡片都跑完**才统一停 2 秒并放行下一圈，
 *   避免「快的已经第二圈、慢的还在第一圈」的参差感。
 * - **对齐**：本页存在超宽名称时，全页统一改为**起始对齐**（否则滚动期间居中内容永远滚不到开头）。
 * - **节奏**：卡片进入屏幕后静止满 3 秒 → 滚第 1 圈 → 停 2 秒 → 第 2 圈 → 回位并**停止**
 *   （[MARQUEE_PASS_COUNT] = 2，不再无限循环）。
 * - **只在屏幕区滚**：只有落在「状态栏与导航栏之间」可视区内的卡片才会滚；
 *   滚出屏幕再回来会重新计数（再滚两圈），懒加载 / 屏幕外区域不消耗滚动。
 * - **打断**：用户手指横向拖动立刻中断当前一圈，需重新静止满 3 秒才会再触发。
 *
 * @param section 所属板块，决定读「文字胶囊」开关。
 * @param text 名称文本。
 * @param modifier 外层修饰符（一般无需传，默认 `fillMaxWidth()`）。
 * @param style 文字样式；传 `null`（默认）时退回主题默认样式 `MiuixTheme.textStyles.main`。
 * @param fontSize 字号；传 `null`（默认）时不覆盖 [style] 的字号。
 * @param contentPadding 外层内边距，默认与旧的物品 / 配方名称行一致（上 2、左右 8）。
 */
@Composable
fun CardNameCapsule(
    section: CardSection,
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle? = null,
    fontSize: TextUnit? = null,
    contentPadding: PaddingValues = PaddingValues(top = 2.dp, start = 8.dp, end = 8.dp),
) {
    val state = LocalAppSettings.current
    val on = state.cardCapsuleEnabled(section)
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val densityValue = density.density

    // —— 文本度量：量一次空格宽（补空格用）与本条文本宽 ——
    val measurer = rememberTextMeasurer()
    val baseStyle = style ?: MiuixTheme.textStyles.main
    val textStyle = if (fontSize != null && fontSize != TextUnit.Unspecified) {
        baseStyle.copy(fontSize = fontSize)
    } else {
        baseStyle
    }
    val spaceWidthPx = remember(measurer, textStyle) {
        measureTextWidthPx(measurer, " ", textStyle).coerceAtLeast(1)
    }
    val textWidthPx = remember(measurer, text, textStyle) {
        measureTextWidthPx(measurer, text, textStyle)
    }
    val coordinator = LocalMarqueeCoordinator.current
    // 本卡片在协调器里的身份（重组间稳定，用于「同屏卡片是否都跑完一圈」的计数）。
    val marqueeKey = remember { Any() }

    // 内层滚动视口宽度（名称区可用宽度）；布局完成后才有值，用于算「一屏间隔」与启用判定。
    var viewportWidthPx by remember { mutableIntStateOf(0) }

    // —— 跑马灯规格 ——
    // 启用条件：有同步基准 + 视口已测量 + 最长名称确实超出视口（否则整页都不需要滚动）。
    // 补空格规则：短名称补到 sharedMaxWidthPx，间隔补满一屏 ⇒ 单元宽 = 最长文本宽 + 视口宽。
    val marquee = remember(textWidthPx, spaceWidthPx, viewportWidthPx, text, textStyle) {
        if (viewportWidthPx <= 0 || textWidthPx <= viewportWidthPx) {
            // 不超宽（或视口还没测量出来）：完全不滚，与旧版一样居中静止。
            null
        } else {
            // 一圈 = 「文本 + 一屏间隔」。滚过这个宽度时，最后一个字刚好从左侧消失，
            // 而重复文本的第一个字正好从右边缘进入 —— 无缝衔接，中间不留空白。
            val gapCount = ceil(viewportWidthPx.toFloat() / spaceWidthPx).toInt().coerceAtLeast(1)
            val unit = text + " ".repeat(gapCount)
            val unitWidth = measureTextWidthPx(measurer, unit, textStyle)
            MarqueeSpec(displayText = unit + text, distancePx = unitWidth.coerceAtLeast(1))
        }
    }

    // —— 屏幕可视区判定（状态栏 ~ 导航栏之间）——
    val windowInfo = LocalWindowInfo.current
    val statusBarTopPx = WindowInsets.statusBars.getTop(density)
    val navBarBottomPx = WindowInsets.navigationBars.getBottom(density)
    var onScreen by remember { mutableStateOf(false) }

    // —— 用户拖动打断 ——
    // 程序化的 animateScrollBy 不会派发 DragInteraction（只有用户手势会），所以自动滚动不会自打断。
    var dragRestartToken by remember { mutableIntStateOf(0) }
    LaunchedEffect(marquee) {
        if (marquee == null) return@LaunchedEffect
        scrollState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) dragRestartToken++
        }
    }

    // 主循环：静止满 3 秒 → 恒速滚 [MARQUEE_PASS_COUNT] 圈（圈间停 2 秒）→ 回位停止。
    // key 变化（用户打断 / 进出屏幕 / 规格重算）或离开组合都会取消协程，无残留。
    LaunchedEffect(marquee, dragRestartToken, onScreen, coordinator) {
        val spec = marquee ?: return@LaunchedEffect
        if (!onScreen) return@LaunchedEffect
        // 登记为「本屏参与者」：协调器据此判断一圈是否全员跑完。
        coordinator.join(marqueeKey)
        try {
            delay(MARQUEE_IDLE_TRIGGER_MS)
            // 用户仍在拖动 / 甩动中：放弃本轮，等下一次「进入屏幕 + 静止」再触发。
            if (scrollState.isScrollInProgress) return@LaunchedEffect
            repeat(MARQUEE_PASS_COUNT) { pass ->
                // 第 2 圈起：等协调器放行（= 同屏所有超宽卡片都跑完上一圈 + 停 2 秒）。
                // 各卡片文字长度不同、耗时不同，这样能保证不会参差不齐。
                if (pass > 0) coordinator.awaitLap(pass)
                scrollState.scrollTo(0)
                // 兜底：实测文本宽与理论值可能有 1~2px 差，别超过可滚动余量被 clamp 卡住。
                val distance = spec.distancePx.toFloat()
                    .coerceAtMost(scrollState.maxValue.toFloat().coerceAtLeast(1f))
                val durationMs =
                    (distance / (densityValue * MARQUEE_SPEED_DP_PER_SECOND) * 1000f)
                        .toInt().coerceIn(1, Int.MAX_VALUE)
                scrollState.animateScrollBy(
                    value = distance,
                    animationSpec = tween(durationMillis = durationMs, easing = LinearEasing),
                )
                // 一圈走完：内容首尾相连，回到 0 在视觉上无缝衔接（下一圈的首字刚好接续）。
                scrollState.scrollTo(0)
                // 向协调器报到；由它判断是否全员跑完、何时放行下一圈（含 2 秒停顿）。
                coordinator.lapDone(marqueeKey, pass, this)
            }
        } finally {
            coordinator.leave(marqueeKey)
            scrollState.scrollTo(0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val top = coordinates.positionInWindow().y
                val bottom = top + coordinates.size.height
                val viewTop = statusBarTopPx.toFloat()
                val viewBottom = (windowInfo.containerSize.height - navBarBottomPx).toFloat()
                val visible = bottom > viewTop && top < viewBottom
                if (visible != onScreen) onScreen = visible
            }
            .then(
                if (on) {
                    Modifier.background(
                        color = MiuixTheme.colorScheme.primary,
                        shape = RoundedCornerShape(percent = 50),
                    )
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
    ) {
        // 内层滚动视口：宽度 = 胶囊内宽，文字超宽只在这里滚动，胶囊本身不动。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { viewportWidthPx = it.width }
                .horizontalScroll(scrollState),
            // 本页需要滚动时统一起始对齐（居中内容滚不到开头），否则保持居中。
            contentAlignment = if (marquee != null) Alignment.CenterStart else Alignment.Center,
        ) {
            Text(
                text = marquee?.displayText ?: text,
                style = textStyle,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                color = if (on) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}

/** 跑马灯规格：[displayText] = 单元 × 2 的滚动内容；[distancePx] = 一个单元的宽度（= 完整一圈）。 */
private data class MarqueeSpec(val displayText: String, val distancePx: Int)

/**
 * 按下阴影 + 点击的公共修饰符。
 *
 * 返回的 [Modifier]：
 * 1. `shadow`：仅当该板块「圆角」总闸开启**且**当前处于按下态时给 8dp 阴影，否则 0dp。
 *    默认态（圆角组总开关关）⇒ elevation 恒 0 ⇒ **零视觉变化**；开启后按下才出现与圆角一致的阴影轮廓。
 * 2. 按压反馈（**只在按住时出现**，松手即消失）：
 *    - 「圆角」总闸关：沿用 miuix [LocalIndication]（黑 10% 矩形高亮），与全 App 其它可点击件一致；
 *    - 「圆角」总闸开：**不能**沿用 [LocalIndication] —— 它画的是一整块直角矩形
 *      （miuix `MiuixIndication` 在 `drawContent()` 后 `drawRect(size = size)`，不带 shape，
 *      圆角外沿会露出直角高亮），改为在 `drawWithContent` 里按
 *      [com.nainiuzhen.wiki.utils.cardPressShadowShape] 的形状画一层黑 10% 覆盖
 *      （数值对齐 miuix `MiuixIndication` 的 `PRESS_ALPHA_DELTA = 0.10f`）。
 *
 * ### 为什么不能再加持久的 `.clip(shape)`（v31.1 回归教训）
 *
 * v31 曾把 `.clip(卡片形状)` 永久挂在 `clickable` 外层来"裁齐"miuix 的直角高亮（`37446c8`）。
 * 但 clip 连**内容**一起裁：卡片单元 = 图标区 + 名称胶囊的整列，16dp 圆角正好切进胶囊底部
 * —— 开「胶囊背景 + 卡片圆角（阴影同步开）」时，所有胶囊下半被永久削掉（用户实测截图）。
 * 按压反馈必须做成"只在按住时绘制的覆盖层"，**绝不能以任何形式常驻**。
 *
 * @param section 所属板块，决定读圆角组开关。
 * @param onClick 点击回调。
 * @return 已接好按下阴影与点击的修饰符，调用点用 `.then(...)` 接上即可。
 */
@Composable
fun rememberCardPressModifier(
    section: CardSection,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val state = LocalAppSettings.current
    val cornerOn = state.cardPressShadowEnabled(section)
    val shape = state.cardPressShadowShape(section)
    val shadowModifier = Modifier.shadow(
        elevation = if (cornerOn && pressed) 8.dp else 0.dp,
        shape = shape,
        clip = false,
    )
    return if (cornerOn) {
        shadowModifier
            .drawWithContent {
                drawContent()
                // 瞬态按压反馈：只在按住时按卡片形状画一层黑 10%，松手消失。
                // 不要改成 .clip(...)（见上方 KDoc 的 v31.1 回归教训）。
                if (pressed) {
                    drawOutline(
                        outline = shape.createOutline(size, layoutDirection, this),
                        color = Color.Black,
                        alpha = PRESS_FEEDBACK_ALPHA,
                    )
                }
            }
            .clickable(
                interactionSource = interaction,
                // indication 置空：miuix 的直角矩形高亮由上面的形状化覆盖层代替。
                indication = null,
                onClick = onClick,
            )
    } else {
        // 默认态（圆角总闸关）：卡片本来就是直角，miuix 的矩形高亮就是正确形状，零改动。
        shadowModifier.clickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            onClick = onClick,
        )
    }
}

/** 按压反馈覆盖层的透明度，对齐 miuix `MiuixIndication` 的 `PRESS_ALPHA_DELTA = 0.10f`。 */
private const val PRESS_FEEDBACK_ALPHA = 0.10f

/** 自动跑马灯：卡片进入屏幕 / 交互静止后，需满 3 秒才触发滚动。 */
private const val MARQUEE_IDLE_TRIGGER_MS = 3_000L

/** 自动跑马灯：两遍滚动之间的停顿时长。 */
internal const val MARQUEE_PASS_PAUSE_MS = 2_000L

/** 自动跑马灯：恒速滚动的速度（dp/s），适中速度。 */
private const val MARQUEE_SPEED_DP_PER_SECOND = 35f

/** 自动跑马灯：每个触发窗口内滚动遍数（滚一遍 → 停 2 秒 → 再滚一遍）。 */
private const val MARQUEE_PASS_COUNT = 2
