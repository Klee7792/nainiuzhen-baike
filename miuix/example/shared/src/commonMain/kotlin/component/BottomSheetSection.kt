// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import i18n.LocalStrings
import i18n.Str
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowBottomSheet

private val BottomSheetDropdownOptionKeys = listOf(Str.Option1, Str.Option2)

fun LazyListScope.bottomSheetSection() {
    item(key = "bottomSheet") {
        val s = LocalStrings.current
        var showSuperBottomSheet by remember { mutableStateOf(false) }
        var showWindowBottomSheet by remember { mutableStateOf(false) }
        var superBottomSheetHoldDown by remember { mutableStateOf(false) }
        var windowBottomSheetHoldDown by remember { mutableStateOf(false) }
        var bottomSheetDropdownSelectedOption by remember { mutableIntStateOf(0) }
        var bottomSheetSuperSwitchState by remember { mutableStateOf(true) }

        SmallTitle(text = s[Str.BottomSheet])
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        ) {
            ArrowPreference(
                title = s[Str.BottomSheetO],
                summary = s[Str.ClickToShowAnOverlayBottomSheet],
                onClick = {
                    showSuperBottomSheet = true
                    superBottomSheetHoldDown = true
                },
                holdDownState = superBottomSheetHoldDown,
            )
            ArrowPreference(
                title = s[Str.BottomSheetW],
                summary = s[Str.ClickToShowAWindowBottomSheet],
                onClick = {
                    showWindowBottomSheet = true
                    windowBottomSheetHoldDown = true
                },
                holdDownState = windowBottomSheetHoldDown,
            )
        }

        SuperBottomSheetDemo(
            show = showSuperBottomSheet,
            onDismissRequest = { showSuperBottomSheet = false },
            dropdownSelectedIndex = bottomSheetDropdownSelectedOption,
            onDropdownSelectedIndexChange = { bottomSheetDropdownSelectedOption = it },
            switchChecked = bottomSheetSuperSwitchState,
            onSwitchCheckedChange = { bottomSheetSuperSwitchState = it },
            onDismissFinished = { superBottomSheetHoldDown = false },
        )
        WindowBottomSheetDemo(
            show = showWindowBottomSheet,
            onDismissRequest = { showWindowBottomSheet = false },
            dropdownSelectedIndex = bottomSheetDropdownSelectedOption,
            onDropdownSelectedIndexChange = { bottomSheetDropdownSelectedOption = it },
            switchChecked = bottomSheetSuperSwitchState,
            onSwitchCheckedChange = { bottomSheetSuperSwitchState = it },
            onDismissFinished = { windowBottomSheetHoldDown = false },
        )
    }
}

@Composable
private fun SuperBottomSheetDemo(
    show: Boolean,
    onDismissRequest: () -> Unit,
    dropdownSelectedIndex: Int,
    onDropdownSelectedIndexChange: (Int) -> Unit,
    switchChecked: Boolean,
    onSwitchCheckedChange: (Boolean) -> Unit,
    onDismissFinished: () -> Unit,
) {
    val s = LocalStrings.current
    var allowDismiss by remember { mutableStateOf(true) }
    var enableNestedScroll by remember { mutableStateOf(true) }

    OverlayBottomSheet(
        title = s[Str.BottomSheetO],
        show = show,
        allowDismiss = allowDismiss,
        enableNestedScroll = enableNestedScroll,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        startAction = {
            BottomSheetActionButton(MiuixIcons.Close, s[Str.Cancel], onClick = onDismissRequest)
        },
        endAction = {
            BottomSheetActionButton(MiuixIcons.Ok, s[Str.Confirm], onClick = onDismissRequest)
        },
    ) {
        BottomSheetContent(
            allowDismiss = allowDismiss,
            onAllowDismissChange = { allowDismiss = it },
            enableNestedScroll = enableNestedScroll,
            onEnableNestedScrollChange = { enableNestedScroll = it },
            switchChecked = switchChecked,
            onSwitchCheckedChange = onSwitchCheckedChange,
        ) {
            OverlayDropdownPreference(
                title = s[Str.DropdownPrefO],
                items = BottomSheetDropdownOptionKeys.map { s[it] },
                selectedIndex = dropdownSelectedIndex,
                onSelectedIndexChange = onDropdownSelectedIndexChange,
            )
        }
    }
}

