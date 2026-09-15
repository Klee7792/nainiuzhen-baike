// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package i18n

/**
 * The BCP 47 language tag of the device, for example `zh`, `zh-Hans-CN`, `en-US`.
 *
 * Only the language part is used by [systemLanguageIsChinese], so region and script are ignored.
 */
expect fun systemLanguageTag(): String

/** True when the device language is Chinese (any region/script). */
fun systemLanguageIsChinese(): Boolean = systemLanguageTag().startsWith("zh", ignoreCase = true)

/**
 * Resolves [language] to the actual text language: [AppLanguage.System] is replaced by the device
 * language and everything that is not Chinese falls back to English.
 */
fun resolveIsChinese(language: AppLanguage): Boolean = when (language) {
    AppLanguage.System -> systemLanguageIsChinese()
    AppLanguage.Chinese -> true
    AppLanguage.English -> false
}
