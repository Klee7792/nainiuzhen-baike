// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.LocalStrings
import i18n.Str
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text

fun LazyListScope.tabRowSection() {
    item(key = "tabRow") {
        val s = LocalStrings.current
        SmallTitle(text = s[Str.TabRow])
        val tabTexts = remember(s) { listOf(s[Str.Tab1], s[Str.Tab2], s[Str.Tab3]) }
        val tabTexts1 =
            remember(s) { listOf(s[Str.Tab1], s[Str.Tab2], s[Str.Tab3], s[Str.Tab4], s[Str.Tab5], s[Str.Tab6]) }
        var selectedTabIndex by remember { mutableIntStateOf(0) }
        val tabListState = rememberLazyListState()
        TabRow(
            tabs = tabTexts,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = {
                selectedTabIndex = it
            },
            listState = tabListState,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            insideMargin = PaddingValues(16.dp),
        ) {
            val scope = rememberCoroutineScope()
            val pagerState = rememberPagerState(pageCount = { tabTexts1.size })
            val contourTabListState = rememberLazyListState()
            TabRowWithContour(
                tabs = tabTexts1,
                selectedTabIndex = pagerState.currentPage,
                onTabSelected = {
                    scope.launch {
                        pagerState.animateScrollToPage(it)
                    }
                },
                listState = contourTabListState,
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                userScrollEnabled = true,
                key = { it },
                pageContent = { page ->
                    Text(
                        text = s.format(Str.TabContent, tabTexts1[page]),
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                },
            )
        }
    }
}
