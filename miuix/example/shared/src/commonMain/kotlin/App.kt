// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.dp
import i18n.AppLanguage
import i18n.LocalStrings
import i18n.Strings
import i18n.resolveIsChinese
import kotlinx.coroutines.flow.drop
import top.yukonga.miuix.kmp.squircle.LocalSquircleEnabled
import ui.AppTheme
import ui.keyColorFor

@Composable
fun App(
    padding: PaddingValues = PaddingValues(0.dp),
    onColorModeChange: ((Int) -> Unit)? = null,
) {
    var appState by remember { mutableStateOf(AppState()) }
    val updateAppState: ((AppState) -> AppState) -> Unit = remember {
        { transform -> appState = transform(appState) }
    }

    val currentOnColorModeChange by rememberUpdatedState(onColorModeChange)
    LaunchedEffect(Unit) {
        snapshotFlow { appState.colorMode }
            .drop(1)
            .collect { currentOnColorModeChange?.invoke(it) }
    }

    // Follow the system language when the setting is "System"; anything that is not Chinese
    // falls back to English.
    val isChinese = remember(appState.language) {
        resolveIsChinese(AppLanguage.fromIndex(appState.language))
    }
    val strings = remember(isChinese) { Strings(isChinese) }

    val keyColor = keyColorFor(appState.seedIndex)

    AppTheme(
        colorMode = appState.colorMode,
        keyColor = keyColor,
        paletteStyle = appState.paletteStyle,
        colorSpec = appState.colorSpec,
    ) {
        CompositionLocalProvider(
            LocalAppState provides appState,
            LocalUpdateAppState provides updateAppState,
            LocalStrings provides strings,
            LocalSquircleEnabled provides appState.enableSquircle,
        ) {
            AppContent(padding = padding)
        }
    }
}
