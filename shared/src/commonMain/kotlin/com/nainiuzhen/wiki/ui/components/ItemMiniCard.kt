package com.nainiuzhen.wiki.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nainiuzhen.wiki.data.model.ItemInfo
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 迷你物品卡片：用于配方原料区 / 产物区、NPC 最爱·喜欢·讨厌区。
 * 正方形切片 + 名称（单行省略）+ 可选数量角标。点击回调用于穿透到物品详情。
 *
 * @param item 物品（为 null 时显示占位名称）。
 * @param num 数量（如配方原料数量）；为 null 不显示。
 * @param onClick 点击回调。
 */
@Composable
fun ItemMiniCard(
    item: ItemInfo?,
    num: Int? = null,
    onClick: () -> Unit = {},
    scaleContext: SpriteScaleContext = SpriteScaleContext.DialogRecipe,
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = MiuixTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 图标区固定 48dp 并裁剪：素材倍率只缩放图片本身（居中、溢出裁掉），
        // 不再撑大背景卡片（修复「卡片随倍率一起缩放」，#22 复盘）。
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(0.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SpriteImage(
                frameKey = item?.iconFrameKey ?: "",
                scaleContext = scaleContext,
            )
            if (num != null) {
                // 数量角标：显示在图片上层右下角，无胶囊底、字号更小
                Text(
                    text = "×$num",
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(start = 1.dp),
                )
            }
        }
        Text(
            text = item?.name ?: "#?",
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/**
 * 物品卡片行（NPC 最爱 / 喜欢 / 讨厌区、配方原料区等复用）：
 * ≤4 张时整体水平居中（不裁切、不滚）；超过 4 张则横向滚动（两侧加淡入淡出，变更点 #33）。
 * 卡片可点击穿透到物品详情。
 *
 * @param items 物品列表（元素可为 null，对应占位）。
 * @param onItemClick 点击回调（仅非空物品触发）。
 */
@Composable
fun ItemCardRow(
    items: List<ItemInfo?>,
    modifier: Modifier = Modifier,
    onItemClick: (ItemInfo) -> Unit = {},
    scaleContext: SpriteScaleContext = SpriteScaleContext.DialogRecipe,
) {
    if (items.isEmpty()) return
    val row: @Composable () -> Unit = {
        if (items.size <= 4) {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                items.forEach { item ->
                    ItemMiniCard(
                        item = item,
                        onClick = { if (item != null) onItemClick(item) },
                        scaleContext = scaleContext,
                    )
                }
            }
        } else {
            LazyRow(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items.size) { index ->
                    val item = items[index]
                    ItemMiniCard(
                        item = item,
                        onClick = { if (item != null) onItemClick(item) },
                        scaleContext = scaleContext,
                    )
                }
            }
        }
    }
    if (items.size > 4) {
        FadeEdges { row() }
    } else {
        row()
    }
}

/**
 * 横向滚动边缘处理容器：在左右两侧叠加柔和的边缘过渡（「渐变淡出」始终渲染作为兜底，
 * 支持高斯模糊时再额外叠加 [textureBlur] 增强通透感）。
 *
 * - 两侧渐变淡出（背景色→透明）**始终绘制**，不依赖模糊是否可用。这样在 dialog 等
 *   [layerBackdrop] 采样偶发失效的场景下，边缘过渡依然可见（#26 修复：此前 dialog 内
 *   仅依赖模糊层、采样失败时整段硬切无效果）。
 * - **不绑定「启用模糊」开关**（变更点 #27-A）：只要运行环境支持 RuntimeShader /
 *   Android 12+（[rememberDialogSideBlurBackdrop] 返回非 null）即叠加左右高斯模糊层，
 *   与顶栏 [BlurSupport.BlurredBar] 同机制；通透度比旧版略提高（渐变 alpha 0.85→0.7、
 *   模糊叠加 alpha 0.4→0.3），使侧栏更清透。不支持时仅保留渐变兜底。
 *
 * 渐变/模糊层无指针处理，不拦截滚动手势。外部签名保持不变，调用方
 * `if (items.size > 4) FadeEdges { row() } else row()` 继续可用。
 */
@Composable
fun FadeEdges(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val backdrop = rememberDialogSideBlurBackdrop()
    Box(modifier = modifier) {
        // 内层 Box 持有真正的内容。支持模糊时叠加 layerBackdrop，使该行物品成为
        // 模糊采样源，供两侧 textureBlur 边缘层采样。
        val innerModifier = if (backdrop != null) {
            Modifier.layerBackdrop(backdrop)
        } else {
            Modifier
        }
        Box(modifier = innerModifier) {
            content()
        }
        // 两侧「渐变淡出」始终渲染（不依赖 backdrop）：作为兜底，保证在 dialog 等
        // textureBlur 采样失效的场景下边缘过渡依然可见（#26 修复）。
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(24.dp)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(MiuixTheme.colorScheme.surface.copy(alpha = 0.7f), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(24.dp)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, MiuixTheme.colorScheme.surface.copy(alpha = 0.7f)),
                    ),
                ),
        )
        // 若系统支持高斯模糊，在渐变之上额外叠加高斯模糊层增强通透感；即便其采样
        // 偶发失效，下方渐变兜底也已保证可见（双保险）。
        if (backdrop != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(24.dp)
                    .fillMaxHeight()
                    .textureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        blurRadius = 25f,
                        colors = BlurDefaults.blurColors(
                            blendColors = listOf(
                                BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(alpha = 0.3f)),
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(24.dp)
                    .fillMaxHeight()
                    .textureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        blurRadius = 25f,
                        colors = BlurDefaults.blurColors(
                            blendColors = listOf(
                                BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(alpha = 0.3f)),
                            ),
                        ),
                    ),
            )
        }
    }
}
