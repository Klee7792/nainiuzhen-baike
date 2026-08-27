package com.harvesttown.encyclopedia.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.harvesttown.encyclopedia.ui.nav.LocalSpriteRepository

/**
 * 切片图显示组件：根据帧名 [frameKey] 从 [LocalSpriteRepository] 取图并渲染。
 * [getImage] 已做缓存，这里用 [remember] 避免每次重组重复解码。
 */
@Composable
fun SpriteImage(
    frameKey: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val sprite = LocalSpriteRepository.current
    val bitmap = remember(frameKey) { sprite.getImage(frameKey) }
    Image(bitmap = bitmap, contentDescription = contentDescription, modifier = modifier, contentScale = contentScale)
}

/** NPC 立绘显示（无需切片，直接解码 `npcs/<id>.png`）。 */
@Composable
fun NpcPortraitImage(
    npcId: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val sprite = LocalSpriteRepository.current
    val bitmap = remember(npcId) { sprite.getNpcImage(npcId) }
    Image(bitmap = bitmap, contentDescription = "NPC $npcId", modifier = modifier, contentScale = contentScale)
}

/** 星级图显示：白(lv_2) / 金(lv_3) / 紫(lv_4)。 */
@Composable
fun StarImage(
    level: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val sprite = LocalSpriteRepository.current
    val bitmap = remember(level) { sprite.getStarImage(level) }
    Image(bitmap = bitmap, contentDescription = null, modifier = modifier, contentScale = contentScale)
}
