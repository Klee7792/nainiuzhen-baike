package com.nainiuzhen.wiki.utils

import androidx.compose.runtime.withFrameNanos
import kotlin.math.roundToInt

/**
 * 连续 FPS 曲线日志（iOS 卡顿定位用）。
 *
 * 与 demo（`miuix/example .../utils/FPSMonitor.kt`）**同源算法**：5 秒滚动窗口、1% low、
 * 帧间隔 >500ms 按 idle 丢弃、窗口 1200 帧上限、MIN_SAMPLES=30。区别在于本类**不绘制任何 UI**，
 * 只把统计结果按稳定格式写进 [AppLog]，用于「定位 iOS 卡顿出现的来源与时机」。
 *
 * 设计约束（务必遵守）：
 * - 采样循环跑在 [start] 的调用协程里（由 `LaunchedEffect(Unit)` 提供帧时钟）。
 * - 采样期间**绝不写任何 Compose State**（不触发重组），只累加普通变量 + 调 [AppLog]。
 * - [start] 幂等：App 级只启一次，重复调用直接返回。
 * - [mark] 用于场景标记：打一行日志，并让随后 3 秒恢复 2 秒/行的高频输出
 *   （覆盖子页首帧冷启动卡顿）。
 *
 * 常态日志（单行、稳定可 grep）：
 * ```
 * FPS avg=58 low1=41 worst=48ms jank=3/120 idle=0 n=120
 * ```
 * - `avg` / `low1`：5 秒窗口内的平均帧率与 1% low 帧率；
 * - `worst`：窗口内最大帧耗时（ms）；
 * - `jank`：窗口内帧耗时 >33ms 的帧数 / 窗口帧数；
 * - `idle`：自上次输出以来被丢弃的空帧（帧间隔 >500ms）数量；
 * - `n`：窗口帧数。
 *
 * 噪声控制：若连续健康（avg≥58 且 jank==0）达 5 秒，输出降频为 5 秒/行；
 * 一旦不健康或刚 [mark] 过，立即恢复 2 秒/行。
 */
object FpsTracker {

    /** 采样循环是否在跑（幂等守卫；仅在主线程读写）。 */
    private var running = false

    /** 收到 [mark] 但尚未在帧回调里换算到帧时钟域时置位。 */
    private var highFreqRequested = false

    /** 高频输出窗口的截止帧时间（帧时钟域，纳秒）。 */
    private var highFreqUntilNs = 0L

    /**
     * 启动采样（幂等）。须在带帧时钟的协程里调用（如 `LaunchedEffect(Unit)`）。
     * 循环每帧采样一次，直到 [stop] 被调用或调用方协程被取消。
     */
    suspend fun start() {
        if (running) return
        running = true
        try {
            sampleLoop()
        } finally {
            running = false
        }
    }

    /** 停止采样：下一帧后退出循环。 */
    fun stop() {
        running = false
    }

    /**
     * 打一个场景标记，并让随后 3 秒恢复 2 秒/行的高频输出。
     *
     * @param label 场景标签（如 `push ItemList` / `pop` / `tab 1` / `dialog BasicDetail`）。
     */
    fun mark(label: String) {
        AppLog.i("FPS 场景: $label")
        highFreqRequested = true
    }

