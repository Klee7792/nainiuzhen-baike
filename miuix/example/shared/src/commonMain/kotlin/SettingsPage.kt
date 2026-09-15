// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

@file:OptIn(ExperimentalScrollBarApi::class)

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import i18n.LocalStrings
import i18n.Str
import misc.VersionInfo
import navigation.Route
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import utils.AdaptiveTopAppBar
import utils.BlurredBar
import utils.pageContentPadding
import utils.pageScrollModifiers
import utils.rememberBlurBackdrop

// Option labels that mirror a library enum are shown verbatim.
private val NavigationBarDisplayModeOptions = listOf("IconAndText", "IconOnly", "IconWithSelectedLabel")
private val FloatingNavigationBarPositionOptions = listOf("Center", "Start", "End")
private val FloatingToolbarPositionOptions =
    listOf("TopStart", "CenterStart", "BottomStart", "TopEnd", "CenterEnd", "BottomEnd", "TopCenter", "BottomCenter")
private val FabPositionOptions = listOf("Start", "Center", "End", "EndOverlay")
private val NavTransitionStyleOptions = listOf("Miuix", "AOSP")
private val PaletteStyleOptions = ThemePaletteStyle.entries.map { it.name }
private val ColorSpecOptions = ThemeColorSpec.entries.map { it.name }

// Same order as [ui.KeyColors].
private val KeyColorLabelKeys =
    listOf(Str.Blue, Str.Green, Str.Purple, Str.Yellow, Str.Orange, Str.Pink, Str.Teal)

@Composable
fun SettingsPage(
    padding: PaddingValues,
) {
    val s = LocalStrings.current
    val appState = LocalAppState.current
    val isWideScreen = LocalIsWideScreen.current
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val topAppBarScrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            BlurredBar(backdrop, blurActive, topAppBarScrollBehavior) {
                AdaptiveTopAppBar(
                    title = s[Str.Settings],
                    showTopAppBar = appState.showTopAppBar,
                    isWideScreen = isWideScreen,
                    scrollBehavior = topAppBarScrollBehavior,
                    color = barColor,
                    subtitle = s.format(Str.VersionLine, VersionInfo.VERSION_NAME, VersionInfo.VERSION_CODE),
                )
            }
        },
    ) { innerPadding ->
        SettingsContent(
            padding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding(),
            ),
            topAppBarScrollBehavior = topAppBarScrollBehavior,
            backdrop = backdrop,
        )
    }
}

