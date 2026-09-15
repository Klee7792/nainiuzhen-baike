// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package i18n

/** Android reports the system language through the default [java.util.Locale]. */
actual fun systemLanguageTag(): String = java.util.Locale.getDefault().language
