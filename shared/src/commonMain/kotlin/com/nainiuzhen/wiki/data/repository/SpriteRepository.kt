package com.nainiuzhen.wiki.data.repository

import androidx.compose.ui.graphics.ImageBitmap
import com.nainiuzhen.wiki.data.model.SpriteAtlas
import com.nainiuzhen.wiki.data.source.AssetLoader
import com.nainiuzhen.wiki.data.source.SpriteSlicer

/**
 * 切片仓储：图集索引 + 懒切片 + 缓存读写。
 *
 * - [getImage]：物品 / 配方图标，按帧名查找图集 → 命中缓存则返回，否则切片并写缓存。
 * - [getNpcImage]：NPC 立绘（无需切片，直接解码 `npcs/<id>.png`）。
 * - [getStarImage]：星级图（`star/lv_2|lv_3|lv_4.png`，对应白 / 金 / 紫）。
 */
class SpriteRepository(
    private val atlas: SpriteAtlas,
    private val slicer: SpriteSlicer,
    private val cache: SpriteCacheManager,
) {
    /** 取切片图（帧名不含 `.png`）。 */
    fun getImage(frameKey: String): ImageBitmap {
        cache.read(frameKey)?.let { return slicer.decode(it) }
        val sheetName = atlas.sheetFor(frameKey) ?: return slicer.placeholder()
        val frame = atlas.getFrame(frameKey) ?: return slicer.placeholder()
        return try {
            val sheetBytes = AssetLoader.loadBytes("res/$sheetName.png")
            val bmp = slicer.slice(sheetBytes, frame)
            cache.write(frameKey, slicer.encode(bmp))
            bmp
        } catch (_: Exception) {
            slicer.placeholder()
        }
    }

    /** 取 NPC 立绘（id）。 */
    fun getNpcImage(npcId: Int): ImageBitmap = try {
        slicer.decode(AssetLoader.loadBytes("res/npcs/$npcId.png"))
    } catch (_: Exception) {
        slicer.placeholder()
    }

    /** 取星级图（1=白 / 2=金 / 其它=紫）。 */
    fun getStarImage(level: Int): ImageBitmap {
        val name = when (level) {
            1 -> "lv_2"
            2 -> "lv_3"
            else -> "lv_4"
        }
        return try {
            slicer.decode(AssetLoader.loadBytes("res/star/$name.png"))
        } catch (_: Exception) {
            slicer.placeholder()
        }
    }

    /** 清空切片缓存（设置页「清理缓存」调用）。 */
    fun clearCache() = cache.clear()

    /** 当前切片缓存占用字节数。 */
    fun cacheSizeBytes(): Long = cache.cacheSizeBytes()

    /** 当前应用版本号（用于展示）。 */
    fun currentVersion(): Int = cache.currentVersion()
}
