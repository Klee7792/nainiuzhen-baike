// 底栏玻璃模糊预设（抽取自 MainScreen 的两处内联 textureBlur 表达式）。
//
// 背景：iOS 上每个 (shader 程序 × 混合模式 × 目标格式) 的 GPU 管线**只在首次真正绘制时才编译**
//（数十~数百 ms），且缓存仅存在于进程内 —— 这正是「冷启动后越用越流畅、切 tab/开弹窗/拖底栏/
// 进子页全是第一次卡、杀进程重来又要重新开荒」的根因。模糊半径决定 tap 数与 downscale 档位，
// 半径不同就是**不同的 shader 程序**。
//
// ShaderWarmup 要在加载页把这些管线提前烤热，就必须**逐字复用**真实底栏所用的同一套参数
//（半径 / 形状 / 高光）；参数一旦各自复制粘贴就会漂移，预热到的是另一条管线，真机依旧首卡。
// 故把这两处表达式收敛到本文件，MainScreen 与 ShaderWarmup 共用同一实现。
//
// 本文件与 MainScreen 同包（ui.home），因此 MainScreen 无需新增 import，改动最小化。
package com.nainiuzhen.wiki.ui.home

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.textureBlur

/**
 * 悬浮底栏模糊半径（dp）。与 MainScreen 原内联值逐字一致：改这里即同时改动真实底栏与预热，
 * 二者永不失配。
 */
internal const val FLOATING_BAR_BLUR_RADIUS = 25f

/** 经典（贴边）底栏模糊半径（dp）。 */
internal const val BOTTOM_BAR_BLUR_RADIUS = 18f

/**
 * 悬浮底栏玻璃模糊：圆角矩形 + 边缘高光，半径 [FLOATING_BAR_BLUR_RADIUS]。
 *
 * [backdrop] 为 null（未启用模糊 / 运行环境不支持 RuntimeShader）时原样返回 [this]，
 * 语义等同原先的 `if (blurActive) textureBlur(...) else Modifier`。
 *
 * @param backdrop 采样背景层（真实底栏与预热盒共用同一个 remember 出来的层）。
 * @param shape 底栏圆角形状（`RoundedCornerShape(FloatingToolbarDefaults.CornerRadius)`）。
 * @param highlight 玻璃描边高光（深浅色不同）。
 * @param colors 叠在模糊之上的混色（由调用方用 `BlurDefaults.blurColors(...)` 构造并 remember）。
 */
internal fun Modifier.appFloatingBarBlur(
    backdrop: LayerBackdrop?,
    shape: Shape,
    highlight: Highlight,
    colors: BlurColors,
): Modifier {
    val bd = backdrop ?: return this
    return this.textureBlur(
        backdrop = bd,
        shape = shape,
        blurRadius = FLOATING_BAR_BLUR_RADIUS,
        colors = colors,
        highlight = highlight,
    )
}

/**
 * 经典（贴边）底栏玻璃模糊：矩形、无高光，半径 [BOTTOM_BAR_BLUR_RADIUS]。
 * [backdrop] 为 null 时原样返回 [this]。
 */
internal fun Modifier.appBottomBarBlur(backdrop: LayerBackdrop?): Modifier {
    val bd = backdrop ?: return this
    return this.textureBlur(
        backdrop = bd,
        shape = RectangleShape,
        blurRadius = BOTTOM_BAR_BLUR_RADIUS,
    )
}
