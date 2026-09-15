package com.nainiuzhen.wiki

import androidx.compose.ui.window.ComposeUIViewController
import com.nainiuzhen.wiki.platform.bundleAssetBytes
import com.nainiuzhen.wiki.platform.IosAppSettingsStore
import com.nainiuzhen.wiki.platform.IosSpriteCacheManager
import com.nainiuzhen.wiki.platform.IosSpriteSlicer
import com.nainiuzhen.wiki.utils.AppVersion
import com.nainiuzhen.wiki.utils.AppLog
import com.nainiuzhen.wiki.utils.IosStartupTime
import kotlin.native.setUnhandledExceptionHook
import platform.Foundation.NSBundle
import platform.UIKit.UIViewController

/**
 * 挂载未捕获异常钩子：任何逃出 Compose / 协程的异常都写进诊断日志
 * （Documents/app_log.txt），否则真机上只会表现为「闪退 / 黑屏」，无从排查。
 */
private var crashHookInstalled = false

private fun installCrashHook() {
    if (crashHookInstalled) return
    crashHookInstalled = true
    setUnhandledExceptionHook { throwable ->
        AppLog.e("UNCAUGHT 未捕获异常", throwable)
    }
}

/**
 * iOS 应用入口（对应 Android 的 `MainActivity` 平台装配层）。
 *
 * Swift 侧经 `MainViewControllerKt.MainViewController()` 调用（framework baseName = shared）。
 * 只做平台装配：构建 iOS 专属切片器 / 缓存管理器 / 设置存储并注入公共入口 [App]；
 * 主题、深色模式、资源加载、导航与页面渲染等全部逻辑均在 [App] 与 :shared 模块中实现。
 *
 * isDebug 固定 false：iOS 分发只有 release（TrollStore 侧载未签名 ipa），黑名单过滤照常生效。
 */
fun MainViewController(): UIViewController {
    installCrashHook()
    // 先打点再打日志：日志里的时间戳依赖它（见 IosStartupTime 注释）。
    IosStartupTime.mark()
    AppLog.i("MainViewController 入口")
    AppLog.i("bundle 路径：${NSBundle.mainBundle.bundlePath}")
    // 预检素材包是否真的进了 bundle（只读一次；失败也会在加载阶段再记一条）
    val packSize = runCatching { bundleAssetBytes("assets.pack").size }.getOrNull()
    AppLog.i("assets.pack 预检：${packSize?.let { "$it 字节" } ?: "缺失"}")
    val versionName = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString")
        as? String ?: "1.0.0"
    val versionCode = (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion")
        as? String)?.toIntOrNull() ?: 1
    return ComposeUIViewController {
        App(
            slicer = IosSpriteSlicer(),
            cache = IosSpriteCacheManager(version = versionCode),
            isDebug = false,
            settings = IosAppSettingsStore(),
            appVersion = AppVersion(versionName, versionCode),
        )
    }
}