    /** 帧采样主循环（与 demo 同算法，仅把展示换成日志输出）。 */
    private suspend fun sampleLoop() {
        val samples = ArrayDeque<Long>(WINDOW_FRAME_CAP)
        var sumNs = 0L
        var lastFrameNs = 0L
        var nextRefreshNs = 0L
        // 自上次输出以来被丢弃的空帧计数
        var idleSinceReport = 0
        // 「连续健康」streak 起始帧时间：0 表示当前不健康
        var healthySinceNs = 0L

        while (running) {
            withFrameNanos { frameNs ->
                // 把 mark() 的请求换算到帧时钟域（避免与 System.nanoTime 时基不一致）。
                if (highFreqRequested) {
                    highFreqUntilNs = frameNs + MARK_HIGH_FREQ_NS
                    highFreqRequested = false
                }

                if (lastFrameNs != 0L) {
                    val delta = frameNs - lastFrameNs
                    if (delta > IDLE_THRESHOLD_NS) {
                        // 帧间隔过长（应用切后台 / 无渲染）⇒ 记为空帧并丢弃，不污染窗口
                        idleSinceReport++
                    } else if (delta in 1L..IDLE_THRESHOLD_NS) {
                        samples.addLast(delta)
                        sumNs += delta
                        // 5 秒滚动窗口：至少保留 MIN_SAMPLES 帧，且不超过帧数上限
                        while (sumNs > WINDOW_NS && samples.size > MIN_SAMPLES) {
                            sumNs -= samples.removeFirst()
                        }
                        while (samples.size > WINDOW_FRAME_CAP) {
                            sumNs -= samples.removeFirst()
                        }

                        if (frameNs >= nextRefreshNs && samples.size >= MIN_SAMPLES) {
                            val n = samples.size
                            val sorted = samples.toLongArray().also { it.sort() }
                            val avgNs = sumNs.toDouble() / n
                            val low1Count = (n / 100).coerceAtLeast(1)
                            var low1SumNs = 0L
                            for (i in n - low1Count until n) low1SumNs += sorted[i]
                            val low1AvgNs = low1SumNs.toDouble() / low1Count
                            val avg = (NS_PER_SECOND / avgNs).toInt()
                            val low1 = (NS_PER_SECOND / low1AvgNs).toInt()
                            val worstMs = (sorted[n - 1] / 1_000_000.0).roundToInt()
                            val jankCount = sorted.count { it > JANK_THRESHOLD_NS }

                            AppLog.i(
                                "FPS avg=$avg low1=$low1 worst=${worstMs}ms " +
                                    "jank=$jankCount/$n idle=$idleSinceReport n=$n",
                            )

                            // 连续健康判定：avg≥58 且 jank==0 持续 LONG_HEALTHY_NS 后降频到 5 秒/行
                            if (avg >= HEALTHY_AVG && jankCount == 0) {
                                if (healthySinceNs == 0L) healthySinceNs = frameNs
                            } else {
                                healthySinceNs = 0L
                            }
                            val longHealthy = healthySinceNs != 0L &&
                                frameNs - healthySinceNs >= LONG_HEALTHY_NS
                            val inHighFreq = frameNs < highFreqUntilNs
                            nextRefreshNs = frameNs +
                                if (inHighFreq || !longHealthy) FAST_INTERVAL_NS else SLOW_INTERVAL_NS

                            idleSinceReport = 0
                        }
                    }
                }
                lastFrameNs = frameNs
            }
        }
    }

    private const val NS_PER_SECOND = 1_000_000_000.0
    private const val WINDOW_NS = 5L * 1_000_000_000L // 5 秒滚动窗口
    private const val WINDOW_FRAME_CAP = 1_200 // 帧数上限（~240Hz × 5s）
    private const val MIN_SAMPLES = 30 // 至少约 0.5s 数据才输出
    private const val FAST_INTERVAL_NS = 2_000_000_000L // 常态 / 高频：2 秒一行
    private const val SLOW_INTERVAL_NS = 5_000_000_000L // 健康降频：5 秒一行
    private const val MARK_HIGH_FREQ_NS = 3_000_000_000L // mark 后 3 秒高频
    private const val LONG_HEALTHY_NS = 5_000_000_000L // 连续健康 5 秒才降频
    private const val IDLE_THRESHOLD_NS = 500_000_000L // 帧间隔 >500ms 视为空帧，丢弃
    private const val JANK_THRESHOLD_NS = 33_000_000L // >33ms 记为 jank
    private const val HEALTHY_AVG = 58 // 健康帧率下限
}
