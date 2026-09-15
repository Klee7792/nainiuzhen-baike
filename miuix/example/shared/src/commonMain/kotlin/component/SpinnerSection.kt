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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import i18n.LocalStrings
import i18n.Str
import i18n.Strings
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.OverlaySpinnerPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference

fun LazyListScope.spinnerSection() {
    item(key = "spinner") {
        val s = LocalStrings.current
        val superSpinnerOptionSelected = remember { mutableIntStateOf(0) }
        val windowSpinnerOptionSelected = remember { mutableIntStateOf(1) }
        val superSpinnerOptionSelectedDialog = remember { mutableIntStateOf(2) }
        val windowSpinnerOptionSelectedDialog = remember { mutableIntStateOf(3) }
        var overlayExpanded by remember { mutableStateOf(false) }
        var windowExpanded by remember { mutableStateOf(false) }
        var overlayDialogExpanded by remember { mutableStateOf(false) }
        var windowDialogExpanded by remember { mutableStateOf(false) }
        var overlayGroupedExpanded by remember { mutableStateOf(false) }
        var windowGroupedExpanded by remember { mutableStateOf(false) }
        var overlayGroupedDialogExpanded by remember { mutableStateOf(false) }
        var windowGroupedDialogExpanded by remember { mutableStateOf(false) }
        var overlayGroup1SpinnerOptionSelected by remember { mutableIntStateOf(0) }
        var overlayGroup2SpinnerOptionSelected by remember { mutableIntStateOf(0) }
        var overlayGroup3SpinnerOptionSelected by remember { mutableIntStateOf(0) }
        var windowGroup1SpinnerOptionSelected by remember { mutableIntStateOf(0) }
        var windowGroup2SpinnerOptionSelected by remember { mutableIntStateOf(0) }
        var windowGroup3SpinnerOptionSelected by remember { mutableIntStateOf(0) }
        var overlayDialogGroup1SpinnerOptionSelected by remember { mutableIntStateOf(0) }
        var overlayDialogGroup2SpinnerOptionSelected by remember { mutableIntStateOf(0) }
        var overlayDialogGroup3SpinnerOptionSelected by remember { mutableIntStateOf(0) }
        var windowDialogGroup1SpinnerOptionSelected by remember { mutableIntStateOf(0) }
        var windowDialogGroup2SpinnerOptionSelected by remember { mutableIntStateOf(0) }
        var windowDialogGroup3SpinnerOptionSelected by remember { mutableIntStateOf(0) }
        val spinnerOptions = remember(s) {
            listOf(
                DropdownItem(
                    icon = {
                        Icon(
                            RoundedRectanglePainter(),
                            s[Str.Icon],
                            Modifier.padding(end = 12.dp),
                            Color(0xFFFF5B29),
                        )
                    },
                    text = s[Str.Option1],
                    summary = s[Str.Red],
                ),
                DropdownItem(
                    icon = {
                        Icon(
                            RoundedRectanglePainter(),
                            s[Str.Icon],
                            Modifier.padding(end = 12.dp),
                            Color(0xFF36D167),
                        )
                    },
                    text = s[Str.Option2],
                    summary = s[Str.Green],
                ),
                DropdownItem(
                    icon = {
                        Icon(
                            RoundedRectanglePainter(),
                            s[Str.Icon],
                            Modifier.padding(end = 12.dp),
                            Color(0xFF3482FF),
                        )
                    },
                    text = s[Str.Option3],
                    summary = s[Str.Blue],
                ),
                DropdownItem(
                    icon = {
                        Icon(
                            RoundedRectanglePainter(),
                            s[Str.Icon],
                            Modifier.padding(end = 12.dp),
                            Color(0xFFFFB21D),
                        )
                    },
                    text = s[Str.Option4],
                    summary = s[Str.Yellow],
                ),
            )
        }
        val overlayGroupedSpinnerOptions = remember(
            s,
            spinnerOptions,
            overlayGroup1SpinnerOptionSelected,
            overlayGroup2SpinnerOptionSelected,
            overlayGroup3SpinnerOptionSelected,
        ) {
            groupedSpinnerOptions(
                s = s,
                spinnerOptions = spinnerOptions,
                group1SelectedIndex = overlayGroup1SpinnerOptionSelected,
                onGroup1SelectedIndexChange = { overlayGroup1SpinnerOptionSelected = it },
                group2SelectedIndex = overlayGroup2SpinnerOptionSelected,
                onGroup2SelectedIndexChange = { overlayGroup2SpinnerOptionSelected = it },
                group3SelectedIndex = overlayGroup3SpinnerOptionSelected,
                onGroup3SelectedIndexChange = { overlayGroup3SpinnerOptionSelected = it },
            )
        }
        val windowGroupedSpinnerOptions = remember(
            s,
            spinnerOptions,
            windowGroup1SpinnerOptionSelected,
            windowGroup2SpinnerOptionSelected,
            windowGroup3SpinnerOptionSelected,
        ) {
            groupedSpinnerOptions(
                s = s,
                spinnerOptions = spinnerOptions,
                group1SelectedIndex = windowGroup1SpinnerOptionSelected,
                onGroup1SelectedIndexChange = { windowGroup1SpinnerOptionSelected = it },
                group2SelectedIndex = windowGroup2SpinnerOptionSelected,
                onGroup2SelectedIndexChange = { windowGroup2SpinnerOptionSelected = it },
                group3SelectedIndex = windowGroup3SpinnerOptionSelected,
                onGroup3SelectedIndexChange = { windowGroup3SpinnerOptionSelected = it },
            )
        }
        val overlayGroupedDialogSpinnerOptions = remember(
            s,
            spinnerOptions,
            overlayDialogGroup1SpinnerOptionSelected,
            overlayDialogGroup2SpinnerOptionSelected,
            overlayDialogGroup3SpinnerOptionSelected,
        ) {
            groupedSpinnerOptions(
                s = s,
                spinnerOptions = spinnerOptions,
                group1SelectedIndex = overlayDialogGroup1SpinnerOptionSelected,
                onGroup1SelectedIndexChange = { overlayDialogGroup1SpinnerOptionSelected = it },
                group2SelectedIndex = overlayDialogGroup2SpinnerOptionSelected,
                onGroup2SelectedIndexChange = { overlayDialogGroup2SpinnerOptionSelected = it },
                group3SelectedIndex = overlayDialogGroup3SpinnerOptionSelected,
                onGroup3SelectedIndexChange = { overlayDialogGroup3SpinnerOptionSelected = it },
            )
        }
        val windowGroupedDialogSpinnerOptions = remember(
            s,
            spinnerOptions,
            windowDialogGroup1SpinnerOptionSelected,
            windowDialogGroup2SpinnerOptionSelected,
            windowDialogGroup3SpinnerOptionSelected,
        ) {
            groupedSpinnerOptions(
                s = s,
                spinnerOptions = spinnerOptions,
                group1SelectedIndex = windowDialogGroup1SpinnerOptionSelected,
                onGroup1SelectedIndexChange = { windowDialogGroup1SpinnerOptionSelected = it },
                group2SelectedIndex = windowDialogGroup2SpinnerOptionSelected,
                onGroup2SelectedIndexChange = { windowDialogGroup2SpinnerOptionSelected = it },
                group3SelectedIndex = windowDialogGroup3SpinnerOptionSelected,
                onGroup3SelectedIndexChange = { windowDialogGroup3SpinnerOptionSelected = it },
            )
        }

        SmallTitle(text = s[Str.Spinner])
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        ) {
            OverlaySpinnerPreference(
                title = s[Str.SpinnerPrefO],
                summary = if (overlayExpanded) s[Str.Expanded] else s[Str.Collapsed],
                items = spinnerOptions,
                selectedIndex = superSpinnerOptionSelected.value,
                onSelectedIndexChange = { newOption -> superSpinnerOptionSelected.value = newOption },
                onExpandedChange = { overlayExpanded = it },
            )
            WindowSpinnerPreference(
                title = s[Str.SpinnerPrefW],
                summary = if (windowExpanded) s[Str.Expanded] else s[Str.Collapsed],
                items = spinnerOptions,
                selectedIndex = windowSpinnerOptionSelected.value,
                onSelectedIndexChange = { newOption -> windowSpinnerOptionSelected.value = newOption },
                onExpandedChange = { windowExpanded = it },
            )
            OverlaySpinnerPreference(
                title = s[Str.SpinnerPrefO],
                summary = s[Str.AsDialogO] + if (overlayDialogExpanded) s[Str.SpinnerSuffixExpanded] else s[Str.SpinnerSuffixCollapsed],
                dialogButtonString = s[Str.OK],
                items = spinnerOptions,
                selectedIndex = superSpinnerOptionSelectedDialog.value,
                onSelectedIndexChange = { newOption -> superSpinnerOptionSelectedDialog.value = newOption },
                onExpandedChange = { overlayDialogExpanded = it },
            )
            WindowSpinnerPreference(
                title = s[Str.SpinnerPrefW],
                summary = s[Str.AsDialogW] + if (windowDialogExpanded) s[Str.SpinnerSuffixExpanded] else s[Str.SpinnerSuffixCollapsed],
                dialogButtonString = s[Str.OK],
                items = spinnerOptions,
                selectedIndex = windowSpinnerOptionSelectedDialog.value,
                onSelectedIndexChange = { newOption -> windowSpinnerOptionSelectedDialog.value = newOption },
                onExpandedChange = { windowDialogExpanded = it },
            )
            OverlaySpinnerPreference(
                title = s[Str.GroupedSpinnerPrefO],
                summary = if (overlayGroupedExpanded) s[Str.Expanded] else s[Str.Collapsed],
                entries = overlayGroupedSpinnerOptions,
                collapseOnSelection = false,
                onExpandedChange = { overlayGroupedExpanded = it },
            )
            WindowSpinnerPreference(
                title = s[Str.GroupedSpinnerPrefW],
                summary = if (windowGroupedExpanded) s[Str.Expanded] else s[Str.Collapsed],
                entries = windowGroupedSpinnerOptions,
                collapseOnSelection = false,
                onExpandedChange = { windowGroupedExpanded = it },
            )
            OverlaySpinnerPreference(
                title = s[Str.GroupedSpinnerPrefO],
                summary = s[Str.AsDialogO] + if (overlayGroupedDialogExpanded) s[Str.SpinnerSuffixExpanded] else s[Str.SpinnerSuffixCollapsed],
                dialogButtonString = s[Str.OK],
                entries = overlayGroupedDialogSpinnerOptions,
                onExpandedChange = { overlayGroupedDialogExpanded = it },
            )
            WindowSpinnerPreference(
                title = s[Str.GroupedSpinnerPrefW],
                summary = s[Str.AsDialogW] + if (windowGroupedDialogExpanded) s[Str.SpinnerSuffixExpanded] else s[Str.SpinnerSuffixCollapsed],
                dialogButtonString = s[Str.OK],
                entries = windowGroupedDialogSpinnerOptions,
                onExpandedChange = { windowGroupedDialogExpanded = it },
            )
            OverlaySpinnerPreference(
                title = s[Str.DisabledSpinnerPrefO],
                summary = s[Str.Collapsed],
                items = listOf(DropdownItem(text = s[Str.Option5])),
                selectedIndex = 0,
                onSelectedIndexChange = {},
                enabled = false,
            )
            WindowSpinnerPreference(
                title = s[Str.DisabledSpinnerPrefW],
                summary = s[Str.Collapsed],
                items = listOf(DropdownItem(text = s[Str.Option6])),
                selectedIndex = 0,
                onSelectedIndexChange = {},
                enabled = false,
            )
        }
    }
}

