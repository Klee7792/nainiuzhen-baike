// 着色器 / GPU 管线静默预热（iOS 冷启动「越用越流畅」治理）。
//
// 病根：miuix 的模糊 / 玻璃走 RuntimeShader(SkSL)，SkSL 自身有缓存，但每个
// (shader 程序 × 混合模式 × 目标格式) 的 **GPU 管线**是首次真正绘制时才编译的（数十~数百 ms），
// 缓存只在进程内 ⇒ 冷启动后第一次切 tab / 开弹窗 / 拖液态玻璃底栏 / 进子页都卡，
// 重复几次才顺滑，杀进程重来又得重新开荒。
//
// 治法：**只预热管线、不预渲染画面**。在加载页（用户本来就在看进度条）用一个 128dp 小盒，
// 渲染真实的模糊 / 玻璃组件；该盒放在 LoadingScreen **之前**绘制 —— Skia 无遮挡剔除，
// 会真实走完整条绘制路径（记录离屏层 → 下采样 → SkSL → 上采样合成），把管线提前编译好；
// 随后立即被加载页的不透明背景盖住，用户完全看不到。
//
// 节奏：**每帧只组一项**（避免一帧内挤几十上百 ms 的编译把加载页卡住），逐项与总计耗时写日志
//（这同时是「预热确实发生、且确实耗时」的自证实验）。
//
// 复用：底栏两处 textureBlur 的参数收敛在 AppBlurPresets.kt，本文件与 MainScreen 共用同一实现，
// 保证预热到的正是真机首帧要编译的那条管线（半径不同 = 不同 shader 程序）。
// 静止态预热补强（本次新增）：A1 关于页动态背景（OS3/OS2 两套片段着色器 + 标题 r150 / 卡片 r60
// 扩展混合 MiBlendModesExt）；A2 按压态底栏两条只在「按住拖动」时才存在的管线（色散 lens +
// 光斑 shader）—— 二者均直接复用真实组件的实现（lens / drawPressSpot），不复制 shader 字符串。
package com.nainiuzhen.wiki.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode as ComposeBlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.nainiuzhen.wiki.ui.components.liquid.IosLiquidGlassNavigationBar
import com.nainiuzhen.wiki.ui.components.liquid.drawPressSpot
import com.nainiuzhen.wiki.ui.components.liquid.lens
import com.nainiuzhen.wiki.ui.home.IOS_LIQUID_GLASS_ENABLED
import com.nainiuzhen.wiki.ui.home.appBottomBarBlur
import com.nainiuzhen.wiki.ui.home.appFloatingBarBlur
import com.nainiuzhen.wiki.ui.settings.about.BgEffectBackground
import com.nainiuzhen.wiki.ui.settings.about.ColorBlendToken
import com.nainiuzhen.wiki.utils.AppLog
import com.nainiuzhen.wiki.utils.LocalAppSettings
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 预热盒边长（dp）：只需让被遮住的离屏绘制真实发生，尺寸不参与观感，可随意调整。 */
private const val WARMUP_BOX_DP = 128

/** 预热总预算（ms）：超过即停止，避免拖慢加载页（加载页本身还要跑数据 / 切片）。 */
private const val WARMUP_BUDGET_MS = 3000L

/** 日志前缀：便于在「关于 → 诊断日志」/ logcat 里检索预热自证。 */
private const val WARMUP_TAG = "[ShaderWarmup]"

/**
 * 单项预热的描述。
 *
 * @param name 日志里显示的人类可读名称。
 * @param content 被真实绘制一次的那一小段 UI（真正的模糊 / 玻璃组件或预设 modifier）。
 */
private class WarmupItem(
    val name: String,
    val content: @Composable () -> Unit,
)

