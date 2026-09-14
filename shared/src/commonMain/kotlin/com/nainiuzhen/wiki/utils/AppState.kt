package com.nainiuzhen.wiki.utils

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 全局应用设置状态。由 [com.nainiuzhen.wiki.App] 持有并通过 CompositionLocal 下发；
 * 设置页（[com.nainiuzhen.wiki.ui.settings.SettingsScreen]）通过
 * [LocalUpdateAppSettings] 修改，并经由 [com.nainiuzhen.wiki.utils.AppSettingsStore] 落盘。
 *
 * v5 变更（相对于 v4）：
 * - 移除 `isDark`，新增 `colorMode`（0=系统/Auto、1=深色、2=浅色）与 `monet`（Monet 取色开关），
 *   两者组合成 6 套主题（前 3 套 Monet 关、后 3 套 Monet 开）。
 * - 新增 14 个开关（#3），全部在此集中声明并由 [AndroidAppSettingsStore] 持久化读写。
 */
data class AppState(
    // —— 主主题（取代旧 isDark）—— //
    val colorMode: Int = 0, // 0=系统(Auto) 1=深色 2=浅色
    val monet: Boolean = false, // Monet 取色开关（与 colorMode 组合成 6 套）
    // —— v4 沿用 —— //
    val enableBlur: Boolean = true,
    val enableSquircle: Boolean = true,
    val navTransitionStyle: Int = 0, // 0=MiuixDefault 1=Modal（git#4，无 AOSP）
    val enableSwipeBack: Boolean = true,
    // —— v5 新增 14 开关（#3，全部真正接续）—— //
    val enableCornerClip: Boolean = true, // Enable Corner Clip
    val scrollEndHaptic: Boolean = true, // Enable Scroll End Haptic
    val pageUserScroll: Boolean = true, // Enable Page User Scroll
    val showTopAppBar: Boolean = true, // Show TopAppBar
    val topAppBarBlurStyle: Int = 0, // TopAppBar Blur Style (0=Gaussian 1=Progressive)
    val showNavigationBar: Boolean = true, // Show NavigationBar
    val showNavigationBadge: Boolean = false, // Show Navigation Badge（默认关闭，避免遮住底栏 icon）
    val navigationBarMode: Int = 0, // NavigationBar Mode (0=IconAndText 1=IconOnly 2=IconWithSelectedLabel)
    val useFloatingNavigationBar: Boolean = false, // Use FloatingNavigationBar
    val showFloatingToolbar: Boolean = false, // Show FloatingToolbar
    val floatingToolbarPosition: Int = 0, // FloatingToolbar Position (0=End 1=Start 2=Center)
    val showFloatingActionButton: Boolean = false, // Show FloatingActionButton
    val floatingActionButtonPosition: Int = 0, // FAB Position (0=End 1=Start 2=Center)
    val enableDim: Boolean = false, // Enable Dim
    val blockInputDuringTransition: Boolean = true, // Block Input During Transition
    // —— v6 新增 —— //
    val floatingNavigationBarStyle: Int = 0, // FloatingNavigationBar Style (0=Default/Miuix 1=iOS)
    val floatingNavigationBarPosition: Int = 0, // FloatingNavigationBar Position (0=Center 1=Start 2=End)
    // —— v7 新增：素材缩放倍率（设置子页可调，#22）—— //
    // 全部为 Float，滑块以 0.1 为步进；括号内为「最大值」（见 ImageScaleSettingsScreen 的 valueRange）。
    val cardImageScale: Float = 5f, // 卡片素材：物品/配方卡片内图片（最大 8）
    val homeImageScale: Float = 8f, // 主页左侧素材：主页物品/配方入口卡片图（最大 10）
    val dialogBodyImageScale: Float = 6f, // 弹窗本体素材：物品/配方详情头部素材（最大 8）
    val dialogRecipeImageScale: Float = 6f, // 弹窗配方素材：配方原料/产物（最大 8）
    val dialogFavHateImageScale: Float = 6f, // 弹窗喜恶素材：NPC 最爱/喜欢/讨厌（最大 8）
    val scaleStep: Float = 0.1f, // 滑块步进：0.1 / 0.5 / 1（设置子页可调，#22 步长）
    // —— v8 新增：手机横屏开关（大屏适配 §8.3）—— //
    // 默认 false = 锁竖屏；true = 允许自由旋转。仅在手机（<sw600dp）生效，大屏由系统忽略方向请求。
    val allowPhoneLandscape: Boolean = false,
    // —— v9 新增：卡片外观设置（v31，设置子页 CardSettingsScreen）—— //
    // 三组「总开关 + n 板块 + 同步」，求值口径见 utils/CardAppearance.kt：
    //     effective = 总开关 && (同步 || 板块自己的值)
    // 三组总开关**默认全关** ? 默认态 = 无底色 / 直角 / 无胶囊。
    // 同步开关默认全开（但总开关关时跟随隐藏，不显示）。
    //
    // 卡片背景
    val cardBgMaster: Boolean = false,
    val cardBgSync: Boolean = true,
    val cardBgItem: Boolean = false,
    val cardBgRecipe: Boolean = false,
    val cardBgNpc: Boolean = false,
    // 卡片圆角（板块之下还有「四角 + 四角同步」一层）
    val cardCornerMaster: Boolean = false,
    val cardCornerSync: Boolean = true,
    val cardCornerItem: Boolean = false,
    val cardCornerRecipe: Boolean = false,
    val cardCornerNpc: Boolean = false,
    val cardCornerItemTL: Boolean = false,
    val cardCornerItemTR: Boolean = false,
    val cardCornerItemBL: Boolean = false,
    val cardCornerItemBR: Boolean = false,
    val cardCornerItemSync4: Boolean = true,
    val cardCornerRecipeTL: Boolean = false,
    val cardCornerRecipeTR: Boolean = false,
    val cardCornerRecipeBL: Boolean = false,
    val cardCornerRecipeBR: Boolean = false,
    val cardCornerRecipeSync4: Boolean = true,
    val cardCornerNpcTL: Boolean = false,
    val cardCornerNpcTR: Boolean = false,
    val cardCornerNpcBL: Boolean = false,
    val cardCornerNpcBR: Boolean = false,
    val cardCornerNpcSync4: Boolean = true,
    // 按下阴影的圆角是否跟卡片圆角同步（默认开）。关 ? 按下阴影一律直角。
    // 与「同步」同级，排在圆角组最后一行的下一行。
    val cardPressShadowSync: Boolean = true,
    // 文字胶囊（名称那行的蓝底）
    val cardCapsuleMaster: Boolean = false,
    val cardCapsuleSync: Boolean = true,
    val cardCapsuleItem: Boolean = false,
    val cardCapsuleRecipe: Boolean = false,
    val cardCapsuleNpc: Boolean = false,
)

/** 运行时应用版本信息：由 Android 端经 [BuildConfig.VERSION_NAME] / [BuildConfig.VERSION_CODE] 注入，
 * 保证设置页 / 关于页显示的版本与构建产物完全一致（不再依赖编译期常量的手动同步，#26）。 */
data class AppVersion(val name: String, val code: Int)

/** 应用版本 CompositionLocal：Android 端从 BuildConfig 注入真实值，commonMain 提供默认值兜底。 */
val LocalAppVersion = compositionLocalOf { AppVersion("1.2.5", 25) }

val LocalAppSettings = compositionLocalOf { AppState() }

val LocalUpdateAppSettings = staticCompositionLocalOf<(AppState) -> Unit> {
    error("No AppSettings updater provided!")
}
