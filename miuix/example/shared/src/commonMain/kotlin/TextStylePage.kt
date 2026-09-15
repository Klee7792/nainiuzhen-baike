// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

@file:OptIn(ExperimentalScrollBarApi::class)

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import i18n.LocalStrings
import i18n.Str
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.theme.MiuixTheme
import utils.AdaptiveTopAppBar
import utils.BlurredBar
import utils.pageContentPadding
import utils.pageScrollModifiers
import utils.rememberBlurBackdrop

@Immutable
private data class TextStyleEntry(
    val name: String,
    val style: TextStyle,
    val description: String,
)

private const val SAMPLE_TEXT_CN = "天地玄黄 宇宙洪荒"
private const val SAMPLE_TEXT_EN = "The Quick Brown Fox Jumps"
private const val SAMPLE_TEXT_NUM = "0123456789 !@#$%&"

@Composable
fun TextStylePage(
    padding: PaddingValues,
) {
    val s = LocalStrings.current
    val appState = LocalAppState.current
    val isWideScreen = LocalIsWideScreen.current
    val topAppBarScrollBehavior = MiuixScrollBehavior()

    val textStyles = MiuixTheme.textStyles

    val styleEntries = remember(s, textStyles) {
        listOf(
            TextStyleEntry("title1", textStyles.title1, "32sp"),
            TextStyleEntry("title2", textStyles.title2, "24sp"),
            TextStyleEntry("title3", textStyles.title3, "20sp"),
            TextStyleEntry("title4", textStyles.title4, "18sp"),
            TextStyleEntry("headline1", textStyles.headline1, "17sp"),
            TextStyleEntry("headline2", textStyles.headline2, "16sp"),
            TextStyleEntry("subtitle", textStyles.subtitle, s[Str.TextStyleBoldSpec]),
            TextStyleEntry("main", textStyles.main, "17sp"),
            TextStyleEntry("paragraph", textStyles.paragraph, s[Str.TextStyleLineHeightSpec]),
            TextStyleEntry("body1", textStyles.body1, "16sp"),
            TextStyleEntry("body2", textStyles.body2, "14sp"),
            TextStyleEntry("button", textStyles.button, "17sp"),
            TextStyleEntry("footnote1", textStyles.footnote1, "13sp"),
            TextStyleEntry("footnote2", textStyles.footnote2, "11sp"),
        )
    }

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop, blurActive, topAppBarScrollBehavior) {
                AdaptiveTopAppBar(
                    title = s[Str.TextStyle2],
                    showTopAppBar = appState.showTopAppBar,
                    isWideScreen = isWideScreen,
                    scrollBehavior = topAppBarScrollBehavior,
                    color = barColor,
                )
            }
        },
    ) { innerPadding ->
        val lazyListState = rememberLazyListState()
        val contentPadding = pageContentPadding(innerPadding, padding, isWideScreen)
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
                item(key = "title_header") {
                    SmallTitle(s[Str.TitleStyles])
                }
                item(key = "title_card") {
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                        cornerRadius = 16.dp,
                        insideMargin = PaddingValues(horizontal = 16.dp),
                    ) {
                        styleEntries.subList(0, 4).forEachIndexed { index, entry ->
                            if (index > 0) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            }
                            TextStyleItem(entry)
                        }
                    }
                }
                item(key = "headline_header") {
                    SmallTitle(s[Str.HeadlineStyles])
                }
                item(key = "headline_card") {
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                        cornerRadius = 16.dp,
                        insideMargin = PaddingValues(horizontal = 16.dp),
                    ) {
                        styleEntries.subList(4, 6).forEachIndexed { index, entry ->
                            if (index > 0) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            }
                            TextStyleItem(entry)
                        }
                    }
                }
                item(key = "body_header") {
                    SmallTitle(s[Str.BodyStyles])
                }
                item(key = "body_card") {
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                        cornerRadius = 16.dp,
                        insideMargin = PaddingValues(horizontal = 16.dp),
                    ) {
                        styleEntries.subList(6, 12).forEachIndexed { index, entry ->
                            if (index > 0) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            }
                            TextStyleItem(entry)
                        }
                    }
                }
                item(key = "footnote_header") {
                    SmallTitle(s[Str.FootnoteStyles])
                }
                item(key = "footnote_card") {
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                        cornerRadius = 16.dp,
                        insideMargin = PaddingValues(horizontal = 16.dp),
                    ) {
                        styleEntries.subList(12, 14).forEachIndexed { index, entry ->
                            if (index > 0) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            }
                            TextStyleItem(entry)
                        }
                    }
                }
                item(key = "all_header") {
                    SmallTitle(s[Str.AllStylesOverview])
                }
                item(key = "all_card") {
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                        cornerRadius = 16.dp,
                        insideMargin = PaddingValues(horizontal = 16.dp),
                    ) {
                        styleEntries.forEachIndexed { index, entry ->
                            if (index > 0) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            }
                            TextStyleItem(entry)
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
private fun TextStyleItem(entry: TextStyleEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.name,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = entry.description,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = SAMPLE_TEXT_CN,
            style = entry.style,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = SAMPLE_TEXT_EN,
            style = entry.style,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = SAMPLE_TEXT_NUM,
            style = entry.style,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}
