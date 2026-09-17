package com.nainiuzhen.wiki.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.ui.nav.Route
import com.nainiuzhen.wiki.utils.LocalAppVersion
import com.nainiuzhen.wiki.utils.ShareDiagnosticsLog
import com.nainiuzhen.wiki.utils.LocalAppSettings
import com.nainiuzhen.wiki.utils.LocalUpdateAppSettings
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.animation.AnimatedVisibility
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 设置页（底栏第 2 页「设置」的主体，无自身顶栏/底栏，由 [com.nainiuzhen.wiki.ui.home.MainScreen] 包裹）。
 *
 * v5 变更（本文件）：T3 重构。
 * - 色彩模式：使用 [ArrowSegmentedPreference] 三段（系统 / 深色 / 浅色，上/下箭头切换，绑定 `appState.colorMode` 0/1/2；
 *   Monet 开启时文案切换为 Monet 系统 / Monet 深色 / Monet 浅色）。
 * - Monet 取色：[SwitchPreference] 绑定 `appState.monet`。
 * - 14 个开关：逐项 [SwitchPreference] / [OverlayDropdownPreference] 接线（中文命名），全部经
 *   [LocalAppSettings]/[LocalUpdateAppSettings] 持久化（开关清单见设计文档 §3.1）。
 * - 关于：[ArrowPreference] 跳转 [Route.About]。
 * 通用（v4 沿用）：启用圆角 / 启用模糊 / 过渡动画；数据：版本
 * （「清理缓存」入口已随切片去磁盘化移除：切片全量驻内存、不再写盘）。
 * v8 追加：新增「屏幕 → 手机横屏」开关（绑定 `appState.allowPhoneLandscape`，默认关 = 锁竖屏，仅对手机生效）。
 */
