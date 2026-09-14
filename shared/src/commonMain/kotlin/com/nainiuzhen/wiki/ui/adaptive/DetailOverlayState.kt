package com.nainiuzhen.wiki.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * 记录「右栏当前开着哪些弹窗」，供左栏做拦截。
 *
 * **要解决的问题**：miuix `OverlayDialog` 默认 `renderInRootScaffold = true`，会把弹窗**连同遮罩**
 * 渲染进最近的 miuix `Scaffold`。分栏时子页跑在右栏、自带 `AppSubPageScaffold`，于是弹窗天然
 * 以右栏为基准居中、遮罩只盖右栏 —— 好处是居中精确，**副作用是左栏完全没被遮罩覆盖**，
 * 弹窗开着时点左栏会**直接跳转并把弹窗盖掉**，观感上是「弹窗被无视了」。
 *
 * 本类补上这一环：左栏在弹窗期间自己盖一层等价的遮罩，点它 = 关掉最上层弹窗（而不是跳转）。
 * 单栏（手机）下 [LocalDetailOverlay] 为 null，弹窗不注册，**行为与改动前完全一致**。
 */
class DetailOverlayState {
    // 用列表当栈：详情弹窗之上还可能再开一层（如「来源」物品弹窗），关的必须是最上层那个。
    private val dismissers = mutableStateListOf<() -> Unit>()

    /** 当前是否有弹窗开着（读取它会订阅变化）。 */
    val isOpen: Boolean get() = dismissers.isNotEmpty()

    internal fun register(dismiss: () -> Unit) {
        dismissers.add(dismiss)
    }

    internal fun unregister(dismiss: () -> Unit) {
        dismissers.remove(dismiss)
    }

    /** 关掉最上层的那个弹窗。 */
    fun dismissTop() {
        dismissers.lastOrNull()?.invoke()
    }
}

/**
 * 右栏弹窗宿主。分栏时为非 null，单栏时为 null（弹窗保持原行为，不做拦截）。
 */
val LocalDetailOverlay = compositionLocalOf<DetailOverlayState?> { null }

/** 创建并记住一个 [DetailOverlayState]。 */
@Composable
fun rememberDetailOverlayState(): DetailOverlayState = remember { DetailOverlayState() }

/**
 * 把一个弹窗登记进 [LocalDetailOverlay]（仅当 [show] 为 true 时登记），使左栏的拦截层能关掉它。
 *
 * 在弹窗 composable 内部调用即可；不在分栏环境（[LocalDetailOverlay] 为 null）时是空操作。
 *
 * @param show 弹窗当前是否显示。
 * @param onDismiss 关闭回调（通常就是弹窗自己的 `onDismissRequest`）。
 */
@Composable
fun RegisterDetailOverlay(show: Boolean, onDismiss: () -> Unit) {
    val host = LocalDetailOverlay.current ?: return
    // 用 rememberUpdatedState 取最新回调，避免把「每次重组都变的 lambda」当作 DisposableEffect 的 key
    // 反复注销重注册；对外只暴露一个引用稳定的 lambda。
    val latest by rememberUpdatedState(onDismiss)
    val stableDismiss = remember(host) { { latest() } }
    DisposableEffect(host, show) {
        if (show) host.register(stableDismiss)
        onDispose { host.unregister(stableDismiss) }
    }
}
