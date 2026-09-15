// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package i18n

/**
 * Web has no common way to read `navigator.language`: the JS and Wasm targets need different
 * interop APIs, so there is no single implementation that compiles for both. Fall back to English,
 * which is the documented default of the language setting; picking "Chinese" in Settings still
 * works on web.
 */
actual fun systemLanguageTag(): String = "en"
