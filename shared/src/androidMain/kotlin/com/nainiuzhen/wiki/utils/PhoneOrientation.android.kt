package com.nainiuzhen.wiki.utils

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/** 大屏判定阈值（dp）：与 Android `sw600dp` 资源限定符口径一致。 */
private const val LARGE_SCREEN_SMALLEST_WIDTH_DP = 600

/**
 * Android 实现：通过宿主的 [Activity] 应用屏幕方向请求。
 *
 * 沿用工程内既有的平台副作用写法（同 `StatusBar.android.kt` / `OnResumeEffect.android.kt`）：
 * 从 [LocalView] 取 context 并上溯到 [Activity]，不新造全局单例。
 * 依赖由 miuix 传递引入，无需新增依赖。
 *
 * ## ⚠️ 大屏必须由业务侧显式放行，不能指望系统忽略方向请求
 * 原实现无条件请求 `SCREEN_ORIENTATION_PORTRAIT`，理由写的是「Android 16/17 起系统在
 * `≥sw600dp` 大屏上会忽略该请求」。**该假设实测不成立**：MuMu 模拟器 sw=726dp（大屏口径），
 * 系统并未忽略 `PORTRAIT`，应用被硬锁竖屏 —— 连模拟器自带的「旋转屏幕」强转都不生效，
 * 大屏横屏布局被彻底废掉。
 *
 * 现改为**用设备自身的最小宽度**（[android.content.res.Configuration.smallestScreenWidthDp]，
 * 即 Android 用来判定 `sw600dp` 的那个值）自己判：
 * - **大屏（≥600dp）** → 恒请求 [ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED]，**不锁方向**；
 * - 手机（<600dp）且 `allowLandscape = false` → [ActivityInfo.SCREEN_ORIENTATION_PORTRAIT]；
 * - 手机且 `allowLandscape = true` → `UNSPECIFIED`。
 *
 * 取「设备最小宽度」而非「当前窗口尺寸」的原因：锁方向是**设备级**能力判断，不是布局判断。
 * `smallestScreenWidthDp` 不随当前方向/窗口变化（竖屏 726×1105dp 与横屏 1105×726dp 同为 726dp），
 * 因此不会出现「竖着判成大屏、横着判成手机」的自相矛盾；折叠屏展开/折叠时该值随形态变化，
 * 行为也正好符合预期。
 *
 * @param allowLandscape 手机端是否放行横屏；大屏端此参数不生效（恒自由旋转）。
 */
@Composable
actual fun ApplyPhoneOrientation(allowLandscape: Boolean) {
    val view = LocalView.current
    val activity = view.context as? Activity

    // 读不到 Activity（理论上不会发生）时按手机处理，保持原有默认行为。
    val isLargeScreen =
        (activity?.resources?.configuration?.smallestScreenWidthDp ?: 0) >= LARGE_SCREEN_SMALLEST_WIDTH_DP

    val targetOrientation = if (allowLandscape || isLargeScreen) {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    } else {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    // key = (目标方向, Activity)：首帧执行一次 + 开关变化/设备形态变化时立即重跑，
    // 覆盖「启动应用一次 + 切换立即应用」两种时机。
    DisposableEffect(targetOrientation, activity) {
        activity?.setRequestedOrientation(targetOrientation)
        onDispose { }
    }
}
