package com.nainiuzhen.wiki

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import com.nainiuzhen.wiki.platform.bundleAssetBytes
import com.nainiuzhen.wiki.platform.IosAppSettingsStore
import com.nainiuzhen.wiki.platform.IosCacheDirGuard
import com.nainiuzhen.wiki.platform.IosSpriteCacheManager
import com.nainiuzhen.wiki.platform.IosSpriteSlicer
import com.nainiuzhen.wiki.utils.AppVersion
import com.nainiuzhen.wiki.utils.AppLog
import com.nainiuzhen.wiki.utils.IosStartupTime
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook
import platform.Foundation.NSBundle
import platform.UIKit.UIViewController

/**
 * 挂载未捕获异常钩子：任何逃出 Compose / 协程的异常都写进诊断日志
 * （Documents/app_log.txt），否则真机上只会表现为「闪退 / 黑屏」，无从排查。
 */
private var crashHookInstalled = false

@OptIn(ExperimentalNativeApi::class)
private fun installCrashHook() {
    if (crashHookInstalled) return
    crashHookInstalled = true
    setUnhandledExceptionHook { throwable ->
        AppLog.e("UNCAUGHT 未捕获异常", throwable)
    }
}

/**
 * iOS 并行渲染总开关（可回退）。
 *
 * ① 置 true 时，Compose 会在**专用渲染线程**编码绘制命令，UI 线程与渲染线程解耦 —— 这是
 *    「切换标签 / 弹窗滞后、厚重感」的典型解药，提升滚动与转场流畅度。
 * ② 必须在 `ComposeUIViewController(configure = { ... })` 作用域内设置；**在 configure 外
 *    修改无效**（CMP 1.12.0 KDoc 原文：Changing this setting outside of `configure` argument
 *    scope has no effect）。
 * ③ 依据：同款设备 + 同 iOS 版本下，miuix example 的 iOS demo 即使用该配置且稳定 60FPS。
 * ④ 若真机出现「截图内容滞后一拍」或 UIKit interop（分享面板）异常，改 false 即可回退，
 *    无需改动其他任何代码。
 */
private const val IOS_PARALLEL_RENDERING = true

/**
 * iOS 应用入口（对应 Android 的 `MainActivity` 平台装配层）。
 *
 * Swift 侧经 `MainViewControllerKt.MainViewController()` 调用（framework baseName = shared）。
 * 只做平台装配：构建 iOS 专属切片器 / 缓存管理器 / 设置存储并注入公共入口 [App]；
 * 主题、深色模式、资源加载、导航与页面渲染等全部逻辑均在 [App] 与 :shared 模块中实现。
 *
 * isDebug 固定 false：iOS 分发只有 release（TrollStore 侧载未签名 ipa），黑名单过滤照常生效。
 */
@OptIn(ExperimentalComposeUiApi::class)
fun MainViewController(): UIViewController {
    installCrashHook()
    // 先打点再打日志：日志里的时间戳依赖它（见 IosStartupTime 注释）。
    IosStartupTime.mark()
    AppLog.i("MainViewController 入口")
    AppLog.i("bundle 路径：${NSBundle.mainBundle.bundlePath}")
    // 启动最早期（早于第一帧）自愈缓存目录并落盘探针：Metal 管线缓存写不进去会让
    // 每次冷启动都退回「先卡后流畅」（见 IosCacheDirGuard 注释）。
    IosCacheDirGuard.ensureAndProbe()
    // 预检素材包是否真的进了 bundle（只读一次；失败也会在加载阶段再记一条）
    val packSize = runCatching { bundleAssetBytes("assets.pack").size }.getOrNull()
    AppLog.i("assets.pack 预检：${packSize?.let { "$it 字节" } ?: "缺失"}")
    val versionName = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString")
        as? String ?: "1.0.0"
    val versionCode = (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion")
        as? String)?.toIntOrNull() ?: 1
    return ComposeUIViewController(
        configure = {
            parallelRendering = IOS_PARALLEL_RENDERING
        },
    ) {
        App(
            slicer = IosSpriteSlicer(),
            cache = IosSpriteCacheManager(version = versionCode),
            isDebug = false,
            settings = IosAppSettingsStore(),
            appVersion = AppVersion(versionName, versionCode),
        )
    }
}