private fun groupedSpinnerOptions(
    s: Strings,
    spinnerOptions: List<DropdownItem>,
    group1SelectedIndex: Int,
    onGroup1SelectedIndexChange: (Int) -> Unit,
    group2SelectedIndex: Int,
    onGroup2SelectedIndexChange: (Int) -> Unit,
    group3SelectedIndex: Int,
    onGroup3SelectedIndexChange: (Int) -> Unit,
): List<DropdownEntry> = listOf(
    DropdownEntry(
        items = spinnerOptions.take(2).mapIndexed { index, item ->
            item.copy(
                selected = group1SelectedIndex == index,
                onClick = {
                    onGroup1SelectedIndexChange(index)
                    item.onClick?.invoke()
                },
            )
        },
    ),
    DropdownEntry(
        items = spinnerOptions.drop(2).mapIndexed { index, item ->
            item.copy(
                selected = group2SelectedIndex == index,
                onClick = {
                    onGroup2SelectedIndexChange(index)
                    item.onClick?.invoke()
                },
            )
        },
    ),
    DropdownEntry(
        items = spinnerOptions.mapIndexed { index, item ->
            item.copy(
                text = s.format(Str.OptionIndex, index + 1),
                enabled = index % 2 == 0,
                selected = group3SelectedIndex == index,
                onClick = {
                    onGroup3SelectedIndexChange(index)
                    item.onClick?.invoke()
                },
            )
        },
    ),
)

private class RoundedRectanglePainter(
    private val cornerRadius: Dp = 6.dp,
) : Painter() {
    override val intrinsicSize = Size.Unspecified

    override fun DrawScope.onDraw() {
        drawRoundRect(
            color = Color.White,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        )
    }
}
