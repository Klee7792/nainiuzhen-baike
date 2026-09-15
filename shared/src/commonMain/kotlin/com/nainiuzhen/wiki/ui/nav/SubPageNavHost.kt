package com.nainiuzhen.wiki.ui.nav

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nainiuzhen.wiki.ui.adaptive.rememberUseDualPane
import com.nainiuzhen.wiki.ui.items.ItemListScreen
import com.nainiuzhen.wiki.ui.npc.NpcListScreen
import com.nainiuzhen.wiki.ui.npc.NpcScheduleScreen
import com.nainiuzhen.wiki.ui.recipe.RecipeListScreen
import com.nainiuzhen.wiki.ui.settings.AboutScreen
import com.nainiuzhen.wiki.ui.settings.CardSettingsScreen
import com.nainiuzhen.wiki.ui.settings.DiagnosticsScreen
import com.nainiuzhen.wiki.ui.settings.ImageScaleSettingsScreen
import com.nainiuzhen.wiki.ui.settings.about.LicenseScreen
import com.nainiuzhen.wiki.utils.LocalAppSettings
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 「详情层」的路由宿主 —— 全应用**唯一**的子页返回栈就挂在这里。
 *
 * ### 与 v31 之前的结构差异（横竖屏状态保持）
 *
 * 改造前工程里有两个互不相干的 `NavDisplay`：
 * 1. 窗口级（`App.kt` / 旧的 `AppNavHost`）—— 单栏时全屏渲染子页；
 * 2. 右栏级（`MainScreen.DetailPaneHost`，用 `when` 手写）—— 分栏时渲染右栏子页。
 *
 * 两者由 `useDualPane` 决定谁生效，但**栈内容从不互相迁移**，于是旋转一下就会出现
 * 「子页莫名回到主页 / 分栏时被全屏子页盖住 / 返回键弹错栈导致横竖叠加」三类幽灵状态。
 *
 * 现在只剩这一个宿主，[backStack] 就是唯一返回栈，**永远**由本组件渲染：
 * - 单栏：它满宽（叠在列表层之上）⇒ 子页全屏，过渡动画、圆角裁剪、遮罩、侧滑返回全部照旧；
 * - 双栏：它被 `ListDetailPanes` 放到右栏 ⇒ 子页落在右栏。
 *
 * 旋转只是让它换了个尺寸/位置，**组件树位置不变** ⇒ composition 不重建 ⇒
 * 子页的滚动位置、搜索词、筛选条件、打开着的弹窗全部原地保留。
 *
 * 路由表：将 [Route] 各目的地映射到对应页面，统一使用 miuix-nav 过渡与效果。
 * 过渡样式（Miuix / AOSP）、圆角裁剪、遮罩变暗、转场拦截输入均跟随设置页偏好（变更点 #23/#26）。
 * AOSP 样式为本工程移植的 [CrossActivityTransition]（源自 miuix example），装配口径对齐 demo
 * AppContent.kt（v1.0.9/1206）：
 * - 圆角模式：AOSP → [NavCornerClipMode.All]（卡片缩放整页，四角全圆），否则 [NavCornerClipMode.Leading]（滑动式只圆前缘）；
 * - 圆角半径：跟随系统屏幕圆角（`rememberNavSystemCornerRadius`），双栏布局用 0.dp（右栏不在屏幕边），
 *   AOSP 且系统报告无圆角时兜底 32.dp 保证卡片在桌面端也是圆的；
 * - 压暗量：关压暗 → 0；AOSP → 深色 0.8 / 浅色 0.2（平台 scrim 常量）；Miuix → 0.5；
 * - 背景：卡片缩放露出的区域以 surface 色填充（等价平台 back-animation 背景层）。
 * 滑动返回方向改为**逐 entry 显式传递**（miuix-nav 的库内预设 dismissDirection 均为 None，
 * 继承式写法恒无效，git#2）：关 → None；RTL → RightToLeft；否则 LeftToRight。
 *
 * @param backStack 唯一返回栈；栈底恒为 [Route.Main]（= 没有打开任何子页）。
 * @param navigator 与该栈配对的导航器（各业务页里的 `pop()` 会弹它）。
 * @param modifier 外层修饰；容器（列表层/详情层）分配尺寸，通常传 `Modifier.fillMaxSize()`。
 * @param homePlaceholder [Route.Main]（栈底）时的占位内容。
 *   分栏时传右栏空态提示（「暂无内容」）；单栏时留空 —— 因为此时详情层满宽叠在列表层上，
 *   若渲染空态提示会把下面的主页整个盖住。
 */
