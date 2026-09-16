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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import com.nainiuzhen.wiki.data.AssetManager
import com.nainiuzhen.wiki.data.source.AssetLoader
import com.nainiuzhen.wiki.utils.IoDispatcher
import com.nainiuzhen.wiki.utils.SetStatusBarLightIcons
import com.nainiuzhen.wiki.utils.AppLog
import com.nainiuzhen.wiki.utils.ApplyPhoneOrientation
import com.nainiuzhen.wiki.data.repository.SpriteCacheManager
import com.nainiuzhen.wiki.data.source.SpriteSlicer
import com.nainiuzhen.wiki.ui.home.MainScreen
import com.nainiuzhen.wiki.ui.nav.LocalDataRepository
import com.nainiuzhen.wiki.ui.nav.LocalSpriteRepository
import com.nainiuzhen.wiki.ui.components.AppToastHost
import com.nainiuzhen.wiki.ui.theme.AppTheme
import com.nainiuzhen.wiki.utils.AppState
import com.nainiuzhen.wiki.utils.AppSettingsStore
import com.nainiuzhen.wiki.utils.LocalAppSettings
import com.nainiuzhen.wiki.utils.LocalUpdateAppSettings
import com.nainiuzhen.wiki.utils.AppVersion
import com.nainiuzhen.wiki.utils.LocalAppVersion
import com.nainiuzhen.wiki.utils.appStartElapsedMs
import com.nainiuzhen.wiki.utils.formatDecimal
import com.nainiuzhen.wiki.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
            // 排障探针：窗口实际尺寸与屏幕密度（iOS 白屏排查用，稳定后移除）。
            // App() 只在设置变化时重组，日志量可忽略。
            AppLog.i(
                "window containerDp=${LocalWindowInfo.current.containerDpSize} " +
                    "density=${LocalDensity.current.density}",
            )
            // 按「手机横屏」设置应用方向：启动时一次 + 开关切换时立即生效（大屏适配 §8.3）。
            ApplyPhoneOrientation(allowLandscape = appState.allowPhoneLandscape)
            AppRoot(slicer = slicer, cache = cache, isDebug = isDebug)
            // 主题感知的 Toast 宿主：置于根部、覆盖在页面之上，跟随深浅色 / Monet（#v36）。
            AppToastHost()
        }
    }
}

/**
 * 启动加载页里「准备数据」阶段（读 plist / JSON）在总进度中占的权重，其余归素材切片。
 * 两个阶段的真实耗时比例随机型波动，这里取经验值：切片帧数多（6565 帧）通常更久，故给足权重。
 */
private const val DATA_PHASE_WEIGHT = 0.3f

