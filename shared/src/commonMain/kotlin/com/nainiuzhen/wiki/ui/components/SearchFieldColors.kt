package com.nainiuzhen.wiki.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nainiuzhen.wiki.utils.LocalAppSettings
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldColors
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 顶栏模糊生效时，搜索框背景的透明度（越低越能透出顶栏的模糊）。 */
const val SEARCH_FIELD_BLUR_ALPHA = 0.45f

/**
 * 搜索框配色：顶栏模糊生效时改为半透明背景，让顶栏的模糊透出；模糊关闭或设备不支持时
 * 保持默认实心外观（否则会出现"透明背景 + 无模糊"导致的文字难读）。
 *
 * 三大板块（物品 / 配方 / NPC）**共用此实现**，以保证搜索框视觉一致——不要在各页面
 * 再各自写一份 `searchFieldColors()` 或直接用 `Color.Transparent`。
 *
 * 判定条件与顶栏取 backdrop 的条件严格一致，见 [rememberAppBlurBackdrop]
 * （`!enableBlur || !isRuntimeShaderSupported()` 时返回 null）。
 *
 * @param backdropGlass 是否由外层 [BlurredSearchField] 的独立高斯磨砂层（`Modifier.textureBlur`）
 *   提供背景。为 `true` 时本函数返回**完全透明**的输入框背景：磨砂层已经是不透底的模糊片，
 *   若再叠一层半透明底色，两层半透明会让整体重新变"通透"（用户反馈「通透度过高」的叠加来源），
 *   故此处必须交给外层磨砂层独占背景。默认 `false`，保证既有调用方行为**逐字节不变**。
 */
@Composable
fun searchFieldColors(backdropGlass: Boolean = false): TextFieldColors {
    val appState = LocalAppSettings.current
    val blurActive = appState.enableBlur && isRuntimeShaderSupported()
    val backgroundColor = MiuixTheme.colorScheme.secondaryContainer
    // 渐进模糊时，顶栏模糊在搜索栏（顶栏底部）处最弱，搜索框自身若太透明会"几乎全透明"；
    // 因此渐进模糊下提高搜索框底色不透明度，保证可读且看起来是磨砂实条（bug-v7 #8）。
    val progressive = appState.topAppBarBlurStyle == 1
    return TextFieldDefaults.textFieldColors(
        backgroundColor = when {
            // 独立磨砂层已铺在输入框下方 ⇒ 输入框自身背景必须完全透明，避免多层半透明叠加。
            backdropGlass -> Color.Transparent
            blurActive -> backgroundColor.copy(alpha = if (progressive) 0.7f else SEARCH_FIELD_BLUR_ALPHA)
            else -> backgroundColor
        },
    )
}

/**
 * 带**独立高斯模糊背景**的搜索框：三大板块（物品 / 配方 / NPC）列表页顶栏 `bottomContent` 共用。
 *
 * 背景：顶栏容器 [BlurredBar] 在「渐进模糊」模式下用 `ProgressiveBlur.Top`（顶部最强、向下渐隐），
 * 而搜索框位于顶栏最底部 ⇒ 拿到的模糊最弱，视觉上"通透度过高"。本组件在**渐进模糊 + 启用模糊 +
 * 环境支持 RuntimeShader** 时，额外复用顶栏同一个 [LayerBackdrop]（经 [LocalTopBarBackdrop] 透传）
 * 给搜索框铺一层独立 `textureBlur`，把通透度压下来。其余情形（均匀高斯 / 模糊关闭 / 环境不支持）
 * 不加层，视觉与改动前完全一致。
 *
 * 硬约束：`blurRadius` 固定 `25f`，与 [BlurredBar] 的均匀高斯同一半径，复用同一着色器程序，
 * 避免新增 program 触发首帧编译卡顿（着色器预热见 `ShaderWarmup`）。
 *
 * @param value 输入框当前值。
 * @param onValueChange 值变更回调。
 * @param label 输入框占位 / 标签文案。
 * @param modifier 外层 `Box` 的修饰符；默认已含 `horizontal = 12.dp, vertical = 4.dp` 边距，
 *   调用点**不要**再重复加 padding。
 */
@Composable
fun BlurredSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppSettings.current
    val progressive = appState.topAppBarBlurStyle == 1
    // 顶栏透出的 backdrop；子页面若自己 remember 会得到未挂载实例，故只在此读取。
    val bd = LocalTopBarBackdrop.current
    // 仅「渐进模糊 + 启用模糊 + 环境支持 + 顶栏确实透出了 backdrop」时才额外铺磨砂层：
    // 均匀高斯模式顶栏本就整条 25f 高斯，再叠一层既无视觉收益又浪费一次采样；
    // 模糊关闭 / 环境不支持时更不应出现"透明且无模糊"。
    val glassBackdrop = if (bd != null && appState.enableBlur && isRuntimeShaderSupported() && progressive) bd else null
    // 外层负责边距：padding 必须在模糊层**外面**，否则模糊矩形会盖到 4dp 竖直边距上。
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (glassBackdrop != null) {
                        Modifier.textureBlur(
                            backdrop = glassBackdrop,
                            shape = RoundedCornerShape(TextFieldDefaults.CornerRadius),
                            blurRadius = 25f,
                            noiseCoefficient = BlurDefaults.NoiseCoefficient,
                            colors = BlurDefaults.blurColors(
                                blendColors = listOf(
                                    BlendColorEntry(
                                        color = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                                    ),
                                ),
                            ),
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                label = label,
                colors = searchFieldColors(backdropGlass = glassBackdrop != null),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
