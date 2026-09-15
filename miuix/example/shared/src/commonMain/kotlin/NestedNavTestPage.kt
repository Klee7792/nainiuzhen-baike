// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

@file:OptIn(ExperimentalScrollBarApi::class)

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import component.BackNavigationIcon
import i18n.LocalStrings
import i18n.Str
import kotlinx.serialization.Serializable
import navigation.Route
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import utils.AdaptiveTopAppBar
import utils.BlurredBar
import utils.pageContentPadding
import utils.pageScrollModifiers
import kotlin.random.Random

@Serializable
private sealed interface InnerRoute : NavKey {
    @Serializable
    data class Level(val depth: Int) : InnerRoute
}

/**
 * Demonstrates a NavDisplay nested inside an entry: the inner stack consumes system back first
 * while this page is the interactive top, falls through to the outer display at its root, and is
 * never popped by back while this page is covered by another outer page.
 */
@Composable
fun NestedNavTestPage(padding: PaddingValues) {
    val s = LocalStrings.current
    val appState = LocalAppState.current
    val isWideScreen = LocalIsWideScreen.current
    val blurSupported = isRuntimeShaderSupported()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = if (blurSupported) {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    } else {
        null
    }
    val blurActive = appState.enableBlur && blurSupported
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val navigator = LocalNavigator.current

    Scaffold(
        topBar = {
            BlurredBar(backdrop, blurActive, topAppBarScrollBehavior) {
                AdaptiveTopAppBar(
                    title = s[Str.NestedNavigation],
                    showTopAppBar = appState.showTopAppBar,
                    isWideScreen = isWideScreen,
                    scrollBehavior = topAppBarScrollBehavior,
                    color = barColor,
                    navigationIcon = {
                        BackNavigationIcon(
                            onClick = { navigator.pop() },
                        )
                    },
                )
            }
        },
    ) { innerPadding ->
        val lazyListState = rememberLazyListState()
        val contentPadding = pageContentPadding(
            innerPadding,
            padding,
            true,
            extraStart = if (isWideScreen) 0.dp else WindowInsets.displayCutout.asPaddingValues().calculateLeftPadding(LayoutDirection.Ltr),
            extraEnd = WindowInsets.displayCutout.asPaddingValues().calculateRightPadding(LayoutDirection.Ltr),
            extraBottom = 12.dp,
        )
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
                item(key = "nested_hint") {
                    Column {
                        SmallTitle(text = s[Str.BackSemantics])
                        Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                            BasicComponent(
                                title = s[Str.OneBackStreamNested],
                                summary = s[Str.NestedBackHint],
                            )
                        }
                    }
                }
                item(key = "nested_stack") {
                    val innerBackStack = rememberNavBackStack<InnerRoute>(InnerRoute.Level(1))
                    Column {
                        SmallTitle(text = s[Str.InnerStack])
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth()
                                .height(420.dp),
                        ) {
                            NavDisplay(
                                backStack = innerBackStack,
                                onBack = { innerBackStack.removeLastOrNull() },
                            ) {
                                entry<InnerRoute.Level> { route ->
                                    InnerLevelContent(
                                        depth = route.depth,
                                        // Idempotent: a double tap would push a duplicate contentKey.
                                        onPushInner = {
                                            val next = InnerRoute.Level(route.depth + 1)
                                            if (next !in innerBackStack) innerBackStack.add(next)
                                        },
                                        // The inner root must stay: an empty stack is rejected.
                                        onPopInner = {
                                            if (innerBackStack.size > 1) innerBackStack.removeLastOrNull()
                                        },
                                        onPushOuter = { navigator.push(Route.Navigation(Random.nextLong().toString())) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            VerticalScrollBar(
                adapter = rememberScrollBarAdapter(lazyListState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                trackPadding = contentPadding,
            )
        }
    }
}

@Composable
private fun InnerLevelContent(
    depth: Int,
    onPushInner: () -> Unit,
    onPopInner: () -> Unit,
    onPushOuter: () -> Unit,
) {
    val s = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surfaceContainer),
    ) {
        SmallTitle(text = s.format(Str.NestedLevel, List(depth) { it + 1 }.joinToString(" / ")))
        ArrowPreference(
            title = s[Str.PushInnerLevel],
            summary = s[Str.SystemBackPopsItBeforeThisPage],
            onClick = onPushInner,
        )
        if (depth > 1) {
            ArrowPreference(
                title = s[Str.PopInnerLevel],
                onClick = onPopInner,
            )
        }
        ArrowPreference(
            title = s[Str.PushAnOuterPageOnTop],
            summary = s[Str.BackThenPopsTheOuterPageNotThisStack],
            onClick = onPushOuter,
        )
    }
}
