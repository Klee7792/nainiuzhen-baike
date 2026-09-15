// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import i18n.LocalStrings
import i18n.Str
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

fun LazyListScope.cardSection() {
    item(key = "card") {
        val s = LocalStrings.current
        SmallTitle(text = s[Str.Card])
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            colors = CardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.primaryVariant,
            ),
            insideMargin = PaddingValues(16.dp),
            pressFeedbackType = PressFeedbackType.None,
            showIndication = true,
        ) {
            Text(
                color = MiuixTheme.colorScheme.onPrimaryVariant,
                text = s[Str.Card],
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                color = MiuixTheme.colorScheme.onPrimaryVariant,
                text = s[Str.ShowIndicationTrue],
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier.weight(1f),
                insideMargin = PaddingValues(16.dp),
                pressFeedbackType = PressFeedbackType.Sink,
                onClick = { println(s[Str.CardClick]) },
                content = {
                    Text(
                        color = MiuixTheme.colorScheme.onSurface,
                        text = s[Str.Card],
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        text = s[Str.PressFeedbackNTypeSink],
                        style = MiuixTheme.textStyles.paragraph,
                    )
                },
            )
            Card(
                modifier = Modifier.weight(1f),
                insideMargin = PaddingValues(16.dp),
                pressFeedbackType = PressFeedbackType.Tilt,
                onLongPress = { println(s[Str.CardLongPress]) },
                content = {
                    Text(
                        color = MiuixTheme.colorScheme.onSurface,
                        text = s[Str.Card],
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        text = s[Str.PressFeedbackNTypeTilt],
                        style = MiuixTheme.textStyles.paragraph,
                    )
                },
            )
        }
        LongPressHoldDownCardDemo()
    }
}

@Composable
private fun LongPressHoldDownCardDemo() {
    val s = LocalStrings.current
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var holdDown by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(16.dp),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        holdDownState = holdDown,
        onLongPress = {
            showDialog = true
            holdDown = true
        },
        content = {
            Text(
                color = MiuixTheme.colorScheme.onSurface,
                text = s[Str.Card],
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                text = s[Str.LongPressToShowDialog],
                style = MiuixTheme.textStyles.paragraph,
            )
        },
    )

    OverlayDialog(
        show = showDialog,
        title = s[Str.LongPressAction],
        summary = s[Str.TriggeredByLongPressingTheCard],
        onDismissRequest = { showDialog = false },
        onDismissFinished = { holdDown = false },
        content = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    text = s[Str.Cancel],
                    onClick = { showDialog = false },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = s[Str.Confirm],
                    onClick = { showDialog = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        },
    )
}
