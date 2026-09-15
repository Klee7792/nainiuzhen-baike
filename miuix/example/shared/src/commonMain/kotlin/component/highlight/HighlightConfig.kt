// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package component.highlight

import i18n.Str
import top.yukonga.miuix.kmp.blur.highlight.Highlight

internal object HighlightConfig {

    enum class Container(val displayName: Str) {
        Disabled(Str.Disabled),
        Large(Str.LargeContainer),
        Medium(Str.MediumContainer),
        Small(Str.SmallContainer),
    }

    fun get(container: Container, isDark: Boolean): Highlight? = when (container) {
        Container.Disabled -> null
        Container.Large -> if (!isDark) Highlight.GlassStrokeBigLight else Highlight.GlassStrokeBigDark
        Container.Medium -> if (!isDark) Highlight.GlassStrokeMiddleLight else Highlight.GlassStrokeMiddleDark
        Container.Small -> if (!isDark) Highlight.GlassStrokeSmallLight else Highlight.GlassStrokeSmallDark
    }
}
