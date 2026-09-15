// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package i18n

/**
 * The language mode selected in Settings.
 *
 * [System] follows the device language: Chinese when the system language is Chinese, English
 * otherwise. The index of the enum entry is what [AppState.language] stores.
 */
enum class AppLanguage {
    System,
    Chinese,
    English,
    ;

    companion object {
        /** The default mode: follow the system language. */
        val Default = System

        fun fromIndex(index: Int): AppLanguage = entries.getOrElse(index) { Default }
    }
}
