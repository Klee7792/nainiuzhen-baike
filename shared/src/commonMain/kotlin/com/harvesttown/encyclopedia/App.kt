package com.harvesttown.encyclopedia

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.harvesttown.encyclopedia.data.AssetManager
import com.harvesttown.encyclopedia.utils.SetStatusBarLightIcons
import com.harvesttown.encyclopedia.data.repository.SpriteCacheManager
import com.harvesttown.encyclopedia.data.source.SpriteSlicer
import com.harvesttown.encyclopedia.ui.nav.AppNavHost
import com.harvesttown.encyclopedia.ui.nav.LocalDataRepository
import com.harvesttown.encyclopedia.ui.nav.LocalNavigator
import com.harvesttown.encyclopedia.ui.nav.LocalSpriteRepository
import com.harvesttown.encyclopedia.ui.nav.Navigator
import com.harvesttown.encyclopedia.ui.nav.Route
import com.harvesttown.encyclopedia.ui.theme.AppTheme
import com.harvesttown.encyclopedia.utils.AppState
import com.harvesttown.encyclopedia.utils.AppSettingsStore
import com.harvesttown.encyclopedia.utils.LocalAppSettings
import com.harvesttown.encyclopedia.utils.LocalUpdateAppSettings
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.squircle.LocalSquircleEnabled
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 进程级已加载数据缓存（变更点 #27）：切后台再回前台、或 Activity 因配置变化重建时，
 * 只要进程存活即复用已加载数据，避免整体重新加载（资源加载较重，放 IO 线程亦不希望重复执行）。
 */
private var cachedLoadedData: com.harvesttown.encyclopedia.data.AssetManager.LoadedData? = null

/**
 * 应用入口（:shared 公共入口）。由 :app 的 MainActivity 注入平台实现
 * [slicer] / [cache]，并传入 [isDebug]（决定是否展示黑名单物品 / NPC）。
 *
 * 内部流程：先以协程加载全部资源（构建 [com.harvesttown.encyclopedia.data.AssetManager.LoadedData]），
 * 版本不符时清空切片缓存并重切；加载完成后注入 [LocalDataRepository] / [LocalSpriteRepository] /
 * [LocalNavigator]，渲染 [AppNavHost]。进程存活时复用已加载数据（[cachedLoadedData]）。
 */
@Composable
fun App(
    slicer: SpriteSlicer,
    cache: SpriteCacheManager,
    isDebug: Boolean,
    settings: AppSettingsStore,
) {
    var appState by remember { mutableStateOf(settings.load()) }
    val updateAppState: (AppState) -> Unit = remember { { new -> appState = new; settings.save(new) } }
    // 解析当前是否为深色主题（系统模式跟随系统），用于状态栏图标深浅（#139：浅色模式白字看不见）。
    val isDark = when (appState.colorMode) {
        1 -> true
        2 -> false
        else -> isSystemInDarkTheme()
    }
    AppTheme(colorMode = appState.colorMode, monet = appState.monet) {
        CompositionLocalProvider(
            LocalAppSettings provides appState,
            LocalUpdateAppSettings provides updateAppState,
            LocalSquircleEnabled provides appState.enableSquircle,
        ) {
            SetStatusBarLightIcons(light = !isDark)
            AppRoot(slicer = slicer, cache = cache, isDebug = isDebug)
        }
    }
}

@Composable
private fun AppRoot(
    slicer: SpriteSlicer,
    cache: SpriteCacheManager,
    isDebug: Boolean,
) {
    // 进程存活且已加载过时，初始值直接复用 [cachedLoadedData]，避免切后台回前台
    // （Activity 重建导致 remember 重置）时闪一下 LoadingScreen，被误认为「重新加载」。
    val loadedState = remember { mutableStateOf(cachedLoadedData) }
    LaunchedEffect(Unit) {
        // 进程存活且版本未变更时直接复用已加载数据（变更点 #27，避免后台回前台重新加载）。
        if (cachedLoadedData == null || cache.needsRebuild()) {
            if (cache.needsRebuild()) cache.clear()
            // 资源加载（读 121 个文件 + 解析 26 个 plist）较重，放到 IO 线程，
            // 避免阻塞主线程导致首启动 ANR；主线程仅负责展示 LoadingScreen。
            val data = withContext(Dispatchers.IO) {
                AssetManager(slicer, cache).loadAll(isDebug)
            }
            cache.markBuilt()
            cachedLoadedData = data
        }
        loadedState.value = cachedLoadedData
    }
    val loaded = loadedState.value
    if (loaded == null) {
        LoadingScreen()
    } else {
        val backStack = rememberNavBackStack<Route>(Route.Main)
        val navigator = remember { Navigator(backStack) }
        CompositionLocalProvider(
            LocalDataRepository provides loaded.data,
            LocalSpriteRepository provides loaded.sprite,
            LocalNavigator provides navigator,
        ) {
            AppNavHost(backStack = backStack, navigator = navigator)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "加载中…", color = MiuixTheme.colorScheme.onBackground)
    }
}
