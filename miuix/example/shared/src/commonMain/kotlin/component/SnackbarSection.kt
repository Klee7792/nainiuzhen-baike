// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.LocalStrings
import i18n.Str
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarDuration
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.SnackbarResult
import top.yukonga.miuix.kmp.basic.TextButton

fun LazyListScope.snackbarSection(snackbarHostState: SnackbarHostState) {
    item(key = "snackbar") {
        val s = LocalStrings.current
        SmallTitle(text = s[Str.Snackbar])
        val scope = rememberCoroutineScope()
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        text = s[Str.DismissOldest],
                        onClick = {
                            scope.launch {
                                snackbarHostState.oldestSnackbarData()?.dismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = s[Str.DismissNewest],
                        onClick = {
                            scope.launch {
                                snackbarHostState.newestSnackbarData()?.dismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        text = s[Str.Short4s],
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(s[Str.ThisMessageStaysFor4Seconds])
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = s[Str.Long10s],
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = s[Str.ThisIsALongerMessageThatStaysFor10Seconds],
                                    duration = SnackbarDuration.Long,
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        text = s[Str.Custom2s],
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = s[Str.ThisMessageUsesACustom2SecondDuration],
                                    duration = SnackbarDuration.Custom(2000L),
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    var text by remember(s) { mutableStateOf(s[Str.Action]) }
                    TextButton(
                        text = text,
                        onClick = {
                            scope.launch {
                                text = s[Str.ActionAlive]
                                val result = snackbarHostState.showSnackbar(
                                    message = s[Str.ThisMessageHasAnActionButton],
                                    actionLabel = s[Str.Undo],
                                    duration = SnackbarDuration.Short,
                                )
                                text = when (result) {
                                    SnackbarResult.ActionPerformed -> s[Str.ActionUndo]
                                    else -> s[Str.ActionExpired]
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        text = s[Str.Dismissible],
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = s[Str.TapTheCloseButtonToDismissThisMessage],
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Long,
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = s[Str.Indefinite],
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = s[Str.ThisMessageStaysUntilYouDismissItManually],
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Indefinite,
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        text = s[Str.ActionClose],
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = s[Str.ThisMessageHasBothAnActionAndACloseButton],
                                    actionLabel = s[Str.Undo],
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Long,
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