/**
 * 与真实页面同源的采样背景层集合，供各项预热复用。
 *
 * 直接在 [ShaderWarmupHost] 顶层 remember（而非塞进每次重组都会被重建的 item 里），
 * 保证被遮挡的模糊层始终能采到「已登记内容」的背景，从而真正跑完纹理模糊管线。
 *
 * @param enabled 与 MainScreen / AppTopAppBar 同源的顶栏 / 底栏采样层（受「启用模糊」开关约束）。
 * @param dialog 与弹窗侧栏 / FadeEdges 同源的采样层（只看运行环境，不绑定开关）。
 */
private class WarmupBackdrops(
    val enabled: LayerBackdrop?,
    val dialog: LayerBackdrop?,
)

@Composable
private fun rememberWarmupBackdrops(): WarmupBackdrops {
    val enabled = rememberAppBlurBackdrop()
    val dialog = rememberDialogSideBlurBackdrop()
    return WarmupBackdrops(enabled = enabled, dialog = dialog)
}

/**
 * 128dp 预热盒里的采样内容源：登记为一层 backdrop，供其上的模糊层采样。
 * 真实页面里这一角色由内容区（`Modifier.layerBackdrop(backdrop)`）承担。
 */
@Composable
private fun BoxScope.WarmupBackdropSource(backdrop: LayerBackdrop?, surface: Color) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
            .background(surface),
    )
}

/** 顶栏类预热的占位内容：给 BlurredBar 一个非零尺寸，使其模糊层确实有得画。 */
@Composable
private fun WarmupBarBody() {
    Box(modifier = Modifier.fillMaxWidth().height(48.dp))
}

/**
 * 预热件清单（覆盖首次交互卡顿面），**按当前配置门控 + 按用户痛点排序**构建：
 *
 * 候选项（含本次新增）：
 * 1. 液态玻璃底栏（真实组件 IosLiquidGlassNavigationBar）—— drawBackdrop 的
 *    `blur(4dp) + lens 折射 + vibrancy + 高光` 管线；
 * 2. 悬浮底栏玻璃（复用 AppBlurPresets.appFloatingBarBlur，r25 + 高光）；
 * 3. 经典贴边底栏（复用 AppBlurPresets.appBottomBarBlur，r18）；
 * 4. 顶栏均匀模糊（真实组件 BlurredBar，强制均匀 r25）—— 主页 / 子页顶栏；
 * 5. 顶栏渐进模糊（真实组件 BlurredBar，随 topAppBarBlurStyle）—— 设置=渐进时为 r10 渐进栈；
 * 6. 弹窗侧栏玻璃（textureBlur r25，弹窗 / FadeEdges 同源背景层）—— 只受 RuntimeShader 能力约束；
 * 7. **A2** 按压态色散 lens（LiquidGlassLensDispersion）—— 只在「按住拖动底栏」时出现，静止态漏掉；
 * 8. **A2** 按压光斑 shader（InteractiveHighlight.SPOT_SHADER）—— 同上，复用 drawPressSpot 同一实现；
 * 9. **A1** 关于页动态背景 OS3（OS3_BG_FRAG + 标题 r150 + 卡片 r60；std/ext 由当前模式的 blend token 决定）；
 * 10. **A1** 关于页动态背景 OS2（OS2_BG_FRAG + 同上 r150/r60）—— 两套片段着色器各编译一次。
 *
 * 门控：底栏项按「底栏是否显示 + 悬浮 / 玻璃风格」入队；顶栏项按「顶栏是否显示 + 当前模糊风格」
 * 决定先后；弹窗项无条件入队；A2 仅在 iOS 液态玻璃底栏（glassEnabled）下有意义，随 ① 同入队；
 * A1 与底栏风格无关，无条件入队（blur 关时仍编译 OS3/OS2 背景 shader 程序）。
 * `bd.enabled`/`bd.dialog` 只决定运行期是否自然降级，不参与「谁入队」的判断。
 *
 * 排序：3000ms 预算触发 break 是从**队尾**砍，故当前配置 + 用户痛点最高者排最前。
 * 第 2/3 项故意走 AppBlurPresets 与 MainScreen **同一实现**：参数漂移就会预热到另一条管线。
 * 新增 A1 / A2 的排序理由：A2 紧跟当前生效底栏（同痛点、且为按压态补充）；A1 放顶栏项之后、
 * 备选底栏风格之前（关于页首进黑屏是独立痛点，与底栏风格无关）。
 */
