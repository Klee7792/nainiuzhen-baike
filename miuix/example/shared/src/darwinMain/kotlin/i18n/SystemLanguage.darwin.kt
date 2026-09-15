// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package i18n

import platform.Foundation.NSLocale

/** iOS/macOS expose the user's preferred languages through `NSLocale.preferredLanguages` (Obj-C class method → Companion in Kotlin/Native). */
actual fun systemLanguageTag(): String =
    NSLocale.Companion.preferredLanguages.firstOrNull() ?: "en"
