package com.nainiuzhen.wiki.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 卡片名称跑马灯的**页级协调器**（v36）。
 *
 * 存在意义：每张卡片文字长度不同 ⇒ 滚一圈的耗时不同。若各滚各的计时，同屏会参差不齐
 * （快的已经跑第二圈、慢的还在第一圈）。所以：
 *
 * - 每张卡片跑完一圈向协调器报一次到（[lapDone]）；
 * - **只有当前可见的超宽卡片全部报完**才统一计时 [MARQUEE_PASS_PAUSE_MS]，然后放行下一圈（[awaitLap]）。
 *
 * 即「等最慢的那个跑完，大家一起开始下一轮」，这是用户明确要的效果。
 *
 * 只滚 [MARQUEE_PASS_COUNT] 圈，放行第 2 圈后协调器不再推进（卡片自己结束）。
 */
class MarqueeCoordinator {
    /** 当前参与滚动的卡片（超宽 + 在屏幕可视区内）。 */
    private val participants = mutableSetOf<Any>()

    /** 本圈已报到的卡片。 */
    private val doneThisLap = mutableSetOf<Any>()

    /** 当前正在进行的圈号；全员报完后 +1。 */
    private var currentLap = 0

    /** 放行信号：emit(n) 表示「第 n 圈可以开始了」。replay=1 让中途加入的卡片也能立即拿到最新状态。 */
    private val _gate = MutableSharedFlow<Int>(replay = 1)

    @Suppress("unused")
    private val gate: Flow<Int> = _gate

    fun join(key: Any) {
        participants.add(key)
    }

    fun leave(key: Any) {
        participants.remove(key)
        doneThisLap.remove(key)
    }

    /**
     * 卡片跑完第 [lap] 圈（0-based）。当**所有参与者**都跑完当前圈时，
     * 用 [scope] 起一个协程：停 [MARQUEE_PASS_PAUSE_MS] 后放行下一圈。
     *
     * 过期的报到（[lap] 与 [currentLap] 不符，比如中途新加入的卡片）直接忽略，不干扰计数。
     */
    fun lapDone(key: Any, lap: Int, scope: CoroutineScope) {
        if (lap != currentLap) return
        doneThisLap.add(key)
        if (participants.isEmpty()) return
        if (!doneThisLap.containsAll(participants)) return
        currentLap += 1
        doneThisLap.clear()
        val nextLap = currentLap
        scope.launch {
            delay(MARQUEE_PASS_PAUSE_MS)
            _gate.emit(nextLap)
        }
    }

    /**
     * 等待第 [lap] 圈放行（第 0 圈不等 —— 卡片自己静止满 [MARQUEE_IDLE_TRIGGER_MS] 后就开始）。
     * 若放行信号已经发过（replay=1），这里会立即返回。
     */
    @OptIn(FlowPreview::class)
    suspend fun awaitLap(lap: Int) {
        if (lap <= 0) return
        _gate.first { it >= lap }
    }
}

/** 页级跑马灯协调器；未提供时各卡片自行其是（不会崩溃，只是不同步）。 */
val LocalMarqueeCoordinator = compositionLocalOf { MarqueeCoordinator() }

/** 提供一个页级 [MarqueeCoordinator] 给子树内的全部卡片共享。 */
@Composable
fun ProvideMarqueeCoordinator(content: @Composable () -> Unit) {
    val coordinator = remember { MarqueeCoordinator() }
    CompositionLocalProvider(LocalMarqueeCoordinator provides coordinator, content = content)
}

/** [CardNameCapsule] 内部用：量一段文本的排版宽度（单行、不换行），单位 px。 */
internal fun measureTextWidthPx(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
): Int = measurer.measure(
    text = AnnotatedString(text),
    style = style,
    maxLines = 1,
    softWrap = false,
).size.width

/** 供 [CardNameCapsule] 复用的 TextMeasurer 入口（避免在业务代码里散落 rememberTextMeasurer）。 */
@Composable
internal fun rememberCardTextMeasurer(): TextMeasurer = rememberTextMeasurer()
