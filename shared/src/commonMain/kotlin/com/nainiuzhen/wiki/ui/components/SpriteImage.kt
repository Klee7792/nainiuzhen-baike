package com.nainiuzhen.wiki.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.nainiuzhen.wiki.ui.nav.LocalSpriteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 切片图显示组件：根据帧名 [frameKey] 从 [LocalSpriteRepository] 取图并渲染。
 *
 * 取图（首启动切片 / 解码 / 写缓存）放到 IO 线程执行，先用占位图显示、加载完成后再替换，
 * 避免在主线程同步切片导致首屏或进入物品页卡顿（启动变慢排查 #1：同步切片阻塞 UI 线程，
 * 尤其版本号变更触发全量重切时最为明显）。
 */
@Composable
fun SpriteImage(
    frameKey: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val sprite = LocalSpriteRepository.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, sprite, frameKey) {
        value = withContext(Dispatchers.IO) { sprite.getImage(frameKey) }
    }
    val show = bitmap ?: remember(sprite) { sprite.placeholder() }
    Image(bitmap = show, contentDescription = contentDescription, modifier = modifier, contentScale = contentScale)
}

/** NPC 立绘显示（无需切片，直接解码 `npcs/<id>.png`）；解码同样放到 IO 线程。 */
@Composable
fun NpcPortraitImage(
    npcId: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val sprite = LocalSpriteRepository.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, sprite, npcId) {
        value = withContext(Dispatchers.IO) { sprite.getNpcImage(npcId) }
    }
    val show = bitmap ?: remember(sprite) { sprite.placeholder() }
    Image(bitmap = show, contentDescription = "NPC $npcId", modifier = modifier, contentScale = contentScale)
}

/** 星级图显示：白(lv_2) / 金(lv_3) / 紫(lv_4)；解码放到 IO 线程。 */
@Composable
fun StarImage(
    level: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val sprite = LocalSpriteRepository.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, sprite, level) {
        value = withContext(Dispatchers.IO) { sprite.getStarImage(level) }
    }
    val show = bitmap ?: remember(sprite) { sprite.placeholder() }
    Image(bitmap = show, contentDescription = null, modifier = modifier, contentScale = contentScale)
}