@Composable
fun SettingsContent(innerPadding: PaddingValues, scrollBehavior: ScrollBehavior) {
    val navigator = LocalNavigator.current
    val appState = LocalAppSettings.current
    val updateAppState = LocalUpdateAppSettings.current
    val appVersion = LocalAppVersion.current

    // 悬浮底栏(或普通底栏)的高度已由 miuix Scaffold 折进 innerPadding.bottom
    // （见 miuix Scaffold.kt：bottomBarPlaceable.height 会并入 innerPadding.bottom），
    // 因此列表底部直接用该值预留即可，无需硬编码 88.dp。

    // 色彩模式选项随 Monet 开关联动（前 3 态 Monet 关、后 3 态 Monet 开）。
    val colorModeOptions = if (appState.monet) {
        listOf("Monet 系统", "Monet 深色", "Monet 浅色")
    } else {
        listOf("系统", "深色", "浅色")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .then(if (appState.scrollEndHaptic) Modifier.scrollEndHaptic() else Modifier)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        // 底部预留 = Scaffold 已为悬浮底栏(或普通底栏)预留的 innerPadding.bottom
        // （miuix 会把 bottomBar 高度折进 innerPadding.bottom），再 +12.dp 留白，
        // 保证「关于」等末项可完整滚到浮栏之上、不被遮挡（对齐 miuix demo）。
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding(),
            bottom = innerPadding.calculateBottomPadding() + 12.dp,
        ),
    ) {
        item(key = "appearance") {
            SmallTitle(text = "外观")
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                OverlayDropdownPreference(
                    items = colorModeOptions,
                    selectedIndex = appState.colorMode.coerceIn(0, 2),
                    title = "色彩模式",
                    onSelectedIndexChange = { updateAppState(appState.copy(colorMode = it)) },
                )
                SwitchPreference(
                    title = "Monet 取色",
                    summary = "自动根据壁纸取色，如遇系统不支持，则自主选择下方配色",
                    checked = appState.monet,
                    onCheckedChange = { updateAppState(appState.copy(monet = it)) },
                )
                AnimatedVisibility(visible = appState.monet) {
                    MonetSeedSwatchRow(
                        selectedSeed = appState.monetSeed,
                        onSelect = { updateAppState(appState.copy(monetSeed = it)) },
                    )
                }
            }

            SmallTitle(text = "导航")
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                OverlayDropdownPreference(
                    items = listOf("Miuix", "AOSP"),
                    selectedIndex = appState.navTransitionStyle,
                    title = "过渡动画",
                    onSelectedIndexChange = { updateAppState(appState.copy(navTransitionStyle = it)) },
                )
                // 顺序对齐 miuix demo SettingsPage：过渡动画 → 圆角裁剪 → 压暗 → 阻止输入 → 滑动返回。
                SwitchPreference(
                    title = "启用圆角裁剪",
                    summary = "转场时顶部圆角裁剪",
                    checked = appState.enableCornerClip,
                    onCheckedChange = { updateAppState(appState.copy(enableCornerClip = it)) },
                )
                SwitchPreference(
                    title = "启用压暗",
                    summary = "转场时压暗后方页面",
                    checked = appState.enableDim,
                    onCheckedChange = { updateAppState(appState.copy(enableDim = it)) },
                )
                SwitchPreference(
                    title = "转场期间阻止输入",
                    summary = "转场动画期间阻止触摸输入",
                    checked = appState.blockInputDuringTransition,
                    onCheckedChange = { updateAppState(appState.copy(blockInputDuringTransition = it)) },
                )
                SwitchPreference(
                    title = "启用滑动返回",
                    checked = appState.enableSwipeBack,
                    onCheckedChange = { updateAppState(appState.copy(enableSwipeBack = it)) },
                )
            }

            SmallTitle(text = "显示")
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                SwitchPreference(
                    title = "显示顶栏",
                    checked = appState.showTopAppBar,
                    onCheckedChange = { updateAppState(appState.copy(showTopAppBar = it)) },
                )
                SwitchPreference(
                    title = "启用模糊",
                    summary = "顶栏模糊效果",
                    checked = appState.enableBlur,
                    onCheckedChange = { updateAppState(appState.copy(enableBlur = it)) },
                )
                AnimatedVisibility(visible = appState.showTopAppBar && appState.enableBlur) {
                    OverlayDropdownPreference(
                        items = listOf("高斯模糊", "渐进模糊"),
                        selectedIndex = appState.topAppBarBlurStyle,
                        title = "顶栏模糊样式",
                        onSelectedIndexChange = { updateAppState(appState.copy(topAppBarBlurStyle = it)) },
                    )
                }
                SwitchPreference(
                    title = "显示底栏",
                    checked = appState.showNavigationBar,
                    onCheckedChange = { updateAppState(appState.copy(showNavigationBar = it)) },
                )
                AnimatedVisibility(visible = appState.showNavigationBar && !appState.useFloatingNavigationBar) {
                    OverlayDropdownPreference(
                        items = listOf("图标+文字", "仅图标", "选中显示文字"),
                        selectedIndex = appState.navigationBarMode,
                        title = "底栏模式",
                        onSelectedIndexChange = { updateAppState(appState.copy(navigationBarMode = it)) },
                    )
                }
                AnimatedVisibility(visible = appState.showNavigationBar) {
                    Column {
                        SwitchPreference(
                            title = "悬浮底栏",
                            checked = appState.useFloatingNavigationBar,
                            onCheckedChange = { updateAppState(appState.copy(useFloatingNavigationBar = it)) },
                        )
                        AnimatedVisibility(visible = appState.useFloatingNavigationBar) {
                            Column {
                                OverlayDropdownPreference(
                                    items = listOf("Miuix", "iOS"),
                                    selectedIndex = appState.floatingNavigationBarStyle,
                                    title = "悬浮底栏样式",
                                    onSelectedIndexChange = {
                                        updateAppState(appState.copy(floatingNavigationBarStyle = it))
                                    },
                                )
                                AnimatedVisibility(visible = appState.floatingNavigationBarStyle == 0) {
                                    Column {
                                        OverlayDropdownPreference(
                                            items = listOf("中心", "开始", "结束"),
                                            selectedIndex = appState.floatingNavigationBarPosition,
                                            title = "悬浮底栏位置",
                                            onSelectedIndexChange = {
                                                updateAppState(appState.copy(floatingNavigationBarPosition = it))
                                            },
                                        )
                                    }
                                }
                                // 交互期玻璃降级：仅 iOS 液态玻璃底栏（样式=1）生效时出现；
                                // 拖动底栏时临时跳过 lens 折射 / 关闭色散以保流畅（见 IosLiquidGlassNavigationBar B3）。
                                AnimatedVisibility(visible = appState.floatingNavigationBarStyle == 1) {
                                    SwitchPreference(
                                        title = "交互期玻璃降级",
                                        summary = "拖动底栏时临时降低折射细节以保流畅",
                                        checked = appState.glassInteractionDegrade,
                                        onCheckedChange = {
                                            updateAppState(appState.copy(glassInteractionDegrade = it))
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SmallTitle(text = "屏幕")
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                SwitchPreference(
                    title = "手机横屏",
                    summary = "仅对手机生效",
                    checked = appState.allowPhoneLandscape,
                    onCheckedChange = { updateAppState(appState.copy(allowPhoneLandscape = it)) },
                )
            }

            SmallTitle(text = "交互")
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                SwitchPreference(
                    title = "滚动到末尾震动",
                    checked = appState.scrollEndHaptic,
                    onCheckedChange = { updateAppState(appState.copy(scrollEndHaptic = it)) },
                )
                SwitchPreference(
                    title = "允许手动滑动翻页",
                    summary = "主页与设置页可左右滑动切换",
                    checked = appState.pageUserScroll,
                    onCheckedChange = { updateAppState(appState.copy(pageUserScroll = it)) },
                )
            }

            SmallTitle(text = "数据")
            Card(
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                ArrowPreference(
                    title = "卡片设置",
                    summary = "板块卡片的背景 / 圆角 / 名称胶囊",
                    onClick = { navigator.openTopLevel(Route.CardSettings) },
                )
                ArrowPreference(
                    title = "素材缩放设置",
                    summary = "卡片 / 主页 / 弹窗素材放大倍率",
                    onClick = { navigator.openTopLevel(Route.ImageScaleSettings) },
                )
                // 「清理缓存」入口已隐藏：切片全量驻内存、不再落盘（磁盘切片残留由启动时
                // AppRoot 清理，内存表由 preloadAllSprites 全量重建），该入口失去意义。
                BasicComponent(
                    title = "版本",
                    summary = "${appVersion.name} (${appVersion.code})",
                )
                ArrowPreference(
                    title = "诊断日志",
                    summary = "弹出系统分享，可存本地或经 QQ/微信/隔空投送发送（排障用）",
                    onClick = { ShareDiagnosticsLog() },
                )
                ArrowPreference(
                    title = "关于",
                    summary = "奶牛镇百科 · 图鉴查询",
                    onClick = { navigator.openTopLevel(Route.About) },
                )
            }
        }
    }
}

/** 预设取色种子色板（ARGB）。「默认」（种子 0）不在本表内，单独用文字圆钮表示。 */
private val MONET_SEED_PALETTE = listOf(
    0xFFE5484D.toInt(), // 红
    0xFFF76B15.toInt(), // 橙
    0xFFF5A623.toInt(), // 金
    0xFF30A46C.toInt(), // 绿
    0xFF12A594.toInt(), // 青绿
    0xFF0090FF.toInt(), // 蓝
    0xFF3E63DD.toInt(), // 靛蓝
    0xFF8E4EC6.toInt(), // 紫
    0xFFE93D82.toInt(), // 玫红
    0xFF8D6E63.toInt(), // 棕
)

/**
 * Monet 取色的「主题种子色」色板行（v40，随 Monet 开关显隐）。
 *
 * - 首个「默认」圆钮 = 种子 0：Android 跟随壁纸动态取色；iOS 无壁纸取色 API，回落 miuix 默认紫。
 * - 其余圆钮 = 手动种子，经 material-color-utilities 生成整套色板（miuix ThemeController.keyColor）。
 *   两端行为一致；iOS 用户由此获得可控的动态配色。
 */
@Composable
private fun MonetSeedSwatchRow(selectedSeed: Int, onSelect: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "主题种子色",
            fontSize = 12.sp,
            color = MiuixTheme.colorScheme.onSurfaceSecondary,
        )
        Spacer(modifier = Modifier.height(10.dp))
        // 横向滚动：竖屏全宽可放下 默认+9 色，横屏/分栏放不下 —— 可滚保证全部色板可达。
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            MonetSeedSwatch(argb = null, selected = selectedSeed == 0) { onSelect(0) }
            MONET_SEED_PALETTE.forEach { argb ->
                MonetSeedSwatch(argb = argb, selected = selectedSeed == argb) { onSelect(argb) }
            }
        }
    }
}

@Composable
private fun MonetSeedSwatch(argb: Int?, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.outline
                },
                shape = CircleShape,
            )
            .padding(3.dp)
            .clip(CircleShape)
            .background(if (argb != null) Color(argb) else MiuixTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (argb == null) {
            Text(text = "默", fontSize = 8.sp, color = MiuixTheme.colorScheme.onSurfaceSecondary)
        }
    }
}
