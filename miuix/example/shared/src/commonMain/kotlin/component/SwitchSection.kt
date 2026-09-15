// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.LocalStrings
import i18n.Str
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

fun LazyListScope.switchSection() {
    item(key = "switch") {
        val s = LocalStrings.current
        val switch = remember { mutableStateOf(false) }
        val switchTrue = remember { mutableStateOf(true) }
        val superSwitch = remember(s) { mutableStateOf(s[Str.False]) }
        val superSwitchState = remember { mutableStateOf(false) }
        val superSwitchAnimState = remember { mutableStateOf(false) }

        SmallTitle(text = s[Str.Switch])
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Switch(
                    checked = switch.value,
                    onCheckedChange = { switch.value = it },
                )
                Switch(
                    checked = switchTrue.value,
                    onCheckedChange = { switchTrue.value = it },
                    modifier = Modifier.padding(start = 6.dp),
                )
                Switch(
                    checked = false,
                    onCheckedChange = { },
                    modifier = Modifier.padding(start = 6.dp),
                    enabled = false,
                )
                Switch(
                    checked = true,
                    onCheckedChange = { },
                    modifier = Modifier.padding(start = 6.dp),
                    enabled = false,
                )
            }
            SwitchPreference(
                title = s[Str.Switch],
                summary = s[Str.ClickToExpandASwitch],
                checked = superSwitchAnimState.value,
                onCheckedChange = {
                    superSwitchAnimState.value = it
                },
            )

            AnimatedVisibility(
                visible = superSwitchAnimState.value,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                SwitchPreference(
                    title = s[Str.Switch],
                    checked = superSwitchState.value,
                    endActions = {
                        Text(
                            text = superSwitch.value,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    },
                    onCheckedChange = {
                        superSwitchState.value = it
                        superSwitch.value = "$it"
                    },
                )
            }
            SwitchPreference(
                title = s[Str.DisabledSwitch],
                checked = true,
                enabled = false,
                onCheckedChange = {},
            )
        }
    }
}