@Composable
private fun rememberWarmupItems(bd: WarmupBackdrops): List<WarmupItem> {
    val appState = LocalAppSettings.current
    // 与 MainScreen 判定 isBarDark 的规则保持一致，确保高光（Light/Dark）与真实底栏同款。
    val isBarDark = when (appState.colorMode) {
        0 -> isSystemInDarkTheme()
        1 -> true
        else -> false
    }
    val floatingHighlight = remember(isBarDark) {
        if (isBarDark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight
    }
    val navItems = remember {
        listOf(
            NavigationItem(label = "主页", icon = MiuixIcons.Home),
            NavigationItem(label = "设置", icon = MiuixIcons.Settings),
        )
    }
    val floatingShape = remember { RoundedCornerShape(FloatingToolbarDefaults.CornerRadius) }
    val floatingColors = BlurDefaults.blurColors(
        blendColors = listOf(
            BlendColorEntry(color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)),
        ),
    )
    val dialogColors = BlurDefaults.blurColors(
        blendColors = listOf(
            BlendColorEntry(color = MiuixTheme.colorScheme.background.copy(alpha = 0.3f)),
        ),
    )
    val surface = MiuixTheme.colorScheme.surface

    // —— 门控语义 ——
    // · `bd.enabled` / `bd.dialog` 只反映「启用模糊 / 运行环境能力」；
    // · 底栏玻璃项「是否入队」还要看**底栏风格设置**，两件事不要混为一谈。
    val navVisible = appState.showNavigationBar
    val topVisible = appState.showTopAppBar
    val floating = appState.useFloatingNavigationBar
    val glassEnabled = floating && appState.floatingNavigationBarStyle == 1 && IOS_LIQUID_GLASS_ENABLED
    val progressiveTop = appState.topAppBarBlurStyle == 1

    // —— 预热件定义（内容原样保留，只改「谁入队 + 排在哪儿」）——
    val glassItem = WarmupItem("液态玻璃底栏 IosLiquidGlassNavigationBar") {
        Box(Modifier.fillMaxSize()) {
            WarmupBackdropSource(backdrop = bd.enabled, surface = surface)
            IosLiquidGlassNavigationBar(
                items = navItems,
                selectedIndex = 0,
                onItemClick = {},
                backdrop = bd.enabled,
                isBlurActive = bd.enabled != null,
            )
        }
    }
    val floatingItem = WarmupItem("悬浮底栏玻璃 appFloatingBarBlur(r25+高光)") {
        Box(Modifier.fillMaxSize()) {
            WarmupBackdropSource(backdrop = bd.enabled, surface = surface)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .appFloatingBarBlur(
                        backdrop = bd.enabled,
                        shape = floatingShape,
                        highlight = floatingHighlight,
                        colors = floatingColors,
                    ),
            )
        }
    }
    val plainItem = WarmupItem("经典贴边底栏 appBottomBarBlur(r18)") {
        Box(Modifier.fillMaxSize()) {
            WarmupBackdropSource(backdrop = bd.enabled, surface = surface)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .appBottomBarBlur(backdrop = bd.enabled),
            )
        }
    }
    val topUniformItem = WarmupItem("顶栏均匀模糊 BlurredBar(r25)") {
        Box(Modifier.fillMaxSize()) {
            WarmupBackdropSource(backdrop = bd.enabled, surface = surface)
            BlurredBar(backdrop = bd.enabled, forceUniformBlur = true) { WarmupBarBody() }
        }
    }
    val topProgressiveItem = WarmupItem("顶栏渐进模糊 BlurredBar(随设置)") {
        Box(Modifier.fillMaxSize()) {
            WarmupBackdropSource(backdrop = bd.enabled, surface = surface)
            BlurredBar(backdrop = bd.enabled) { WarmupBarBody() }
        }
    }
    // FadeEdges 的两层（顶/底渐隐）用的是完全相同的 radius / shape / colors，烘一次即可覆盖。
    val dialogItem = WarmupItem("弹窗侧栏玻璃 textureBlur(r25,background@0.3)") {
        Box(Modifier.fillMaxSize()) {
            WarmupBackdropSource(backdrop = bd.dialog, surface = surface)
            val dialogBackdrop = bd.dialog
            if (dialogBackdrop != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .textureBlur(
                            backdrop = dialogBackdrop,
                            shape = RectangleShape,
                            blurRadius = 25f,
                            colors = dialogColors,
                        ),
                )
            }
        }
    }

    // —— A1：关于页动态背景（OS3 / OS2 两套片段着色器 + r150 / r60 扩展混合模糊）——
    // 进关于页首帧会黑屏 ~2.4s：BgEffectPainter 的 OS3_BG_FRAG / OS2_BG_FRAG 两程序，以及
    // 标题 r150、卡片 r60 的 textureBlur 都**只在关于页**才出现（与已覆盖的 r4/10/18/25 是不同程序）；
    // 标题 r150 两组 token 都含 LinearLight(100) + Lab(106) ⇒ 恒走 MiBlendModesExt；
    // 卡片 r60 的 std/ext 由当前模式决定：深色 Overlay_Thin_Light 含 PlusDarker(120) ⇒ Ext，
    // 浅色 Pured_Regular_Light 仅 Overlay(15)/HardLight(20) ⇒ 只走 MiBlendModesStd。两处 isOs3Effect 各组合一次。
    val aboutBgWarmupContent: @Composable (Boolean) -> Unit = { isOs3Effect ->
        val appState = LocalAppSettings.current
        // 与 AboutScreen 判定 isDark 的规则一致，确保 logo / card 那两处 blendColors 命中同款扩展模式。
        val isDark = when (appState.colorMode) {
            1 -> true
            2 -> false
            else -> isSystemInDarkTheme()
        }
        // 原样抄 AboutScreen 的 logoBlend / cardBlend（含 blendModes）—— 不重写 shader key。
        // 注意 token 名与模式相反：深色取 TitleLight（见 AboutScreen L230 注释）。
        val logoBlend = if (isDark) ColorBlendToken.TitleLight else ColorBlendToken.TitleDark
        val cardBlend = if (isDark) ColorBlendToken.Overlay_Thin_Light else ColorBlendToken.Pured_Regular_Light
        val backdrop = bd.enabled
        Box(Modifier.fillMaxSize()) {
            BgEffectBackground(
                dynamicBackground = false, // 不跑颜色阶段动画，只编译背景 shader 管线
                modifier = Modifier.fillMaxSize(),
                bgModifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier,
                isOs3Effect = isOs3Effect,
                content = {
                    // 标题 r150（与 AboutScreen :334 同实参，含 DstIn 合成）
                    if (backdrop != null) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .textureBlur(
                                    backdrop = backdrop,
                                    shape = RoundedCornerShape(16.dp),
                                    blurRadius = 150f,
                                    noiseCoefficient = BlurDefaults.NoiseCoefficient,
                                    colors = BlurDefaults.blurColors(blendColors = logoBlend),
                                    contentBlendMode = ComposeBlendMode.DstIn,
                                ),
                        )
                    }
                    // 卡片 r60（与 AboutScreen :403 / :450 同实参）
                    if (backdrop != null) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .textureBlur(
                                    backdrop = backdrop,
                                    shape = RoundedCornerShape(16.dp),
                                    blurRadius = 60f,
                                    noiseCoefficient = BlurDefaults.NoiseCoefficient,
                                    colors = BlurDefaults.blurColors(blendColors = cardBlend),
                                ),
                        )
                    }
                },
            )
        }
    }
    // std/ext 由当前模式的 blend token 决定；浅色模式 r60 走 Std、深色走 Ext，运行时切模式会走另一支。
    val aboutOs3Item = WarmupItem("关于页动态背景 OS3(OS3_BG_FRAG+r150/r60+混合模式)") {
        aboutBgWarmupContent(true)
    }
    val aboutOs2Item = WarmupItem("关于页动态背景 OS2(OS2_BG_FRAG+r150/r60+混合模式)") {
        aboutBgWarmupContent(false)
    }

    // —— A2：按压态底栏两条静止态碰不到的管线 ——
    // 静止态预热烘不到「第一次按住拖动底栏水滴」的卡顿来源：色散折射 lens（LiquidGlassLensDispersion）
    // 与按压光斑 shader（InteractiveHighlight.SPOT_SHADER）。两项以「按压进行中」的实参各跑一遍，
    // 复用真实组件同一实现（lens / drawPressSpot），不复制 shader 字符串。
    val pressLensItem = WarmupItem("按压态色散 lens LiquidGlassLensDispersion") {
        val backdrop = bd.enabled
        Box(Modifier.fillMaxSize()) {
            WarmupBackdropSource(backdrop = backdrop, surface = surface)
            if (backdrop != null) {
                // 参数抄 IosLiquidGlassNavigationBar pill 按压态 progress=1f 的实参。
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { CircleShape },
                            effects = {
                                lens(
                                    refractionHeight = 10.dp.toPx(),
                                    refractionAmount = 14.dp.toPx(),
                                    depthEffect = true,
                                    chromaticAberration = 0.5f,
                                )
                            },
                        ),
                )
            }
        }
    }
    val pressSpotItem = WarmupItem("按压光斑 SPOT_SHADER InteractiveHighlight") {
        // 固定 progress=1f 跑一遍光斑 shader（与 InteractiveHighlight.modifier 同一 drawPressSpot 实现）。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawPressSpot(
                        progress = 1f,
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension * 1.2f,
                    )
                },
        )
    }

    // —— 入队顺序 ——
    // 预算一旦触发 break 是从**队尾**砍，所以「当前配置 + 用户痛点最高」必须排在前面，
    // 否则用户第 2 号痛点（色彩模式那类弹窗）可能永远排不到。
    return buildList {
        // ① 当前生效的底栏（用户痛点最高）。
        if (navVisible) {
            when {
                glassEnabled -> {
                    add(glassItem)
                    // A2：按压态底栏两条静止态碰不到的管线（色散 lens + 光斑），紧跟底栏之后；
                    //     只在 iOS 液态玻璃底栏生效时才有意义（其余风格无此 press 态管线）。
                    add(pressLensItem)
                    add(pressSpotItem)
                }
                floating -> add(floatingItem)
                else -> add(plainItem)
            }
        }
        // ② 弹窗侧栏：只受 RuntimeShader 能力约束，与「启用模糊」开关无关 ⇒ 无条件入队；
        //    紧跟同半径项几乎零成本，但必须排在预算砍尾之前。
        add(dialogItem)
        // ③ 当前顶栏风格在前，另一种紧随其后（用户切换风格后也能立刻顺）。
        if (topVisible) {
            if (progressiveTop) {
                add(topProgressiveItem)
                add(topUniformItem)
            } else {
                add(topUniformItem)
                add(topProgressiveItem)
            }
        }
        // A1：关于页动态背景（OS3 / OS2 两套 + r150 / r60 扩展混合）—— 只在顶栏项之后、备选底栏风格之前：
        // 关于页首进黑屏是其独立痛点，与底栏风格无关，无条件入队（blur 关时仍编译 OS3/OS2 背景 shader）。
        add(aboutOs3Item)
        add(aboutOs2Item)
        // ④ 备选底栏风格：预埋在后，用户切换底栏风格后也能立刻顺。
        if (navVisible && glassEnabled) add(floatingItem)
        if (navVisible && floating && !glassEnabled) add(glassItem)
        if (navVisible && !floating) {
            add(floatingItem)
            add(glassItem)
        }
    }
}

