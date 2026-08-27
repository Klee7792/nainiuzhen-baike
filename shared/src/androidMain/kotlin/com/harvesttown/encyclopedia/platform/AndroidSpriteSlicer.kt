package com.harvesttown.encyclopedia.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.harvesttown.encyclopedia.data.model.SpriteAtlasFrame
import com.harvesttown.encyclopedia.data.source.SpriteSlicer

/**
 * 基于 Android `Bitmap` 的切片器实现。
 *
 * - [slice]：从图集字节裁剪单帧；处理 Cocos2d 的 rotated（顺时针 90° 打包 → 逆时针还原）。
 *   裁剪/旋转后，将结果贴回一张 `sourceSize` 透明画布（偏移为 `sourceColorRect`），
 *   以还原 plist 中被裁剪掉的透明边距，保证图标对齐（参考 `smart_picture_tool.py` 的 `_slice_plist_new`）。
 * - [decode]/[encode]：PNG 字节与 [ImageBitmap] 互转（用于缓存与 NPC / 星级原图）。
 */
class AndroidSpriteSlicer : SpriteSlicer {
    override fun slice(sheetBytes: ByteArray, frame: SpriteAtlasFrame): ImageBitmap {
        val sheet = BitmapFactory.decodeByteArray(sheetBytes, 0, sheetBytes.size) ?: return placeholder()
        return try {
            val left = frame.rect.left
            val top = frame.rect.top
            val w = frame.rect.width
            val h = frame.rect.height
            // rotated 帧在图集中占位为 h × w，裁剪框需交换宽高；rotated=false 时为 w × h。
            val cropped =
                if (frame.rotated) {
                    val raw = Bitmap.createBitmap(sheet, left, top, h, w)
                    rotateCcw(raw) // 还原图集中顺时针打包的帧：逆时针 90°
                } else {
                    Bitmap.createBitmap(sheet, left, top, w, h)
                }
            // 贴回 sourceSize 透明画布，偏移为 sourceColorRect（若均为 0 则等价无操作）。
            val placed = placeOnSourceCanvas(cropped, frame.sourceSize, frame.sourceColorRect)
            placed.asImageBitmap()
        } catch (_: Exception) {
            placeholder()
        } finally {
            sheet.recycle()
        }
    }

    /** 逆时针旋转 90°（对应 Python PIL `Image.ROTATE_90` 的逆），把图集中顺时针打包的帧还原为正向。 */
    private fun rotateCcw(src: Bitmap): Bitmap {
        val matrix = Matrix().apply { postRotate(-90f) }
        val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        src.recycle()
        return out
    }

    /** 将 [src] 贴到一张 [sourceSize] 透明画布，偏移 [colorRect] 的 (x, y)。 */
    private fun placeOnSourceCanvas(src: Bitmap, sourceSize: IntSize, colorRect: IntOffset): Bitmap {
        val sw = sourceSize.width.coerceAtLeast(src.width)
        val sh = sourceSize.height.coerceAtLeast(src.height)
        if (sw == src.width && sh == src.height && colorRect.x == 0 && colorRect.y == 0) {
            return src // 无需合成
        }
        val out = Bitmap.createBitmap(sw, sh, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(src, colorRect.x.toFloat(), colorRect.y.toFloat(), null)
        src.recycle()
        return out
    }

    override fun decode(bytes: ByteArray): ImageBitmap {
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return placeholder()
        return bmp.asImageBitmap()
    }

    override fun encode(bitmap: ImageBitmap): ByteArray {
        val bmp = bitmap.asAndroidBitmap()
        val stream = java.io.ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    override fun placeholder(): ImageBitmap {
        val bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bmp.setPixel(0, 0, android.graphics.Color.TRANSPARENT)
        return bmp.asImageBitmap()
    }
}
