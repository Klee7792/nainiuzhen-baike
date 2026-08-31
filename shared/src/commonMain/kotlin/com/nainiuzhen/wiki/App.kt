package com.nainiuzhen.wiki

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nainiuzhen.wiki.data.AssetManager
import com.nainiuzhen.wiki.utils.SetStatusBarLightIcons
import com.nainiuzhen.wiki.data.repository.SpriteCacheManager
import com.nainiuzhen.wiki.data.source.SpriteSlicer
import com.nainiuzhen.wiki.ui.nav.AppNavHost
import com.nainiuzhen.wiki.ui.nav.LocalDataRepository
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.ui.nav.LocalSpriteRepository
import com.nainiuzhen.wiki.ui.nav.Navigator
import com.nainiuzhen.wiki.ui.nav.Route
import com.nainiuzhen.wiki.ui.theme.AppTheme
import com.nainiuzhen.wiki.utils.AppState
import com.nainiuzhen.wiki.utils.AppSettingsStore
import com.nainiuzhen.wiki.utils.LocalAppSettings
import com.nainiuzhen.wiki.utils.LocalUpdateAppSettings
import com.nainiuzhen.wiki.utils.AppVersion
import com.nainiuzhen.wiki.utils.LocalAppVersion
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.squircle.LocalSquircleEnabled
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 进程级已加载数据缓存（变更点 #27）：切后台再回前台、或 Activity 因配置变化重建时，
 * 只要进程存活即复用已加载数据，避免整体重新加载（资源加载较重，放 IO 线程亦不希望重复执行）。
 */
private var cachedLoadedData: com.nainiuzhen.wiki.data.AssetManager.LoadedData? = null

/**
 * 应用入口（:shared 公共入口）。由 :app 的 MainActivity 注入平台实现
 * [slicer] / [cache]，并传入 [isDebug]（决定是否展示黑名单物品 / NPC）。
 *
 * 内部流程：先以协程加载全部资源（构建 [com.nainiuzhen.wiki.data.AssetManager.LoadedData]），
 * 版本不符时清空切片缓存并重切；加载完成后注入 [LocalDataRepository] / [LocalSpriteRepository] /
 * [LocalNavigator]，渲染 [AppNavHost]。进程存活时复用已加载数据（[cachedLoadedData]）。
 */
@Composable
fun App(
    slicer: SpriteSlicer,
    cache: SpriteCacheManager,
    isDebug: Boolean,
    settings: AppSettingsStore,
    appVersion: AppVersion,
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
            LocalAppVersion provides appVersion,
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
    // 适配深色模式：以主题背景色铺底，跟随色彩模式（#139 / bug-v7 #1）
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 上方图标：512 方形源图，圆角显示（此处以主题色圆角方块 + 文字作为占位 Logo）
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MiuixTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "牛",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.height(28.dp))
            // 进度条：宽 100% 内间距，xy 居中
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
            )
            Spacer(Modifier.height(12.dp))
            // 加载中文字在进度条下方
            Text(text = "加载中…", color = MiuixTheme.colorScheme.onBackground)
        }
    }
}