/**
 * 管线预热宿主：**只预热管线、不预渲染画面**。
 *
 * 由 App.kt 在加载页分支里、**先于 LoadingScreen** 组合，小盒随后被加载页不透明背景盖住。
 * 每帧只组一项（用 [withFrameNanos] 逐帧推进），总耗时受 [WARMUP_BUDGET_MS] 约束。
 */
@Composable
fun ShaderWarmupHost(modifier: Modifier = Modifier) {
    if (!isRuntimeShaderSupported()) {
        AppLog.i("$WARMUP_TAG 运行环境不支持 RuntimeShader（Android <12 / 无 SkSL），跳过 GPU 管线预热")
        return
    }
    val backdrops = rememberWarmupBackdrops()
    val items = rememberWarmupItems(backdrops)

    // -1 = 不渲染任何项（预热结束 / 尚未开始）。
    var activeIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        val n = items.size
        if (n == 0) {
            AppLog.w("$WARMUP_TAG 当前配置下无预热项，跳过")
            return@LaunchedEffect
        }
        // 取一帧时间戳做基准。
        val startNs = withFrameNanos { it }
        var prevNs = startNs
        var done = 0
        for (i in 0 until n) {
            // 切到第 i 项。`activeIndex = i` 只保证「此后取的重组快照能看到它」，
            // 而「续体」与「Recomposer 重组回调」在同一帧内的先后随平台 / 版本而变；
            // 若续体总在快照之前跑，第 0 项就永远进不了快照 ⇒ 必须多等一整帧。
            activeIndex = i
            val commitNs = withFrameNanos { it }
            val drawNs = withFrameNanos { it }
            // 第 i 项的首绘开销必然落在「commitNs 之前那一帧」或「commitNs 那一帧」内
            // （取决于上述帧内时序），取较大者 —— 无编译开销时读数也只是 1 帧基准，而不是 2 帧。
            val itemMs = maxOf(
                (commitNs - prevNs) / 1_000_000L,
                (drawNs - commitNs) / 1_000_000L,
            )
            val totalMs = (drawNs - startNs) / 1_000_000L
            AppLog.i("$WARMUP_TAG [${i + 1}/$n] ${items[i].name} 首绘≈${itemMs}ms 累计=${totalMs}ms")
            prevNs = drawNs
            done = i + 1
            if (totalMs >= WARMUP_BUDGET_MS) {
                AppLog.i("$WARMUP_TAG 已达预算 ${WARMUP_BUDGET_MS}ms，提前结束（剩 ${n - done} 项未烘）")
                break
            }
        }
        activeIndex = -1
        val totalMs = (prevNs - startNs) / 1_000_000L
        AppLog.i(
            "$WARMUP_TAG GPU 管线预热结束：已烘 $done/$n 项，总耗时 ${totalMs}ms（预算 ${WARMUP_BUDGET_MS}ms）",
        )
    }

    if (activeIndex in items.indices) {
        val idx = activeIndex
        // 自证：组合成功即代表本帧会走到绘制（Skia 无遮挡剔除），
        // 这行日志就是「第 idx 项确实被绘制过」的凭据，用于排查预热漏项。
        SideEffect { AppLog.i("$WARMUP_TAG 已绘制第 ${idx + 1} 项 / 共 ${items.size} 项") }
        Box(modifier = modifier.size(WARMUP_BOX_DP.dp)) {
            items[idx].content()
        }
    }
}
