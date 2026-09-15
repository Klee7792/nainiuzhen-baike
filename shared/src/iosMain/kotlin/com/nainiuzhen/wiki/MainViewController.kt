package com.nainiuzhen.wiki

import androidx.compose.ui.window.ComposeUIViewController
import com.nainiuzhen.wiki.platform.IosAppSettingsStore
import com.nainiuzhen.wiki.platform.IosSpriteCacheManager
import com.nainiuzhen.wiki.platform.IosSpriteSlicer
import com.nainiuzhen.wiki.utils.AppVersion
import com.nainiuzhen.wiki.utils.IosStartupTime
import platform.Foundation.NSBundle
import platform.UIKit.UIViewController

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
    // 进程级启动打点：必须在构建 Compose 根之前（见 IosStartupTime 注释）。
    IosStartupTime.mark()
    val versionName = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString")
        as? String ?: "1.3.8"
    val versionCode = (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion")
        as? String)?.toIntOrNull() ?: 38
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
