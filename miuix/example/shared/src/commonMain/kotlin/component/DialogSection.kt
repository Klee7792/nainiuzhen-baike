// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import i18n.LocalStrings
import i18n.Str
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.VerticalDivider
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

fun LazyListScope.dialogSection() {
    item(key = "dialog") {
        val s = LocalStrings.current
        var showOverlayDialog by rememberSaveable { mutableStateOf(false) }
        var showWindowDialog by rememberSaveable { mutableStateOf(false) }
        var overlayDialogHoldDown by rememberSaveable { mutableStateOf(false) }
        var windowDialogHoldDown by rememberSaveable { mutableStateOf(false) }
        var showWideSuperDialog by rememberSaveable { mutableStateOf(false) }
        var showWideWindowDialog by rememberSaveable { mutableStateOf(false) }
        var wideSuperDialogHoldDown by rememberSaveable { mutableStateOf(false) }
        var wideWindowDialogHoldDown by rememberSaveable { mutableStateOf(false) }
        var showCenteredDialog by rememberSaveable { mutableStateOf(false) }
        var centeredDialogHoldDown by rememberSaveable { mutableStateOf(false) }

        SmallTitle(text = s[Str.Dialog])
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        ) {
            ArrowPreference(
                title = s[Str.DialogO],
                summary = s[Str.ClickToShowAnOverlayDialog],
                onClick = {
                    showOverlayDialog = true
                    overlayDialogHoldDown = true
                },
                holdDownState = overlayDialogHoldDown,
            )
            ArrowPreference(
                title = s[Str.DialogW],
                summary = s[Str.ClickToShowAWindowDialog],
                onClick = {
                    showWindowDialog = true
                    windowDialogHoldDown = true
                },
                holdDownState = windowDialogHoldDown,
            )
            ArrowPreference(
                title = s[Str.WideDialogO],
                summary = s[Str.PortraitShowsARegularDialogLandscapeShowsATw],
                onClick = {
                    showWideSuperDialog = true
                    wideSuperDialogHoldDown = true
                },
                holdDownState = wideSuperDialogHoldDown,
            )
            ArrowPreference(
                title = s[Str.WideDialogW],
                summary = s[Str.PortraitShowsARegularDialogLandscapeShowsATw],
                onClick = {
                    showWideWindowDialog = true
                    wideWindowDialogHoldDown = true
                },
                holdDownState = wideWindowDialogHoldDown,
            )
            ArrowPreference(
                title = s[Str.CenteredDialogO],
                summary = s[Str.ForceTheLargeScreenPresentationWithLargeScre],
                onClick = {
                    showCenteredDialog = true
                    centeredDialogHoldDown = true
                },
                holdDownState = centeredDialogHoldDown,
            )
        }

        OverlayDialogDemo(
            show = showOverlayDialog,
            onDismissRequest = { showOverlayDialog = false },
            onDismissFinished = { overlayDialogHoldDown = false },
        )
        WindowDialogDemo(
            show = showWindowDialog,
            onDismissRequest = { showWindowDialog = false },
            onDismissFinished = { windowDialogHoldDown = false },
        )
        WideSuperDialogDemo(
            show = showWideSuperDialog,
            onDismissRequest = { showWideSuperDialog = false },
            onDismissFinished = { wideSuperDialogHoldDown = false },
        )
        WideWindowDialogDemo(
            show = showWideWindowDialog,
            onDismissRequest = { showWideWindowDialog = false },
            onDismissFinished = { wideWindowDialogHoldDown = false },
        )
        CenteredOverlayDialogDemo(
            show = showCenteredDialog,
            onDismissRequest = { showCenteredDialog = false },
            onDismissFinished = { centeredDialogHoldDown = false },
        )
    }
}

@Composable
private fun OverlayDialogDemo(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    val s = LocalStrings.current
    OverlayDialog(
        show = show,
        title = s[Str.DialogO],
        summary = s[Str.ADialogComponentInsideMiuixPopupHost],
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        content = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    text = s[Str.Cancel],
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = s[Str.Confirm],
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        },
    )
}

@Composable
private fun WindowDialogDemo(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    val s = LocalStrings.current
    WindowDialog(
        show = show,
        title = s[Str.DialogW],
        summary = s[Str.AWindowLevelDialogNoMiuixPopupHostRequired],
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        content = {
            val dismissState = LocalDismissState.current
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    text = s[Str.Cancel],
                    onClick = { dismissState?.invoke() },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = s[Str.Confirm],
                    onClick = { dismissState?.invoke() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        },
    )
}

@Composable
private fun CenteredOverlayDialogDemo(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    val s = LocalStrings.current
    OverlayDialog(
        show = show,
        title = s[Str.CenteredDialog],
        summary = s[Str.LargeScreenTrueForcesTheCenteredPresentation],
        largeScreen = true,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        content = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    text = s[Str.Cancel],
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = s[Str.Confirm],
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        },
    )
}

@Composable
private fun WideSuperDialogDemo(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    val s = LocalStrings.current
    val windowSize = LocalWindowInfo.current.containerDpSize
    val isLandscape = windowSize.width > windowSize.height

    OverlayDialog(
        show = show,
        title = if (isLandscape) null else s[Str.WideDialog],
        summary = if (isLandscape) null else s[Str.RotateToLandscapeToSeeTheEffect],
        maxWidth = if (isLandscape) 560.dp else DialogDefaults.MaxWidth,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        content = {
            WideDialogContent(isLandscape = isLandscape)
        },
    )
}

@Composable
private fun WideWindowDialogDemo(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    val s = LocalStrings.current
    val windowSize = LocalWindowInfo.current.containerDpSize
    val isLandscape = windowSize.width > windowSize.height

    WindowDialog(
        show = show,
        title = if (isLandscape) null else s[Str.WideDialog],
        summary = if (isLandscape) null else s[Str.RotateToLandscapeToSeeTheEffect],
        maxWidth = if (isLandscape) 560.dp else DialogDefaults.MaxWidth,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        content = {
            WideDialogContent(isLandscape = isLandscape)
        },
    )
}

@Composable
private fun WideDialogContent(
    isLandscape: Boolean,
) {
    val s = LocalStrings.current
    val dismissState = LocalDismissState.current

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = s[Str.WideDialog],
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = s[Str.RotateToLandscapeToSeeTheEffect],
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    textAlign = TextAlign.Center,
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 20.dp),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(
                    space = 12.dp,
                    alignment = Alignment.CenterVertically,
                ),
            ) {
                TextButton(
                    text = s[Str.AllowOnce],
                    onClick = { dismissState?.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
                TextButton(
                    text = s[Str.AlwaysAllow],
                    onClick = { dismissState?.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    text = s[Str.Deny],
                    onClick = { dismissState?.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = s[Str.AllowOnce],
                onClick = { dismissState?.invoke() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
            TextButton(
                text = s[Str.AlwaysAllow],
                onClick = { dismissState?.invoke() },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                text = s[Str.Deny],
                onClick = { dismissState?.invoke() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
