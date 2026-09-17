// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package i18n

import platform.Foundation.NSUserDefaults

/**
 * iOS/macOS: the user's preferred languages come from the `AppleLanguages` defaults key,
 * which is exactly what `NSLocale.preferredLanguages` reads underneath
 * (accessed via NSUserDefaults because NSLocale class members are not
 * resolvable on Kotlin/Native 2.4.20 bindings).
 */
actual fun systemLanguageTag(): String =
    NSUserDefaults.standardUserDefaults
        .stringArrayForKey("AppleLanguages")
        ?.firstOrNull() as? String
        ?: "en"