@Composable
fun SubPageNavHost(
    backStack: NavBackStack,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    homePlaceholder: @Composable () -> Unit = {},
) {
    val appState = LocalAppSettings.current
    val isAosp = appState.navTransitionStyle == 1
    // 过渡动画：navTransitionStyle == 1 → AOSP（CrossActivity，移植自 miuix example），否则 MiuixDefault。
    val transition: NavTransition = if (isAosp) CrossActivityTransition else NavTransitions.MiuixDefault
    // 深色判定：与 App.kt 口径一致（1=深色 2=浅色 0=跟随系统），仅用于 AOSP 压暗量分档。
    val isDark = when (appState.colorMode) {
        1 -> true
        2 -> false
        else -> isSystemInDarkTheme()
    }
    // 圆角半径：双栏时右栏不在屏幕边缘，跟随屏幕圆角无意义 ⇒ 0.dp（对齐 demo :196）。
    val navCornerRadius = if (rememberUseDualPane()) 0.dp else rememberNavSystemCornerRadius()
    // 取值必须提升到 remember 计算块之外：MiuixTheme.colorScheme 是 @Composable 读取，
    // remember 的 calculation lambda 不是 composable 上下文（编译错误 :110）。
    val surfaceColor = MiuixTheme.colorScheme.surface
    // 正交转场效果：圆角裁剪 / 遮罩变暗 / 转场拦截输入跟随开关（装配口径对齐 demo :200-231）。
    val effects = remember(
        isAosp,
        appState.enableCornerClip,
        appState.enableDim,
        appState.blockInputDuringTransition,
        navCornerRadius,
        isDark,
        surfaceColor,
    ) {
        NavDisplayEffects(
            enableCornerClip = appState.enableCornerClip,
            // AOSP 卡片在系统报告无屏幕圆角的平台（桌面/预览）也要保持圆角，兜底 32.dp。
            cornerClipRadius = if (isAosp && navCornerRadius <= 0.dp) 32.dp else navCornerRadius,
            // AOSP 卡片缩放整页 ⇒ 四角全圆；滑动式只圆进入侧前缘。
            cornerClipMode = if (isAosp) NavCornerClipMode.All else NavCornerClipMode.Leading,
            // 压暗量分风格与深浅色：AOSP 用平台 scrim 常量（浅 0.2 / 深 0.8），Miuix 维持 0.5。
            dimAmount = when {
                !appState.enableDim -> 0f
                isAosp -> if (isDark) 0.8f else 0.2f
                else -> 0.5f
            },
            blockInputDuringTransition = appState.blockInputDuringTransition,
            // 卡片缩小露出的区域以页面背景色填充，等价平台的 back-animation 背景层。
            backdropColor = surfaceColor,
        )
    }
    // 滑动返回：miuix-nav 预设的 dismissDirection 全为 None，继承式写法恒无效；
    // 改为逐 entry 显式传方向（对齐 demo :236-240）：关 → None；RTL → 右滑；LTR → 左滑。
    val swipeBackDirection = when {
        !appState.enableSwipeBack -> NavSwipeDirection.None
        LocalLayoutDirection.current == LayoutDirection.Rtl -> NavSwipeDirection.RightToLeft
        else -> NavSwipeDirection.LeftToRight
    }
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { navigator.pop() },
        transition = transition,
        effects = effects,
    ) {
        // 栈底：不是「一个页面」，而是「详情层当前没有内容」这个状态。
        // NavEntryHost 的根节点不带任何底色，所以这里渲染空 ⇒ 下方的列表层直接透出来。
        entry<Route.Main> { homePlaceholder() }
        entry<Route.ItemList>(swipeDismiss = swipeBackDirection) {
            ItemListScreen()
        }
        entry<Route.RecipeList>(swipeDismiss = swipeBackDirection) {
            RecipeListScreen()
        }
        entry<Route.NpcList>(swipeDismiss = swipeBackDirection) {
            NpcListScreen()
        }
        entry<Route.NpcSchedule>(swipeDismiss = swipeBackDirection) { route ->
            NpcScheduleScreen(npcId = route.npcId)
        }
        entry<Route.About>(swipeDismiss = swipeBackDirection) {
            AboutScreen()
        }
        entry<Route.License>(swipeDismiss = swipeBackDirection) {
            LicenseScreen()
        }
        entry<Route.ImageScaleSettings>(swipeDismiss = swipeBackDirection) {
            ImageScaleSettingsScreen()
        }
        entry<Route.CardSettings>(swipeDismiss = swipeBackDirection) {
            CardSettingsScreen()
        }
        entry<Route.Diagnostics>(swipeDismiss = swipeBackDirection) {
            DiagnosticsScreen()
        }
    }
}
