package com.nainiuzhen.wiki.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nainiuzhen.wiki.ui.items.ItemListScreen
import com.nainiuzhen.wiki.ui.npc.NpcListScreen
import com.nainiuzhen.wiki.ui.npc.NpcScheduleScreen
import com.nainiuzhen.wiki.ui.recipe.RecipeListScreen
import com.nainiuzhen.wiki.ui.settings.AboutScreen
import com.nainiuzhen.wiki.ui.settings.CardSettingsScreen
import com.nainiuzhen.wiki.ui.settings.ImageScaleSettingsScreen
import com.nainiuzhen.wiki.ui.settings.about.LicenseScreen
import com.nainiuzhen.wiki.utils.LocalAppSettings
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions

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
 * 过渡样式（Miuix / Modal）、圆角裁剪、遮罩变暗、转场拦截输入均跟随设置页偏好（变更点 #23/#26）。
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
    // 过渡动画：navTransitionStyle == 1 → Modal，否则 MiuixDefault（pit #4：无 AOSP）。
    val transition = if (appState.navTransitionStyle == 1) NavTransitions.Modal else NavTransitions.MiuixDefault
    // 正交转场效果：圆角裁剪 / 遮罩变暗 / 转场拦截输入跟随开关。
    val effects = NavDisplayEffects(
        enableCornerClip = appState.enableCornerClip,
        dimAmount = if (appState.enableDim) 0.5f else 0f,
        blockInputDuringTransition = appState.blockInputDuringTransition,
    )
    // 滑动返回：关闭开关时显式禁用，否则继承过渡默认方向。
    val swipe = if (appState.enableSwipeBack) null else NavSwipeDirection.None
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
        entry<Route.ItemList>(swipeDismiss = swipe) {
            ItemListScreen()
        }
        entry<Route.RecipeList>(swipeDismiss = swipe) {
            RecipeListScreen()
        }
        entry<Route.NpcList>(swipeDismiss = swipe) {
            NpcListScreen()
        }
        entry<Route.NpcSchedule>(swipeDismiss = swipe) { route ->
            NpcScheduleScreen(npcId = route.npcId)
        }
        entry<Route.About>(swipeDismiss = swipe) {
            AboutScreen()
        }
        entry<Route.License>(swipeDismiss = swipe) {
            LicenseScreen()
        }
        entry<Route.ImageScaleSettings>(swipeDismiss = swipe) {
            ImageScaleSettingsScreen()
        }
        entry<Route.CardSettings>(swipeDismiss = swipe) {
            CardSettingsScreen()
        }
    }
}