@Composable
private fun WindowBottomSheetDemo(
    show: Boolean,
    onDismissRequest: () -> Unit,
    dropdownSelectedIndex: Int,
    onDropdownSelectedIndexChange: (Int) -> Unit,
    switchChecked: Boolean,
    onSwitchCheckedChange: (Boolean) -> Unit,
    onDismissFinished: () -> Unit,
) {
    val s = LocalStrings.current
    var allowDismiss by remember { mutableStateOf(true) }
    var enableNestedScroll by remember { mutableStateOf(true) }

    WindowBottomSheet(
        title = s[Str.BottomSheetW],
        show = show,
        allowDismiss = allowDismiss,
        enableNestedScroll = enableNestedScroll,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        startAction = {
            val dismissState = LocalDismissState.current
            BottomSheetActionButton(MiuixIcons.Close, s[Str.Cancel], onClick = { dismissState?.invoke() })
        },
        endAction = {
            val dismissState = LocalDismissState.current
            BottomSheetActionButton(MiuixIcons.Ok, s[Str.Confirm], onClick = { dismissState?.invoke() })
        },
    ) {
        BottomSheetContent(
            allowDismiss = allowDismiss,
            onAllowDismissChange = { allowDismiss = it },
            enableNestedScroll = enableNestedScroll,
            onEnableNestedScrollChange = { enableNestedScroll = it },
            switchChecked = switchChecked,
            onSwitchCheckedChange = onSwitchCheckedChange,
        ) {
            WindowDropdownPreference(
                title = s[Str.DropdownPrefW],
                items = BottomSheetDropdownOptionKeys.map { s[it] },
                selectedIndex = dropdownSelectedIndex,
                onSelectedIndexChange = onDropdownSelectedIndexChange,
            )
        }
    }
}

/**
 * Shared content of both BottomSheet demos. The only part that differs between the Overlay and
 * Window variants is the dropdown preference, supplied through the [dropdown] slot.
 */
@Composable
private fun BottomSheetContent(
    allowDismiss: Boolean,
    onAllowDismissChange: (Boolean) -> Unit,
    enableNestedScroll: Boolean,
    onEnableNestedScrollChange: (Boolean) -> Unit,
    switchChecked: Boolean,
    onSwitchCheckedChange: (Boolean) -> Unit,
    dropdown: @Composable () -> Unit,
) {
    val s = LocalStrings.current
    LazyColumn(
        modifier = Modifier.fillMaxWidth()
            .scrollEndHaptic()
            .overScrollVertical(),
    ) {
        item {
            SmallTitle(
                text = s[Str.BehaviorSettings],
                insideMargin = PaddingValues(16.dp, 8.dp),
            )
            Card(
                modifier = Modifier.padding(bottom = 12.dp),
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.secondaryContainer,
                ),
            ) {
                SwitchPreference(
                    title = s[Str.AllowDismiss],
                    summary = s[Str.DragOrBackToDismiss],
                    checked = allowDismiss,
                    onCheckedChange = onAllowDismissChange,
                )
                SwitchPreference(
                    title = s[Str.EnableNestedScroll],
                    summary = s[Str.ScrollContentVsDragSheet],
                    checked = enableNestedScroll,
                    onCheckedChange = onEnableNestedScrollChange,
                )
            }
        }
        item {
            var textFieldValue by remember { mutableStateOf("") }
            TextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                label = s[Str.TextField],
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Card(
                modifier = Modifier.padding(bottom = 12.dp),
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.secondaryContainer,
                ),
            ) {
                dropdown()
                SwitchPreference(
                    title = s[Str.SwitchPref],
                    checked = switchChecked,
                    onCheckedChange = onSwitchCheckedChange,
                )
            }
            Spacer(
                Modifier.padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                        WindowInsets.captionBar.asPaddingValues().calculateBottomPadding(),
                ),
            )
        }
    }
}

@Composable
private fun BottomSheetActionButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = MiuixTheme.colorScheme.onBackground,
        )
    }
}
