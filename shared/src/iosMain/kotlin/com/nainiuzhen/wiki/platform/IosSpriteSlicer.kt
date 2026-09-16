@file:OptIn(ExperimentalForeignApi::class)

package com.nainiuzhen.wiki.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.nainiuzhen.wiki.data.model.SpriteAtlasFrame
import com.nainiuzhen.wiki.data.source.SpriteSlicer
import com.nainiuzhen.wiki.utils.AppLog
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.MipmapMode
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRestoreGState
import platform.CoreGraphics.CGContextSaveGState
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIImage

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

    private fun decodeSheet(bytes: ByteArray): Image? {
        val skia = try {
            Image.makeFromEncoded(bytes)
        } catch (_: Exception) {
            // Skia 编解码器拒收（历史上：Xcode CgBI 重压缩的 bundle PNG）→ 上层全部静默
            // 回退透明占位，表现为「图标/图集空白但无报错」，必须留痕。
            val header = bytes.take(8).joinToString("") { it.toString(16).padStart(2, '0').uppercase() }
            AppLog.w("Skia PNG 解码失败（字节头 $header）→ 尝试 UIKit 解码兜底")
            decodeViaUIKit(bytes)
        }
        return skia
    }

    /**
     * UIKit（ImageIO）解码兜底：CgBI 等苹果私有 PNG 格式 Skia 拒收，但 ImageIO 原生支持。
     * 把 CGImage 逐像素绘制进 RGBA8 缓冲（CG 坐标系 Y 向上，翻转成 Skia 的 Y 向下），
     * 再经 `Image.makeRaster(RGBA_8888 + PREMUL)` 重建 Skia 图。
     */
    private fun decodeViaUIKit(bytes: ByteArray): Image? = try {
        val uiImage = UIImage(data = bytes.toNSData())
        val cgImage = uiImage.CGImage ?: return null
        val w = CGImageGetWidth(cgImage).toInt()
        val h = CGImageGetHeight(cgImage).toInt()
        if (w <= 0 || h <= 0) return null
        val bytesPerRow = w * 4
        val buffer = ByteArray(bytesPerRow * h)
        val drawn = buffer.usePinned { pinned ->
            val ctx = CGBitmapContextCreate(
                pinned.addressOf(0),
                w.toULong(), h.toULong(),
                8u, bytesPerRow.toULong(),
                CGColorSpaceCreateDeviceRGB(),
                CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
            ) ?: return null
            CGContextSaveGState(ctx)
            CGContextTranslateCTM(ctx, 0.0, h.toDouble())
            CGContextScaleCTM(ctx, 1.0, -1.0)
            CGContextDrawImage(ctx, CGRectMake(0.0, 0.0, w.toDouble(), h.toDouble()), cgImage)
            CGContextRestoreGState(ctx)
            true
        }
        if (!drawn) return null
        AppLog.i("UIKit 解码兜底成功 ${w}x$h")
        Image.makeRaster(ImageInfo(w, h, ColorType.RGBA_8888, ColorAlphaType.PREMUL), buffer, bytesPerRow)
    } catch (t: Throwable) {
        AppLog.e("UIKit 解码兜底失败", t)
        null
    }

    /** 从图集 [sheet] 上按 1:1 像素矩形裁出 [w]×[h] 新位图（NEAREST：像素硬边零模糊）。 */
    private fun extractRegion(sheet: Image, left: Int, top: Int, w: Int, h: Int): Bitmap {
        val out = newBitmap(w, h)
        val canvas = Canvas(out)
        canvas.drawImageRect(
            image = sheet,
            srcLeft = left.toFloat(),
            srcTop = top.toFloat(),
            srcRight = (left + w).toFloat(),
            srcBottom = (top + h).toFloat(),
            dstLeft = 0f,
            dstTop = 0f,
            dstRight = w.toFloat(),
            dstBottom = h.toFloat(),
            samplingMode = FilterMipmap(FilterMode.NEAREST, MipmapMode.NONE),
            paint = null,
            strict = true,
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
        drawNearest(canvas, Image.makeFromBitmap(src), 0f, 0f)
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
        drawNearest(canvas, Image.makeFromBitmap(src), colorRect.x.toFloat(), colorRect.y.toFloat())
        return out
    }

    /** 以整数偏移 1:1 贴图（NEAREST，与 Android 端硬边语义一致）。 */
    private fun drawNearest(canvas: Canvas, image: Image, left: Float, top: Float) {
        canvas.drawImageRect(
            image = image,
            srcLeft = 0f,
            srcTop = 0f,
            srcRight = image.width.toFloat(),
            srcBottom = image.height.toFloat(),
            dstLeft = left,
            dstTop = top,
            dstRight = left + image.width.toFloat(),
            dstBottom = top + image.height.toFloat(),
            samplingMode = FilterMipmap(FilterMode.NEAREST, MipmapMode.NONE),
            paint = null,
            strict = true,
        )
    }

    override fun decode(bytes: ByteArray): ImageBitmap {
        val sheet = decodeSheet(bytes) ?: return placeholder()
        try {
            // 整图 1:1 贴到等尺寸位图（等价 peekPixels，但只用本文件已验证的 API）。
            val bmp = newBitmap(sheet.width, sheet.height)
            drawNearest(Canvas(bmp), sheet, 0f, 0f)
            return bmp.asComposeImageBitmap()
        } finally {
            sheet.close()
        }
    }

    override fun encode(bitmap: ImageBitmap): ByteArray =
        Image.makeFromBitmap(bitmap.asSkiaBitmap())
            .encodeToData(EncodedImageFormat.PNG)
            .let { requireNotNull(it) { "encodeToData 失败" } }
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
