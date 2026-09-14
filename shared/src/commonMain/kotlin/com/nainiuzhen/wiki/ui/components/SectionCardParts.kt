package com.nainiuzhen.wiki.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
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
 * ### 自动跑马灯（仅超宽时启用）
 *
 * - 触发：文字宽度 > 可视区宽度（胶囊内宽）才启用；不超宽时行为与旧版完全一致（居中、静止）。
 * - 节奏：卡片进入屏幕后静止满 3 秒 → 以约 35dp/s 恒速从头滚到尾 → 停 2 秒 → 再滚一遍
 *   → 回位到起点并恢复居中观感 → 重新等下一个「静止满 3 秒」窗口循环。
 * - 打断：用户手指横向拖动 / 甩动立刻打断自动滚动，且必须重新静止满 3 秒才会再次触发。
 * - 实现：见 [CardNameCapsule] 体内的 `MARQUEE_*` 常量与两个 `LaunchedEffect`；
 *   未使用 `basicMarquee`（其语义与「两遍后回位」不匹配）。
 *
 * @param section 所属板块，决定读「文字胶囊」开关。
 * @param text 名称文本。
 * @param modifier 外层修饰符（一般无需传，默认 `fillMaxWidth()`）。
 * @param style 文字样式；传 `null`（默认）时退回主题默认样式 `MiuixTheme.textStyles.main`，
 *   等价于直接调用 miuix `Text` 不传 `style`（miuix `Text` 的默认 `style` 正是 `LocalTextStyles.current.main`）。
 * @param fontSize 字号；传 `null`（默认）时向 `Text` 传 [TextUnit.Unspecified]，由 `Text` 从 [style] 取字号。
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
    val density = LocalDensity.current.density

    // —— 超宽检测 ——
    // softWrap=false 且处于横向滚动容器（无限宽约束）内时，Text 的 hasVisualOverflow
    // 恒为 false（无上界可比），必须拿文本布局宽度与可视区宽度自己比。
    // 直接读滚动容器自己算好的可滚动余量（= 内容宽 − 可视宽），布局完成后才 > 0。
    // 比自己比对 Text 布局宽度可靠：softWrap=false + 横向滚动的无限宽约束下，
    // Text 自带的溢出标志恒为 false。用 derivedStateOf 只在数值/布尔翻转时重组。
    val scrollRangePx by remember { derivedStateOf { scrollState.maxValue } }
    val overflow = scrollRangePx > 0

    // —— 自动跑马灯状态 ——
    // 用户每次开始拖动（DragInteraction.Start）自增，作为主循环的「重启令牌」：
    // 令牌变化 ⇒ 取消主循环协程（打断进行中的自动滚动 / 3 秒等待）并从头等起。
    // 注意：程序化的 animateScrollBy 不会派发 DragInteraction（只有用户手势会），
    // 所以自动滚动本身不会误触发令牌。
    var dragRestartToken by remember { mutableIntStateOf(0) }
    // 自动滚动会话是否激活：激活时内容对齐改为起始对齐 —— 超宽文本 + 居中对齐时
    // 文本起始段在可视区外（两侧对称溢出），而滚动范围只覆盖 [0, max]，居中对齐下
    // 永远滚不到开头，所以滚动期间必须让内容从 0 偏移起排。
    var marqueeActive by remember { mutableStateOf(false) }

    // 滚动偏移离开起点（自动跑马灯滚动中或用户拖过）⇒ 起始对齐；静止在起点 ⇒ 保持居中。
    // 用 derivedStateOf 只在布尔翻转时通知重组，避免逐帧重组。
    val scrolledFromStart by remember { derivedStateOf { scrollState.value > 0 } }
    val contentAlignment = if (marqueeActive || scrolledFromStart) {
        Alignment.CenterStart
    } else {
        Alignment.Center
    }

    // 用户拖动监视：一旦手指开始横向拖动 / 甩动就自增令牌，打断自动跑马灯
    // （打断后由主循环的下一个实例重新等 3 秒静止窗口）。
    LaunchedEffect(overflow) {
        if (!overflow) return@LaunchedEffect
        scrollState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) dragRestartToken++
        }
    }

    // 主循环：静止满 3 秒 → 恒速滚两遍（遍间停 2 秒）→ 回位 → 重新等下一个窗口。
    // 协程随 LaunchedEffect 的 key 变化（用户打断 / 超宽状态翻转）或离开组合自动取消，无残留。
    LaunchedEffect(overflow, dragRestartToken) {
        if (!overflow) return@LaunchedEffect
        try {
            while (true) {
                // ① 静止满 3 秒才触发。
                delay(MARQUEE_IDLE_TRIGGER_MS)
                // 用户仍在拖动 / 甩动中：继续等下一个 3 秒窗口。
                if (scrollState.isScrollInProgress) continue
                // ② 滚动距离与时长按「最新布局结果」计算（字号缩放等布局变化后自动跟进）。
                val distancePx = scrollRangePx.toFloat()
                if (distancePx <= 0f) continue
                val durationMs = (distancePx / (density * MARQUEE_SPEED_DP_PER_SECOND)) * 1000f
                // 激活会话：复位到起点并切换为起始对齐。
                marqueeActive = true
                scrollState.scrollBy(-scrollState.value.toFloat())
                // ③ 恒速从头滚到尾两遍，两遍之间停 2 秒。
                repeat(MARQUEE_PASS_COUNT) { pass ->
                    scrollState.animateScrollBy(
                        value = distancePx,
                        animationSpec = tween(
                            durationMillis = durationMs.toInt().coerceIn(1, Int.MAX_VALUE),
                            easing = LinearEasing,
                        ),
                    )
                    if (pass < MARQUEE_PASS_COUNT - 1) delay(MARQUEE_PASS_PAUSE_MS)
                }
                // ④ 回位到起点并恢复居中观感，随后进入下一个 3 秒空闲窗口。
                scrollState.scrollBy(-scrollState.value.toFloat())
                marqueeActive = false
            }
        } finally {
            // 任何退出路径（用户打断、超宽状态翻转、离开组合）都回到静止形态。
            marqueeActive = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
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
                .horizontalScroll(scrollState),
            contentAlignment = contentAlignment,
        ) {
            Text(
                text = text,
                style = style ?: MiuixTheme.textStyles.main,
                fontSize = fontSize ?: TextUnit.Unspecified,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                color = if (on) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}

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
private const val MARQUEE_PASS_PAUSE_MS = 2_000L

/** 自动跑马灯：恒速滚动的速度（dp/s），适中速度。 */
private const val MARQUEE_SPEED_DP_PER_SECOND = 35f

/** 自动跑马灯：每个触发窗口内滚动遍数（滚一遍 → 停 2 秒 → 再滚一遍）。 */
private const val MARQUEE_PASS_COUNT = 2
