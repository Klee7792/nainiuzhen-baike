package com.nainiuzhen.wiki.data.model

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

/**
 * 单张切片帧（由 plist 解析而来）。
 *
 * @param sheetName 所属图集（png 文件名，不含扩展名，如 `items`）。
 * @param rect 在图集中的裁剪矩形 `{x,y,w,h}`。
 * @param rotated 是否旋转 90°（Cocos2d plist 标记）。
 * @param offset 居中偏移 `{x,y}`（旧格式字段，保留以备兼容）。
 * @param sourceSize 原始尺寸 `{w,h}`。
 * @param sourceColorRect 在原始画布上的裁剪位置 `{{x,y},{w,h}}`，用于将裁剪/旋转后的帧
 *     贴回一张 `sourceSize` 透明画布（新 plist 格式字段）。
 */
data class SpriteAtlasFrame(
    val sheetName: String,
    val rect: IntRect,
    val rotated: Boolean,
    val offset: IntOffset,
    val sourceSize: IntSize,
    val sourceColorRect: IntOffset = IntOffset.Zero,
)

/**
 * 图集索引：帧名 → [SpriteAtlasFrame]，跨全部图集合并。
 *
 * @param frames 帧名（如 `262353.png`）到帧描述的映射。
 */
class SpriteAtlas(private val frames: Map<String, SpriteAtlasFrame>) {
    /** 根据帧名获取帧描述。 */
    fun getFrame(name: String): SpriteAtlasFrame? = frames[name]

    /** 全部图集（sheet）名称。 */
    fun sheetNames(): List<String> = frames.values.map { it.sheetName }.distinct()

    /** 根据帧名获取其所属图集名称。 */
    fun sheetFor(frameKey: String): String? = frames[frameKey]?.sheetName

    /** 全部帧名。 */
    val frameKeys: Set<String> get() = frames.keys
}