@Composable
private fun AppRoot(
    slicer: SpriteSlicer,
    cache: SpriteCacheManager,
    isDebug: Boolean,
) {
    // 进程存活且已加载过时，初始值直接复用 [cachedLoadedData]，避免切后台回前台
    // （Activity 重建导致 remember 重置）时闪一下 LoadingScreen，被误认为「重新加载」。
    val loadedState = remember { mutableStateOf(cachedLoadedData) }
    // 加载进度：分两个阶段拼成**一条连续**的总进度（避免「准备数据」阶段长时间卡在 0%：
    // 该阶段此前完全没有进度回流，实测占冷启动相当比例）。
    //   · phase 0 = 准备数据（读 plist / JSON，占前 [DATA_PHASE_WEIGHT]）；
    //   · phase 1 = 素材切片（主体，占剩余权重）。
    // done/total 是**当前阶段内部**的步数，LoadingScreen 负责折算成总进度。
    var loadPhase by remember { mutableIntStateOf(0) }
    var loadDone by remember { mutableIntStateOf(0) }
    var loadTotal by remember { mutableIntStateOf(0) }
    // 冷启动计时：只有本次真的跑完加载流程才有值（>0），热启动 / 后台回前台为 -1 ⇒ 不弹 toast。
    var startupElapsedMs by remember { mutableLongStateOf(-1L) }
    // 启动加载失败原因：非空时直接上屏展示（iOS 首版排障：宁可看到报错，也不要黑屏无因可查）。
    var loadError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        // 进程存活时直接复用已加载数据（变更点 #27，避免后台回前台重新加载）。
        if (cachedLoadedData == null) {
            // 去磁盘化：切片全量驻内存、不再写盘；启动时顺手删除旧版本残留的磁盘切片目录
            // （一次性清理，幂等，try-catch 包住避免清理失败阻塞启动）。
            runCatching { cache.clear() }
            // 资源加载（读 121 个文件 + 解析 26 个 plist）较重，放到 IO 线程，
            // 避免阻塞主线程导致首启动 ANR；主线程仅负责展示 LoadingScreen。
            // iOS 首版排障：加载全流程兜底。异常写诊断日志并上屏，避免「加载失败 → 黑屏」无从下手。
            try {
                AppLog.i("开始加载数据：isDebug=$isDebug")
                val data = withContext(IoDispatcher) {
                    AssetManager(slicer, cache).loadAll(isDebug) { done, total ->
                        withContext(Dispatchers.Main) {
                            loadPhase = 0
                            loadDone = done
                            loadTotal = total
                        }
                    }
                }
                // 预热切片：全部帧切片进内存后才置 loaded，主界面首屏即可秒出全部图标；
                // 进度回调在 IO 线程，切回主线程更新状态驱动 LoadingScreen 的真实 0-100%。
                val failedSheets = withContext(IoDispatcher) {
                    data.sprite.preloadAllSprites { done, total ->
                        withContext(Dispatchers.Main) {
                            loadPhase = 1
                            loadDone = done
                            loadTotal = total
                        }
                    }
                }
                cachedLoadedData = data
                // 计时点：数据 + 切片都就绪（= 首屏内容齐备）。
                startupElapsedMs = appStartElapsedMs()
                AppLog.i(
                    "加载完成：耗时 ${startupElapsedMs}ms | 物品 ${data.data.items.size} / " +
                        "配方 ${data.data.recipes.size} / NPC ${data.data.npcs.size} / " +
                        "帧 ${data.sprite.atlasFrameCount}（已切片 ${data.sprite.loadedSpriteCount}）/ " +
                        "失败图集 $failedSheets"
                )
                // 「没报错但也没加载出来」的兜底：数据与图集任一为空都要当成失败处理，
                // 否则会静默进入主界面显示一片空白（iOS 首版黑屏的真凶候选）。
                val emptyReason = when {
                    data.data.items.isEmpty() -> "物品数据为 0 条"
                    data.sprite.atlasFrameCount == 0 -> "图集帧数为 0"
                    data.sprite.loadedSpriteCount == 0 -> "全部图集切片失败（图标全空）"
                    else -> null
                }
                if (emptyReason != null) {
                    AppLog.e("数据加载结果异常：$emptyReason → 视为启动失败")
                    loadError = "数据未加载成功：$emptyReason"
                    // 空数据不进进程级缓存：否则后台回前台会直接复用空数据、跳过重试，
                    // 又变回「黑屏且无报错」。
                    cachedLoadedData = null
                }
            } catch (t: Throwable) {
                AppLog.e("启动加载失败", t)
                loadError = "$t"
            }
        }
        loadedState.value = cachedLoadedData
    }
    val loaded = loadedState.value
    if (loadError != null) {
        StartupErrorScreen(message = loadError!!)
    } else if (loaded == null) {
        LoadingScreen(
            slicer = slicer,
            phase = loadPhase,
            done = loadDone,
            total = loadTotal,
        )
    } else {
        // 进入主页后弹一次冷启动耗时（X.XX 秒）。延迟 300ms 让主页首帧先出来，
        // 避免 toast 抢在界面绘制之前。只在冷启动（真的加载过）时弹。
        LaunchedEffect(Unit) {
            AppLog.i("进入主界面（MainScreen 首帧）")
            val ms = startupElapsedMs
            if (ms > 0) {
                delay(300)
                showToast("启动耗时 ${formatDecimal(ms / 1000.0, 2)} 秒")
            }
        }
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

/**
 * 启动加载失败页：把异常原文直接铺在屏幕上。
 *
 * 仅用于排障（iOS 无 Mac、连不了 Xcode 调试器）：错误信息同时写入诊断日志
 * （Android = logcat；iOS = Documents/app_log.txt）。正常情况下此页不应出现。
 */
@Composable
private fun StartupErrorScreen(message: String) {
    // 日志快照：取此刻的内存缓冲（最近若干行），随错误一起上屏 —— 无 Mac 时
    // 直接截屏即可，无需连电脑导出 Documents/app_log.txt。
    val log = remember { AppLog.readLog() }
    MiuixTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .padding(20.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Text(
                    text = "启动失败（完整日志见下方 / Documents/app_log.txt）",
                    color = MiuixTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = message,
                    color = MiuixTheme.colorScheme.onBackground,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "—— 诊断日志（最近 ${log.lineSequence().count()} 行）——",
                    color = MiuixTheme.colorScheme.onBackground,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = log.ifBlank { "（无）" },
                    color = MiuixTheme.colorScheme.onBackground,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

/**
 * 启动加载页。
 *
 * 进度走**两阶段拼接**的总进度条：准备数据（读 plist / JSON）占前 [DATA_PHASE_WEIGHT]，
 * 素材切片占剩余权重；两阶段各自内部 0→100%，折算后进度条单调不回退。
 * 百分比下方同时给出当前阶段的 `done/total` 原始计数（切片阶段即帧数），
 * 便于直观判断卡在哪一步。
 *
 * @param phase 0 = 准备数据，1 = 素材切片。
 * @param done 当前阶段已完成步数。
 * @param total 当前阶段总步数；≤0 时进度条为 0。
 */
@Composable
private fun LoadingScreen(
    slicer: SpriteSlicer,
    phase: Int,
    done: Int,
    total: Int,
) {
    // logo：加载页尚未 provide LocalSpriteRepository，故直接用 AssetLoader + slicer
    // 从 assets 根解码应用图标（ic_launcher.png，与 AboutScreen 同源），失败时回退占位方块。
    val logo by produceState<ImageBitmap?>(initialValue = null, slicer) {
        value = withContext(IoDispatcher) {
            try {
                slicer.decode(AssetLoader.loadBytes("ic_launcher.png"))
            } catch (_: Exception) {
                null
            }
        }
    }
    // 总进度 = 已完成阶段的权重 + 当前阶段内部进度 × 本阶段权重（单调不回退）。
    val phaseFraction = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
    val fraction = if (phase == 0) {
        DATA_PHASE_WEIGHT * phaseFraction
    } else {
        DATA_PHASE_WEIGHT + (1f - DATA_PHASE_WEIGHT) * phaseFraction
    }
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
            // 加载阶段文案：数据加载 / 素材切片，均带当前阶段原始计数（切片阶段即帧数）。
            Text(
                text = if (phase == 0) {
                    "准备数据… $done/$total"
                } else {
                    "正在加载素材… $done/$total"
                },
                color = MiuixTheme.colorScheme.onBackground,
            )
        }
    }
}
