package com.nainiuzhen.wiki.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/**
 * 窗口分档判定层（一期 · 阶段 1 地基，设计文档 §4.2）。
 *
 * ## 只判「窗口有多少空间」，不判「设备是什么」
 * 分屏、自由窗口、折叠屏展开/折叠态下，同一台设备的「手机/平板」属性会变；
 * 而 `sw600dp` 资源限定符取的是显示区**最小边**、**不随窗口变化**，在分屏/自由窗口下会给出错误答案。
 * 因此本文件一律以**当前窗口的实际尺寸**（[LocalWindowInfo] 的 `containerDpSize`）为唯一依据，
 * 与 AndroidX 官方「避免用物理硬件属性做布局决策」的指导一致。
 *
 * ## 为什么不用 `androidx.window` / `currentWindowAdaptiveInfo()`
 * 设计文档 §4.2 理想落点是 `currentWindowAdaptiveInfo().windowSizeClass`，但本工程实际情况不适合：
 * 1. `:shared` 目前只有 `androidLibrary` 一个 target，把 Android-only 的 AAR（`androidx.window`）
 *    放进 `commonMain` 存在 **metadata 编译兼容风险**；
 * 2. `material3-adaptive`（`currentWindowAdaptiveInfo()` 所在）**本地 Gradle 缓存里根本没有**，
 *    引入它需要**联网拉取新依赖**，与本轮「零新增依赖」的约束冲突。
 *
 * 而本文件只做**断点比较** —— 断点本身就是稳定常数（见下）。用 Compose 自带的
 * [LocalWindowInfo.current]`.containerDpSize` 语义等价、零新增依赖；工程内
 * `ui/components/DialogSizing.kt` 已经在使用同一 API，可靠且一致。
 *
 * > 后续若要支持折叠铰链避让等高级形态，再评估是否显式引入 `androidx.window`。
 */

// 横向分档断点 = AndroidX WindowSizeClass 官方断点值，判断时必须从大到小。
private val WIDTH_LARGE = 1200.dp
private val WIDTH_EXPANDED = 840.dp
private val WIDTH_MEDIUM = 600.dp
private val HEIGHT_MEDIUM = 480.dp

/** 当前窗口的横向分档（只看「窗口有多少空间」，不看设备类型 —— 分屏/自由窗口下设备属性会骗人）。 */
enum class WindowClass { COMPACT, MEDIUM, EXPANDED, LARGE }

/**
 * 读取当前窗口横向分档。
 *
 * 断点（与 AndroidX WindowSizeClass 一致，**必须从大到小判断**）：
 * - `>= 1200dp` → [WindowClass.LARGE]
 * - `>= 840dp`  → [WindowClass.EXPANDED]
 * - `>= 600dp`  → [WindowClass.MEDIUM]
 * - 否则        → [WindowClass.COMPACT]
 */
@Composable
fun rememberWindowClass(): WindowClass {
    val width = LocalWindowInfo.current.containerDpSize.width
    return when {
        width >= WIDTH_LARGE -> WindowClass.LARGE
        width >= WIDTH_EXPANDED -> WindowClass.EXPANDED
        width >= WIDTH_MEDIUM -> WindowClass.MEDIUM
        else -> WindowClass.COMPACT
    }
}

/**
 * 矮屏判定：窗口高 < 480dp 时命中。
 *
 * 手机横屏（如 800×360）命中；而 Pura X Max 展开态(940×665) 高 665dp > 480dp，**不**命中矮窗口判定。
 */
@Composable
fun isShortWindow(): Boolean =
    LocalWindowInfo.current.containerDpSize.height < HEIGHT_MEDIUM