@Composable
private fun SettingsContent(
    padding: PaddingValues,
    topAppBarScrollBehavior: ScrollBehavior,
    backdrop: LayerBackdrop?,
) {
    val s = LocalStrings.current
    val appState = LocalAppState.current
    val isWideScreen = LocalIsWideScreen.current
    val updateAppState = LocalUpdateAppState.current
    val navigator = LocalNavigator.current
    val lazyListState = rememberLazyListState()

    // Option labels are resolved here so that they follow the language setting.
    val languageOptions = remember(s) {
        listOf(
            s[Str.LanguageSystem],
            "中文",
            "English",
        )
    }
    val colorModeOptions = remember(s) {
        listOf(
            s[Str.System],
            s[Str.Light],
            s[Str.Dark],
            s[Str.MonetSystem],
            s[Str.MonetLight],
            s[Str.MonetDark],
        )
    }
    val keyColorOptions = remember(s) { listOf(s[Str.Default]) + KeyColorLabelKeys.map { s[it] } }
    val floatingNavigationBarStyleOptions = remember(s) { listOf(s[Str.Default], "iOS-like") }
    val floatingToolbarOrientationOptions = remember(s) { listOf(s[Str.Horizontal], s[Str.Vertical]) }
    val blurStyleOptions = remember(s) { listOf(s[Str.Gaussian], s[Str.Progressive]) }

    val contentPadding = pageContentPadding(padding, padding, isWideScreen)
    Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.pageScrollModifiers(
                appState.enableScrollEndHaptic,
                appState.showTopAppBar,
                topAppBarScrollBehavior,
            ),
            contentPadding = contentPadding,
        ) {
            item(key = "settingsUi") {
                Card(
                    modifier = Modifier.padding(12.dp),
                ) {
                    OverlayDropdownPreference(
                        title = s[Str.Language],
                        items = languageOptions,
                        selectedIndex = appState.language,
                        onSelectedIndexChange = { updateAppState { state -> state.copy(language = it) } },
                    )
                    SwitchPreference(
                        title = s[Str.ShowFPSMonitor],
                        checked = appState.showFPSMonitor,
                        onCheckedChange = { updateAppState { state -> state.copy(showFPSMonitor = it) } },
                    )
                    OverlayDropdownPreference(
                        title = s[Str.ColorMode],
                        items = colorModeOptions,
                        selectedIndex = appState.colorMode,
                        onSelectedIndexChange = { updateAppState { state -> state.copy(colorMode = it) } },
                    )
                    AnimatedVisibility(visible = appState.colorMode in 3..5) {
                        OverlayDropdownPreference(
                            title = s[Str.KeyColor],
                            items = keyColorOptions,
                            selectedIndex = appState.seedIndex,
                            onSelectedIndexChange = { updateAppState { state -> state.copy(seedIndex = it) } },
                        )
                    }
                    AnimatedVisibility(visible = appState.colorMode in 3..5 && appState.seedIndex > 0) {
                        Column {
                            OverlayDropdownPreference(
                                title = s[Str.PaletteStyle],
                                items = PaletteStyleOptions,
                                selectedIndex = appState.paletteStyle,
                                onSelectedIndexChange = { updateAppState { state -> state.copy(paletteStyle = it) } },
                            )
                            OverlayDropdownPreference(
                                title = s[Str.ColorSpec],
                                items = ColorSpecOptions,
                                selectedIndex = appState.colorSpec,
                                onSelectedIndexChange = { updateAppState { state -> state.copy(colorSpec = it) } },
                            )
                        }
                    }
                    AnimatedVisibility(visible = isRuntimeShaderSupported()) {
                        SwitchPreference(
                            title = s[Str.EnableSquircleShapes],
                            checked = appState.enableSquircle,
                            onCheckedChange = { updateAppState { state -> state.copy(enableSquircle = it) } },
                        )
                    }
                    AnimatedVisibility(visible = isRuntimeShaderSupported()) {
                        SwitchPreference(
                            title = s[Str.EnableBlurEffect],
                            checked = appState.enableBlur,
                            onCheckedChange = { updateAppState { state -> state.copy(enableBlur = it) } },
                        )
                    }
                    SwitchPreference(
                        title = s[Str.EnableScrollEndHaptic],
                        checked = appState.enableScrollEndHaptic,
                        onCheckedChange = { updateAppState { state -> state.copy(enableScrollEndHaptic = it) } },
                    )
                    SwitchPreference(
                        title = s[Str.EnablePageUserScroll],
                        checked = appState.enablePageUserScroll,
                        onCheckedChange = { updateAppState { state -> state.copy(enablePageUserScroll = it) } },
                    )
                    SwitchPreference(
                        title = s[Str.ShowTopAppBar],
                        checked = appState.showTopAppBar,
                        onCheckedChange = { updateAppState { state -> state.copy(showTopAppBar = it) } },
                    )
                    AnimatedVisibility(visible = appState.showTopAppBar && appState.enableBlur && isRuntimeShaderSupported()) {
                        OverlayDropdownPreference(
                            title = s[Str.TopAppBarBlurStyle],
                            items = blurStyleOptions,
                            selectedIndex = appState.blurStyle,
                            onSelectedIndexChange = { updateAppState { state -> state.copy(blurStyle = it) } },
                        )
                    }
                    SwitchPreference(
                        title = if (isWideScreen) s[Str.ShowNavigationRail] else s[Str.ShowNavigationBar],
                        checked = appState.showNavigationBar,
                        onCheckedChange = { updateAppState { state -> state.copy(showNavigationBar = it) } },
                    )
                    AnimatedVisibility(visible = appState.showNavigationBar) {
                        SwitchPreference(
                            title = s[Str.ShowNavigationBadge],
                            checked = appState.showNavigationBarBadge,
                            onCheckedChange = { updateAppState { state -> state.copy(showNavigationBarBadge = it) } },
                        )
                    }
                    AnimatedVisibility(visible = appState.showNavigationBar && !isWideScreen && !appState.useFloatingNavigationBar) {
                        OverlayDropdownPreference(
                            title = s[Str.NavigationBarMode],
                            items = NavigationBarDisplayModeOptions,
                            selectedIndex = appState.navigationBarMode,
                            onSelectedIndexChange = { updateAppState { state -> state.copy(navigationBarMode = it) } },
                        )
                    }
                    AnimatedVisibility(visible = appState.showNavigationBar && !isWideScreen) {
                        Column {
                            SwitchPreference(
                                title = s[Str.UseFloatingNavigationBar],
                                checked = appState.useFloatingNavigationBar,
                                onCheckedChange = { updateAppState { state -> state.copy(useFloatingNavigationBar = it) } },
                            )
                            AnimatedVisibility(visible = appState.useFloatingNavigationBar) {
                                Column {
                                    OverlayDropdownPreference(
                                        title = s[Str.FloatingNavigationBarStyle],
                                        items = floatingNavigationBarStyleOptions,
                                        selectedIndex = appState.floatingNavigationBarStyle,
                                        onSelectedIndexChange = { updateAppState { state -> state.copy(floatingNavigationBarStyle = it) } },
                                    )
                                    AnimatedVisibility(visible = appState.floatingNavigationBarStyle == 0) {
                                        Column {
                                            OverlayDropdownPreference(
                                                title = s[Str.FloatingNavigationBarPosition],
                                                items = FloatingNavigationBarPositionOptions,
                                                selectedIndex = appState.floatingNavigationBarPosition,
                                                onSelectedIndexChange = { updateAppState { state -> state.copy(floatingNavigationBarPosition = it) } },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    SwitchPreference(
                        title = s[Str.ShowFloatingToolbar],
                        checked = appState.showFloatingToolbar,
                        onCheckedChange = { updateAppState { state -> state.copy(showFloatingToolbar = it) } },
                    )
                    AnimatedVisibility(visible = appState.showFloatingToolbar) {
                        Column {
                            OverlayDropdownPreference(
                                title = s[Str.FloatingToolbarPosition],
                                items = FloatingToolbarPositionOptions,
                                selectedIndex = appState.floatingToolbarPosition,
                                onSelectedIndexChange = { updateAppState { state -> state.copy(floatingToolbarPosition = it) } },
                            )
                            OverlayDropdownPreference(
                                title = s[Str.FloatingToolbarOrientation],
                                items = floatingToolbarOrientationOptions,
                                selectedIndex = appState.floatingToolbarOrientation,
                                onSelectedIndexChange = { updateAppState { state -> state.copy(floatingToolbarOrientation = it) } },
                            )
                        }
                    }
                    SwitchPreference(
                        title = s[Str.ShowFloatingActionButton],
                        checked = appState.showFloatingActionButton,
                        onCheckedChange = { updateAppState { state -> state.copy(showFloatingActionButton = it) } },
                    )
                    AnimatedVisibility(visible = appState.showFloatingActionButton) {
                        OverlayDropdownPreference(
                            title = s[Str.FloatingActionButtonPosition],
                            items = FabPositionOptions,
                            selectedIndex = appState.floatingActionButtonPosition,
                            onSelectedIndexChange = { updateAppState { state -> state.copy(floatingActionButtonPosition = it) } },
                        )
                    }
                }
            }
            item(key = "settingsTransition") {
                SmallTitle(s[Str.Navigation])
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                ) {
                    OverlayDropdownPreference(
                        title = s[Str.TransitionStyle],
                        items = NavTransitionStyleOptions,
                        selectedIndex = appState.navTransitionStyle,
                        onSelectedIndexChange = { updateAppState { state -> state.copy(navTransitionStyle = it) } },
                    )
                    SwitchPreference(
                        title = s[Str.EnableCornerClip],
                        summary = s[Str.ClipTheTopSceneWithRoundedCornersDuringTrans],
                        checked = appState.enableCornerClip,
                        onCheckedChange = { updateAppState { state -> state.copy(enableCornerClip = it) } },
                    )
                    SwitchPreference(
                        title = s[Str.EnableDim],
                        summary = s[Str.DimTheSceneBehindDuringTransitions],
                        checked = appState.enableDim,
                        onCheckedChange = { updateAppState { state -> state.copy(enableDim = it) } },
                    )
                    SwitchPreference(
                        title = s[Str.BlockInputDuringTransition],
                        summary = s[Str.BlockTouchInputOnTheNonTargetScene],
                        checked = appState.blockInputDuringTransition,
                        onCheckedChange = { updateAppState { state -> state.copy(blockInputDuringTransition = it) } },
                    )
                    SwitchPreference(
                        title = s[Str.EnableSwipeBack],
                        summary = s[Str.SwipeAPushedPageToPopItDirectionFollowsLayou],
                        checked = appState.enableSwipeBack,
                        onCheckedChange = { updateAppState { state -> state.copy(enableSwipeBack = it) } },
                    )
                }
            }
            item(key = "settingsAbout") {
                SmallTitle(s[Str.Other])
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    ArrowPreference(
                        title = s[Str.About],
                        summary = s[Str.AboutThisExampleApp],
                        onClick = { navigator.push(Route.About) },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
        VerticalScrollBar(
            adapter = rememberScrollBarAdapter(lazyListState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            trackPadding = contentPadding,
        )
    }
}
