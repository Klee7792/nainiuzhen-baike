package com.harvesttown.encyclopedia

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.harvesttown.encyclopedia.data.AssetManager
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
import com.harvesttown.encyclopedia.utils.LocalAppSettings
import com.harvesttown.encyclopedia.utils.LocalUpdateAppSettings
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.squircle.LocalSquircleEnabled
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 应用入口（:shared 公共入口）。由 :app 的 MainActivity 注入平台实现
 * [slicer] / [cache]，并传入 [isDebug]（决定是否展示黑名单物品 / NPC）。
 *
 * 内部流程：先以协程加载全部资源（构建 [com.harvesttown.encyclopedia.data.AssetManager.LoadedData]），
 * 版本不符时清空切片缓存并重切；加载完成后注入 [LocalDataRepository] / [LocalSpriteRepository] /
 * [LocalNavigator]，渲染 [AppNavHost]。
 */
@Composable
fun App(
    slicer: SpriteSlicer,
    cache: SpriteCacheManager,
    isDebug: Boolean,
) {
    var appState by remember { mutableStateOf(AppState()) }
    val updateAppState: (AppState) -> Unit = remember { { appState = it } }
    AppTheme(isDark = appState.isDark) {
        CompositionLocalProvider(
            LocalAppSettings provides appState,
            LocalUpdateAppSettings provides updateAppState,
            LocalSquircleEnabled provides appState.enableSquircle,
        ) {
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
    val loadedState = remember { mutableStateOf<AssetManager.LoadedData?>(null) }
    LaunchedEffect(Unit) {
        if (cache.needsRebuild()) cache.clear()
        val data = AssetManager(slicer, cache).loadAll(isDebug)
        cache.markBuilt()
        loadedState.value = data
    }
    val loaded = loadedState.value
    if (loaded == null) {
        LoadingScreen()
    } else {
        val backStack = rememberNavBackStack<Route>(Route.Home)
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
