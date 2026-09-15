package com.nainiuzhen.wiki.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.nainiuzhen.wiki.data.model.SpriteAtlasFrame
import com.nainiuzhen.wiki.data.source.SpriteSlicer
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Rect

/**
 * 基于 skiko（Skia）的切片器实现（iOS）。
 *
 * 逻辑逐条对齐 `AndroidSpriteSlicer`（参考 `smart_picture_tool.py` 的 `_slice_plist_new`）：
 * - [slice]：从图集按帧裁剪；处理 Cocos2d 的 rotated（顺时针 90° 打包 → 逆时针还原），
 *   裁剪/旋转后贴回一张 `sourceSize` 透明画布（偏移 `sourceColorRect`），
 *   还原 plist 中被裁剪掉的透明边距，保证图标对齐。
 * - [sliceBatch]：解码图集一次后批量裁出全部帧（启动预热切片主路径）。
 * - 裁剪/合成全部 1:1 像素映射 + `FilterMode.NEAREST`，满足像素艺术硬边（零模糊）要求。
 */
class IosSpriteSlicer : SpriteSlicer {

    override fun slice(sheetBytes: ByteArray, frame: SpriteAtlasFrame): ImageBitmap {
        val sheet = decodeSheet(sheetBytes) ?: return placeholder()
        return try {
            sliceFromSheet(sheet, frame)
        } finally {
            sheet.close()
        }
    }

    override fun sliceBatch(
        sheetBytes: ByteArray,
        frames: Map<String, SpriteAtlasFrame>,
    ): Map<String, ImageBitmap> {
        // 解码图集仅一次，逐帧裁剪（启动预热切片主路径）。
        val sheet = decodeSheet(sheetBytes)
            ?: return frames.mapValues { placeholder() }
        return try {
            frames.mapValues { (_, frame) -> sliceFromSheet(sheet, frame) }
        } finally {
            sheet.close()
        }
    }

    /** 在已解码图集上裁剪单帧（不关闭 sheet，由调用方统一关闭）。 */
    private fun sliceFromSheet(sheet: Image, frame: SpriteAtlasFrame): ImageBitmap = try {
        val left = frame.rect.left
        val top = frame.rect.top
        val w = frame.rect.width
        val h = frame.rect.height
        // rotated 帧在图集中占位为 h × w，裁剪框需交换宽高；rotated=false 时为 w × h。
        val cropped =
            if (frame.rotated) {
                val raw = extractRegion(sheet, left, top, h, w)
                rotateCcw(raw) // 还原图集中顺时针打包的帧：逆时针 90°
            } else {
                extractRegion(sheet, left, top, w, h)
            }
        // 贴回 sourceSize 透明画布，偏移为 sourceColorRect（若均为 0 则等价无操作）。
        placeOnSourceCanvas(cropped, frame.sourceSize, frame.sourceColorRect).asComposeImageBitmap()
    } catch (_: Exception) {
        placeholder()
    }

    private fun decodeSheet(bytes: ByteArray): Image? = try {
        Image.makeFromEncoded(bytes)
    } catch (_: Exception) {
        null
    }

    /** 从图集 [sheet] 上按 1:1 像素矩形裁出 [w]×[h] 新位图。 */
    private fun extractRegion(sheet: Image, left: Int, top: Int, w: Int, h: Int): Bitmap {
        val out = newBitmap(w, h)
        val canvas = Canvas(out)
        canvas.drawImageRect(
            sheet,
            Rect.makeXYWH(left.toFloat(), top.toFloat(), w.toFloat(), h.toFloat()),
            Rect.makeXYWH(0f, 0f, w.toFloat(), h.toFloat()),
            FilterMipmap(FilterMode.NEAREST, MipmapMode.NONE),
        )
        return out
    }

    /** 逆时针旋转 90°（对应 Android `postRotate(-90f)` / Python PIL `Image.ROTATE_90` 的逆）。 */
    private fun rotateCcw(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        // 旋转后宽高互换：dest 宽 = src 高，dest 高 = src 宽。
        val out = newBitmap(h, w)
        val canvas = Canvas(out)
        canvas.save()
        // (x, y) → (y, W - x)：先平移再旋转 -90°（y-down 坐标系下即视觉逆时针）。
        canvas.translate(0f, w.toFloat())
        canvas.rotate(-90f, 0f, 0f)
        canvas.drawImage(Image.makeFromBitmap(src), 0f, 0f)
        canvas.restore()
        return out
    }

    /** 将 [src] 贴到一张 [sourceSize] 透明画布，偏移 [colorRect] 的 (x, y)。 */
    private fun placeOnSourceCanvas(src: Bitmap, sourceSize: IntSize, colorRect: IntOffset): Bitmap {
        val sw = sourceSize.width.coerceAtLeast(src.width)
        val sh = sourceSize.height.coerceAtLeast(src.height)
        if (sw == src.width && sh == src.height && colorRect.x == 0 && colorRect.y == 0) {
            return src // 无需合成
        }
        val out = newBitmap(sw, sh)
        val canvas = Canvas(out)
        canvas.drawImage(Image.makeFromBitmap(src), colorRect.x.toFloat(), colorRect.y.toFloat())
        return out
    }

    override fun decode(bytes: ByteArray): ImageBitmap {
        val sheet = decodeSheet(bytes) ?: return placeholder()
        val bmp = sheet.peekPixels() ?: run {
            sheet.close()
            return placeholder()
        }
        return bmp.asComposeImageBitmap()
    }

    override fun encode(bitmap: ImageBitmap): ByteArray =
        Image.makeFromBitmap(bitmap.asSkiaBitmap())
            .encodeToData(EncodedImageFormat.PNG)
            .bytes

    override fun placeholder(): ImageBitmap {
        val out = newBitmap(1, 1)
        out.erase(0x00000000) // 全透明 1×1 占位
        return out.asComposeImageBitmap()
    }

    private fun newBitmap(w: Int, h: Int): Bitmap {
        val out = Bitmap()
        val ok = out.allocPixels(ImageInfo.makeN32(w, h, ColorAlphaType.PREMUL))
        check(ok) { "Bitmap.allocPixels 失败: ${w}x$h" }
        out.erase(0x00000000) // 透明底
        return out
    }
}
