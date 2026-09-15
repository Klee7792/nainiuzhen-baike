// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package component

import LocalNavigator
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.LocalStrings
import i18n.Str
import navigation.Route
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference
import kotlin.random.Random

fun LazyListScope.otherPageSection() {
    item(key = "other") {
        val s = LocalStrings.current
        val navigator = LocalNavigator.current
        SmallTitle(text = s[Str.Other])
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp),
        ) {
            ArrowPreference(
                title = s[Str.PullToRefreshTest],
                summary = s[Str.NavigateToAPullToRefreshPage],
                onClick = {
                    navigator.push(Route.PullToRefresh)
                },
            )
            ArrowPreference(
                title = s[Str.NavigationTest],
                summary = s[Str.NavigateToANavigationPage],
                onClick = { navigator.push(Route.Navigation(Random.nextLong().toString())) },
            )
            ArrowPreference(
                title = s[Str.MultiScaffoldTest2],
                summary = s[Str.NavigateToAMultiScaffoldPage],
                onClick = { navigator.push(Route.MultiScaffold) },
            )
            ArrowPreference(
                title = s[Str.NestedNavigationTest],
                summary = s[Str.ANavDisplayNestedInsideAnEntry],
                onClick = { navigator.push(Route.NestedNav) },
            )
            ArrowPreference(
                title = s[Str.OverscrollLoadMoreTest],
                summary = s[Str.FlingToTheBottomThenFlingAgain],
                onClick = { navigator.push(Route.OverscrollLoadMore) },
            )
        }
    }
}
