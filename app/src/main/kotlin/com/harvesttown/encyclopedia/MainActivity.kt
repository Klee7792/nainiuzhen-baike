package com.harvesttown.encyclopedia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.harvesttown.encyclopedia.data.source.AppContextHolder
import com.harvesttown.encyclopedia.platform.AndroidSpriteCacheManager
import com.harvesttown.encyclopedia.platform.AndroidSpriteSlicer

/**
 * Android 应用入口（:app 模块唯一 Activity，已在 AndroidManifest 中声明为 LAUNCHER）。
 *
 * 本类只做平台装配：构建 Android 专属的切片器 [AndroidSpriteSlicer] 与切片缓存管理器
 * [AndroidSpriteCacheManager]，并把它们注入公共入口 [App]。缓存版本号取自 [BuildConfig.VERSION_CODE]，
 * 用于判定切片缓存是否过期（版本不符时 [App] 内部会自动清空并重切）。
 *
 * 主题、深色模式、资源加载、导航与页面渲染等全部逻辑均在 [App] 与 :shared 模块中实现，
 * 因此本文件保持极薄，不涉及任何业务/UI 细节。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 注入 Android Context，供 :shared 经原生 AssetManager 读取 assets/ 资源。
        AppContextHolder.init(this)
        setContent {
            App(
                slicer = AndroidSpriteSlicer(),
                cache = AndroidSpriteCacheManager(
                    cacheDir = cacheDir.absolutePath,
                    version = BuildConfig.VERSION_CODE,
                ),
                isDebug = BuildConfig.DEBUG,
            )
        }
    }
}
