// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package i18n

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Resolves [Str] entries for the language that is currently active.
 *
 * Read it once at the top of a composable (`val s = LocalStrings.current`) and use it anywhere in
 * that scope, including inside `remember` blocks and event lambdas - it is a plain object, not a
 * composable function.
 */
@Immutable
class Strings(
    val isChinese: Boolean,
) {
    /** The text of [key] in the active language. */
    operator fun get(key: Str): String = if (isChinese) key.zh else key.en

    /**
     * The text of [key] in the active language with its `{0}`, `{1}`, ... placeholders replaced by
     * [args]. `{n}` without a matching argument is left untouched, so a missing translation never
     * crashes the UI.
     */
    fun format(key: Str, vararg args: Any?): String = formatLiteral(get(key), args)
}

private fun formatLiteral(template: String, args: Array<out Any?>): String {
    if (args.isEmpty()) return template
    val out = StringBuilder(template.length + 16)
    var i = 0
    while (i < template.length) {
        val c = template[i]
        if (c == '{') {
            var j = i + 1
            var index = 0
            var digits = 0
            while (j < template.length && template[j].isDigit()) {
                index = index * 10 + (template[j] - '0')
                j++
                digits++
            }
            if (digits > 0 && j < template.length && template[j] == '}' && index < args.size) {
                out.append(args[index]?.toString() ?: "")
                i = j + 1
                continue
            }
        }
        out.append(c)
        i++
    }
    return out.toString()
}

/** The strings of the currently active language. Defaults to English outside of [App]. */
val LocalStrings = staticCompositionLocalOf { Strings(isChinese = false) }
