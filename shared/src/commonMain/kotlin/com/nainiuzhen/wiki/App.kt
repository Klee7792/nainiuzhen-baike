package com.nainiuzhen.wiki

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nainiuzhen.wiki.data.AssetManager
import com.nainiuzhen.wiki.data.source.AssetLoader
import com.nainiuzhen.wiki.utils.SetStatusBarLightIcons
import com.nainiuzhen.wiki.utils.ApplyPhoneOrientation
import com.nainiuzhen.wiki.data.repository.SpriteCacheManager
import com.nainiuzhen.wiki.data.source.SpriteSlicer
import com.nainiuzhen.wiki.ui.home.MainScreen
import com.nainiuzhen.wiki.ui.nav.LocalDataRepository
import com.nainiuzhen.wiki.ui.nav.LocalSpriteRepository
import com.nainiuzhen.wiki.ui.theme.AppTheme
import com.nainiuzhen.wiki.utils.AppState
import com.nainiuzhen.wiki.utils.AppSettingsStore
import com.nainiuzhen.wiki.utils.LocalAppSettings
import com.nainiuzhen.wiki.utils.LocalUpdateAppSettings
import com.nainiuzhen.wiki.utils.AppVersion
import com.nainiuzhen.wiki.utils.LocalAppVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.isSystemInDarkTheme
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
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
 * 再预热切片把全部素材帧切片进内存（真实 0-100% 进度展示于 LoadingScreen，切片不落盘）；
 * 全部就绪后注入 [LocalDataRepository] / [LocalSpriteRepository]，
 * 渲染 [com.nainiuzhen.wiki.ui.home.MainScreen]（它自己持有全应用唯一的返回栈）。
 * 进程存活时复用已加载数据（[cachedLoadedData]）。
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
            // 按「手机横屏」设置应用方向：启动时一次 + 开关切换时立即生效（大屏适配 §8.3）。
            ApplyPhoneOrientation(allowLandscape = appState.allowPhoneLandscape)
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
    // 加载进度（去磁盘化后的「内存切片」阶段）：done < 0 表示尚未进入切片阶段（准备数据中）；
    // 切片阶段 done ∈ 0..total，驱动 LoadingScreen 的确定进度条与百分比文字。
    var preloadDone by remember { mutableStateOf(-1) }
    var preloadTotal by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        // 进程存活时直接复用已加载数据（变更点 #27，避免后台回前台重新加载）。
        if (cachedLoadedData == null) {
            // 去磁盘化：切片全量驻内存、不再写盘；启动时顺手删除旧版本残留的磁盘切片目录
            // （一次性清理，幂等，try-catch 包住避免清理失败阻塞启动）。
            runCatching { cache.clear() }
            // 资源加载（读 121 个文件 + 解析 26 个 plist）较重，放到 IO 线程，
            // 避免阻塞主线程导致首启动 ANR；主线程仅负责展示 LoadingScreen。
            val data = withContext(Dispatchers.IO) {
                AssetManager(slicer, cache).loadAll(isDebug)
            }
            // 预热切片：全部帧切片进内存后才置 loaded，主界面首屏即可秒出全部图标；
            // 进度回调在 IO 线程，切回主线程更新状态驱动 LoadingScreen 的真实 0-100%。
            withContext(Dispatchers.IO) {
                data.sprite.preloadAllSprites { done, total ->
                    withContext(Dispatchers.Main) {
                        preloadDone = done
                        preloadTotal = total
                    }
                }
            }
            cachedLoadedData = data
        }
        loadedState.value = cachedLoadedData
    }
    val loaded = loadedState.value
    if (loaded == null) {
        LoadingScreen(slicer = slicer, preloadDone = preloadDone, preloadTotal = preloadTotal)
    } else {
        // 这里不再持有返回栈 / 导航器：全应用**唯一**的返回栈由 MainScreen 持有，
        // 它不受「数据加载完成」以外的任何重组影响，且它的宿主组件永远不被销毁
        // （横竖屏切换、分栏↔单栏切换都只是改尺寸，不重建 composition）。
        CompositionLocalProvider(
            LocalDataRepository provides loaded.data,
            LocalSpriteRepository provides loaded.sprite,
        ) {
            MainScreen()
        }
    }
}

@Composable
private fun LoadingScreen(
    slicer: SpriteSlicer,
    preloadDone: Int,
    preloadTotal: Int,
) {
    // logo：加载页尚未 provide LocalSpriteRepository，故直接用 AssetLoader + slicer
    // 从 assets 根解码应用图标（ic_launcher.png，与 AboutScreen 同源），失败时回退占位方块。
    val logo by produceState<ImageBitmap?>(initialValue = null, slicer) {
        value = withContext(Dispatchers.IO) {
            try {
                slicer.decode(AssetLoader.loadBytes("ic_launcher.png"))
            } catch (_: Exception) {
                null
            }
        }
    }
    // 进度：准备数据阶段（done < 0）恒为 0；切片阶段 done/total 归一到 0..1。
    val fraction =
        if (preloadDone < 0 || preloadTotal <= 0) 0f
        else (preloadDone.toFloat() / preloadTotal).coerceIn(0f, 1f)
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
            // 上方图标：真实应用图标（assets/ic_launcher.png）圆角显示；解码失败回退占位方块
            // （logo 为委托属性无法智能转换，先落到局部非委托变量再判空）。
            val logoBitmap = logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MiuixTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                if (logoBitmap != null) {
                    Image(
                        bitmap = logoBitmap,
                        contentDescription = "应用图标",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = "牛",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onPrimary,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            // 进度条（确定模式）+ 百分比文字：done < 0 时进度 0
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
            ) {
                LinearProgressIndicator(
                    progress = fraction,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "${(fraction * 100).toInt()}%",
                    color = MiuixTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.height(12.dp))
            // 加载阶段文案：数据加载（切片前）/ 素材切片（0-100%）
            Text(
                text = if (preloadDone < 0) "准备数据…" else "正在加载素材…",
                color = MiuixTheme.colorScheme.onBackground,
            )
        }
    }
}
