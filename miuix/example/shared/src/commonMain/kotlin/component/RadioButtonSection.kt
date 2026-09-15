// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.LocalStrings
import i18n.Str
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.RadioButtonPreference

fun LazyListScope.radioButtonSection() {
    item(key = "radioButton") {
        val s = LocalStrings.current
        SmallTitle(text = s[Str.RadioButton])
        RadioButtonCardsDemo()
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        ) {
            RadioButtonPreference(
                title = s[Str.DisabledRadioButton],
                summary = s[Str.ThisOptionIsUnavailable],
                selected = true,
                enabled = false,
                onClick = {},
            )
        }
    }
}

@Composable
private fun RadioButtonCardsDemo() {
    val s = LocalStrings.current
    var selectedIndex by remember { mutableIntStateOf(0) }

    listOf(s[Str.OptionA], s[Str.OptionB], s[Str.OptionC]).forEachIndexed { index, title ->
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        ) {
            RadioButtonPreference(
                title = title,
                summary = s.format(Str.SelectedState, selectedIndex == index),
                selected = selectedIndex == index,
                onClick = { selectedIndex = index },
            )
        }
    }
}
