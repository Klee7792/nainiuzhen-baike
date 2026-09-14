package com.nainiuzhen.wiki.data.source

import androidx.compose.ui.graphics.ImageBitmap
import com.nainiuzhen.wiki.data.model.SpriteAtlasFrame

/**
 * 素材切片器（平台接口）。
 *
 * 负责把图集 PNG 的某帧裁剪成 [ImageBitmap]，并支持 PNG 编解码与占位图，
 * 由 androidMain 以 Android `Bitmap` 实现。
 */
interface SpriteSlicer {
    /** 从图集字节中按帧裁剪出 [ImageBitmap]（处理 rotated / offset / sourceSize）。 */
    fun slice(sheetBytes: ByteArray, frame: SpriteAtlasFrame): ImageBitmap

    /**
     * 批量裁剪：解码图集**一次**后裁出全部帧（启动预热切片专用，
     * 避免逐帧调用 [slice] 时反复解码同一张大图集）。
     * 默认实现逐帧回退到 [slice]（每帧重复解码，仅供平台未覆写时兜底）。
     */
    fun sliceBatch(sheetBytes: ByteArray, frames: Map<String, SpriteAtlasFrame>): Map<String, ImageBitmap> =
        frames.mapValues { (_, frame) -> slice(sheetBytes, frame) }

    /** 把 PNG 字节解码为 [ImageBitmap]（用于读取缓存或 NPC / 星级原图）。 */
    fun decode(bytes: ByteArray): ImageBitmap

    /** 把 [ImageBitmap] 编码为 PNG 字节（用于写入切片缓存）。 */
    fun encode(bitmap: ImageBitmap): ByteArray

    /** 占位图（切片 / 解码失败或资源缺失时返回）。 */
    fun placeholder(): ImageBitmap
}
