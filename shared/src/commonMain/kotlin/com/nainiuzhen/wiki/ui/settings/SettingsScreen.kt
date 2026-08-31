package com.nainiuzhen.wiki.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.ui.nav.LocalSpriteRepository
import com.nainiuzhen.wiki.ui.nav.Route
import com.nainiuzhen.wiki.utils.LocalAppVersion
import com.nainiuzhen.wiki.utils.LocalAppSettings
import com.nainiuzhen.wiki.utils.LocalUpdateAppSettings
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
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
 * 通用（v4 沿用）：启用圆角 / 启用模糊 / 过渡动画；数据：清理缓存 / 版本。
 */
@Composable
fun SettingsContent(innerPadding: PaddingValues, scrollBehavior: ScrollBehavior) {
    val navigator = LocalNavigator.current
    val sprite = LocalSpriteRepository.current
    val appState = LocalAppSettings.current
    val updateAppState = LocalUpdateAppSettings.current
    val appVersion = LocalAppVersion.current
    var cacheSize by remember { mutableStateOf(sprite.cacheSizeBytes()) }

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
                    summary = "跟随系统壁纸动态取色",
                    checked = appState.monet,
                    onCheckedChange = { updateAppState(appState.copy(monet = it)) },
                )
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
                SwitchPreference(
                    title = "启用滑动返回",
                    checked = appState.enableSwipeBack,
                    onCheckedChange = { updateAppState(appState.copy(enableSwipeBack = it)) },
                )
                SwitchPreference(
                    title = "启用圆角裁剪",
                    summary = "转场时顶部圆角裁剪",
                    checked = appState.enableCornerClip,
                    onCheckedChange = { updateAppState(appState.copy(enableCornerClip = it)) },
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
                            }
                        }
                    }
                }
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
                    title = "素材缩放设置",
                    summary = "卡片 / 主页 / 弹窗素材放大倍率",
                    onClick = { navigator.push(Route.ImageScaleSettings) },
                )
                ArrowPreference(
                    title = "清理缓存",
                    summary = "切片缓存 ${formatSize(cacheSize)}",
                    onClick = {
                        sprite.clearCache()
                        cacheSize = sprite.cacheSizeBytes()
                    },
                )
                BasicComponent(
                    title = "版本",
                    summary = "${appVersion.name} (${appVersion.code})",
                )
                ArrowPreference(
                    title = "关于",
                    summary = "奶牛镇百科 · 图鉴查询",
                    onClick = { navigator.push(Route.About) },
                )
            }
        }
    }
}

/** 缓存字节数格式化为 KB / MB 文本。 */
private fun formatSize(bytes: Long): String {
    val mb = bytes / 1024.0 / 1024.0
    return if (mb < 1) "${(bytes / 1024).toInt()} KB" else "%.2f MB".format(mb)
}
