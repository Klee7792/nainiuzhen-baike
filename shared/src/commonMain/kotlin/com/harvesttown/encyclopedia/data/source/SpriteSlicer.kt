package com.harvesttown.encyclopedia.data.source

import androidx.compose.ui.graphics.ImageBitmap
import com.harvesttown.encyclopedia.data.model.SpriteAtlasFrame

/**
 * 素材切片器（平台接口）。
 *
 * 负责把图集 PNG 的某帧裁剪成 [ImageBitmap]，并支持 PNG 编解码与占位图，
 * 由 androidMain 以 Android `Bitmap` 实现。
 */
interface SpriteSlicer {
    /** 从图集字节中按帧裁剪出 [ImageBitmap]（处理 rotated / offset / sourceSize）。 */
    fun slice(sheetBytes: ByteArray, frame: SpriteAtlasFrame): ImageBitmap

    /** 把 PNG 字节解码为 [ImageBitmap]（用于读取缓存或 NPC / 星级原图）。 */
    fun decode(bytes: ByteArray): ImageBitmap

    /** 把 [ImageBitmap] 编码为 PNG 字节（用于写入切片缓存）。 */
    fun encode(bitmap: ImageBitmap): ByteArray

    /** 占位图（切片 / 解码失败或资源缺失时返回）。 */
    fun placeholder(): ImageBitmap
}
