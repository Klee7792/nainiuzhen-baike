// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.LocalStrings
import i18n.Str
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference

fun LazyListScope.dropdownSection() {
    item(key = "dropdown") {
        val s = LocalStrings.current
        var overlayDropdownOptionSelected by remember { mutableIntStateOf(0) }
        var windowDropdownOptionSelected by remember { mutableIntStateOf(0) }
        var overlayExpanded by remember { mutableStateOf(false) }
        var windowExpanded by remember { mutableStateOf(false) }
        val dropdownOptions = remember(s) { listOf(s[Str.Option1], s[Str.Option2], s[Str.Option3], s[Str.Option4]) }
        val dropdownLongOptions = remember(s) {
            listOf(
                s[Str.Option1],
                s[Str.LongOption2],
                s[Str.LongOption3],
                s[Str.LongOption4],
                s[Str.LongOption5],
                s[Str.LongOption6],
                s[Str.LongOption7],
                s[Str.LongOption8],
                s[Str.LongOption9],
                s[Str.LongOption10],
                s[Str.LongOption11],
                s[Str.LongOption12],
            )
        }

        var overlayGroupedExpanded by remember { mutableStateOf(false) }
        var overlayGroup1DropdownOptionSelected by remember { mutableIntStateOf(0) }
        var overlayGroup2DropdownOptionSelected by remember { mutableIntStateOf(0) }
        var overlayGroup3DropdownOptionSelected by remember { mutableIntStateOf(0) }
        val overlayMultiGroupOptions = remember(
            overlayGroup1DropdownOptionSelected,
            overlayGroup2DropdownOptionSelected,
            overlayGroup3DropdownOptionSelected,
            s,
        ) {
            listOf(
                DropdownEntry(
                    items = listOf(s[Str.OptionA1], s[Str.OptionA2])
                        .mapIndexed { index, text ->
                            DropdownItem(
                                text = text,
                                selected = overlayGroup1DropdownOptionSelected == index,
                                onClick = { overlayGroup1DropdownOptionSelected = index },
                            )
                        },
                ),
                DropdownEntry(
                    items = listOf(s[Str.OptionB1], s[Str.OptionB2], s[Str.OptionB3])
                        .mapIndexed { index, text ->
                            DropdownItem(
                                text = text,
                                selected = overlayGroup2DropdownOptionSelected == index,
                                onClick = { overlayGroup2DropdownOptionSelected = index },
                            )
                        },
                ),
                DropdownEntry(
                    items = listOf(s[Str.OptionC1], s[Str.OptionC2], s[Str.OptionC3], s[Str.OptionC4])
                        .mapIndexed { index, string ->
                            DropdownItem(
                                text = string,
                                enabled = index % 2 == 0,
                                selected = overlayGroup3DropdownOptionSelected == index,
                                onClick = { overlayGroup3DropdownOptionSelected = index },
                            )
                        },
                ),
            )
        }

        var windowGroupedExpanded by remember { mutableStateOf(false) }
        var windowGroup1DropdownOptionSelected by remember { mutableIntStateOf(0) }
        var windowGroup2DropdownOptionSelected by remember { mutableIntStateOf(0) }
        var windowGroup3DropdownOptionSelected by remember { mutableIntStateOf(0) }
        val windowMultiGroupOptions = remember(
            windowGroup1DropdownOptionSelected,
            windowGroup2DropdownOptionSelected,
            windowGroup3DropdownOptionSelected,
            s,
        ) {
            listOf(
                DropdownEntry(
                    items = listOf(s[Str.OptionA1], s[Str.OptionA2])
                        .mapIndexed { index, text ->
                            DropdownItem(
                                text = text,
                                selected = windowGroup1DropdownOptionSelected == index,
                                onClick = { windowGroup1DropdownOptionSelected = index },
                            )
                        },
                ),
                DropdownEntry(
                    items = listOf(s[Str.OptionB1], s[Str.OptionB2], s[Str.OptionB3])
                        .mapIndexed { index, text ->
                            DropdownItem(
                                text = text,
                                selected = windowGroup2DropdownOptionSelected == index,
                                onClick = { windowGroup2DropdownOptionSelected = index },
                            )
                        },
                ),
                DropdownEntry(
                    items = listOf(s[Str.OptionC1], s[Str.OptionC2], s[Str.OptionC3], s[Str.OptionC4])
                        .mapIndexed { index, string ->
                            DropdownItem(
                                text = string,
                                enabled = index % 2 == 0,
                                selected = windowGroup3DropdownOptionSelected == index,
                                onClick = { windowGroup3DropdownOptionSelected = index },
                            )
                        },
                ),
            )
        }

        SmallTitle(text = s[Str.Dropdown])
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        ) {
            OverlayDropdownPreference(
                title = s[Str.DropdownPrefO],
                summary = if (overlayExpanded) s[Str.Expanded] else s[Str.Collapsed],
                items = dropdownOptions,
                selectedIndex = overlayDropdownOptionSelected,
                onSelectedIndexChange = { newOption ->
                    overlayDropdownOptionSelected = newOption
                },
                onExpandedChange = { overlayExpanded = it },
            )
            WindowDropdownPreference(
                title = s[Str.DropdownPrefW],
                summary = if (windowExpanded) s[Str.Expanded] else s[Str.Collapsed],
                items = dropdownLongOptions,
                selectedIndex = windowDropdownOptionSelected,
                onSelectedIndexChange = { newOption ->
                    windowDropdownOptionSelected = newOption
                },
                onExpandedChange = { windowExpanded = it },
            )
            OverlayDropdownPreference(
                title = s[Str.GroupedDropdownPrefO],
                summary = if (overlayGroupedExpanded) s[Str.Expanded] else s[Str.Collapsed],
                entries = overlayMultiGroupOptions,
                collapseOnSelection = false,
                onExpandedChange = { overlayGroupedExpanded = it },
            )
            WindowDropdownPreference(
                title = s[Str.GroupedDropdownPrefW],
                summary = if (windowGroupedExpanded) s[Str.Expanded] else s[Str.Collapsed],
                entries = windowMultiGroupOptions,
                collapseOnSelection = false,
                onExpandedChange = { windowGroupedExpanded = it },
            )
            OverlayDropdownPreference(
                title = s[Str.DisabledDropdownPrefO],
                items = listOf(s[Str.Option1]),
                selectedIndex = 0,
                onSelectedIndexChange = {},
                enabled = false,
            )
            WindowDropdownPreference(
                title = s[Str.DisabledDropdownPrefW],
                items = listOf(s[Str.Option1]),
                selectedIndex = 0,
                onSelectedIndexChange = {},
                enabled = false,
            )
        }
    }
}
