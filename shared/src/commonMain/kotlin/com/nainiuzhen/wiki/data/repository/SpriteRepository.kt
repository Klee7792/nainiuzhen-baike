package com.nainiuzhen.wiki.data.repository

import androidx.compose.ui.graphics.ImageBitmap
import com.nainiuzhen.wiki.data.model.SpriteAtlas
import com.nainiuzhen.wiki.data.source.AssetLoader
import com.nainiuzhen.wiki.data.source.SpriteSlicer
import com.nainiuzhen.wiki.utils.AppLog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 切片仓储：图集索引 + 内存切片表 + 启动预热。
 *
 * 去磁盘化策略：切片全量驻内存、**不再写盘**。启动时由 [preloadAllSprites] 把全部
 * 6565 帧一次性切片进内存表（每张图集只 loadBytes + 解码一次），此后 [getImage]
 * 等取图接口全部内存命中、秒回；未预热到的帧（理论上不存在）走懒切片兜底，
 * 切片结果同样只进内存。磁盘缓存（[SpriteCacheManager]）仅保留「清理旧版本残留」
 * 的职责，读取 / 写入路径已不再使用。
 *
 * - [getImage]：物品 / 配方图标，按帧名查内存表。
 * - [getNpcImage]：NPC 立绘（无需切片，直接解码 `npcs/<id>.png`）。
 * - [getStarImage]：星级图（`star/lv_2|lv_3|lv_4.png`，对应白 / 金 / 紫）。
 * - [getAssetImage]：`assets/` 根任意 PNG（如关于页 app 图标）。
 */
class SpriteRepository(
    private val atlas: SpriteAtlas,
    private val slicer: SpriteSlicer,
    private val cache: SpriteCacheManager,
) {
    /** 切片内存表：帧名 → 位图（预热全量填充，运行期懒补）。 */
    private val spriteMemory = mutableMapOf<String, ImageBitmap>()

    /** NPC 立绘内存表：id → 位图（57 张、184×141，全量驻内存无压力）。 */
    private val npcMemory = mutableMapOf<Int, ImageBitmap>()

    /** 星级图内存表：level → 位图。 */
    private val starMemory = mutableMapOf<Int, ImageBitmap>()

    /** `assets/` 根 PNG 内存表：路径 → 位图。 */
    private val assetMemory = mutableMapOf<String, ImageBitmap>()

    /** 保护上述内存表的互斥锁（取图在 IoDispatcher 多线程并发执行，禁 java.util.concurrent）。 */
    private val mutex = Mutex()

    /** 取切片图（帧名不含 `.png`）。内存命中直接返回；未命中懒切片，结果只进内存、不落盘。 */
    suspend fun getImage(frameKey: String): ImageBitmap {
        mutex.withLock { spriteMemory[frameKey] }?.let { return it }
        val frame = atlas.getFrame(frameKey) ?: return slicer.placeholder()
        return try {
            val sheetBytes = AssetLoader.loadBytes("res/${frame.sheetName}.png")
            val bmp = slicer.slice(sheetBytes, frame)
            mutex.withLock { spriteMemory[frameKey] = bmp }
            bmp
        } catch (_: Exception) {
            slicer.placeholder()
        }
    }

    /** 取 NPC 立绘（id）。内存命中直接返回。 */
    suspend fun getNpcImage(npcId: Int): ImageBitmap {
        mutex.withLock { npcMemory[npcId] }?.let { return it }
        return try {
            val bmp = slicer.decode(AssetLoader.loadBytes("res/npcs/$npcId.png"))
            mutex.withLock { npcMemory[npcId] = bmp }
            bmp
        } catch (_: Exception) {
            slicer.placeholder()
        }
    }

    /** 占位图（透明 1x1）。供 [SpriteImage] 等组件在异步取图完成前显示。 */
    fun placeholder(): ImageBitmap = slicer.placeholder()

    /** 图集总帧数（诊断用：0 = plist 未加载）。 */
    val atlasFrameCount: Int get() = atlas.frameKeys.size

    /** 已切片进内存的帧数（诊断用：远小于 [atlasFrameCount] = 图集切片大面积失败）。 */
    val loadedSpriteCount: Int get() = spriteMemory.size

    /** 取星级图（1=白 / 2=金 / 其它=紫）。内存命中直接返回。 */
    suspend fun getStarImage(level: Int): ImageBitmap {
        mutex.withLock { starMemory[level] }?.let { return it }
        val name = when (level) {
            1 -> "lv_2"
            2 -> "lv_3"
            else -> "lv_4"
        }
        return try {
            val bmp = slicer.decode(AssetLoader.loadBytes("res/star/$name.png"))
            mutex.withLock { starMemory[level] = bmp }
            bmp
        } catch (_: Exception) {
            slicer.placeholder()
        }
    }

    /** 从 `assets/` 根读取任意 PNG 并解码为 [ImageBitmap]（如关于页 app 图标）。内存命中直接返回。 */
    suspend fun getAssetImage(path: String): ImageBitmap {
        mutex.withLock { assetMemory[path] }?.let { return it }
        return try {
            val bmp = slicer.decode(AssetLoader.loadBytes(path))
            mutex.withLock { assetMemory[path] = bmp }
            bmp
        } catch (_: Exception) {
            slicer.placeholder()
        }
    }

    /**
     * 预热切片：按图集分批把全部帧切片进内存表（每张图集只 loadBytes + 解码一次，
     * 处理完即由 [SpriteSlicer.sliceBatch] 内部回收 sheet 位图），全部切片不落盘。
     *
     * @param onProgress 进度回调，`done` / `total` 为已处理帧数与总帧数；
     *     每处理完一张图集回调一次，在调用协程的调度线程上执行。
     *
     * @return 切片失败的图集张数（0 = 全部成功）。整张图集失败时其帧会回退占位图，
     *     界面表现为「空白但没崩」——必须写诊断日志，否则无从判断素材是否真的加载成功。
     */
    suspend fun preloadAllSprites(onProgress: suspend (done: Int, total: Int) -> Unit): Int {
        // 按图集分组：同一 sheet 的全部帧共用一次解码。
        val bySheet = atlas.frameKeys
            .mapNotNull { key -> atlas.getFrame(key)?.let { key to it } }
            .groupBy({ it.second.sheetName }, { it.first to it.second })
        val total = bySheet.values.sumOf { it.size }
        var done = 0
        var failedSheets = 0
        for ((sheet, frames) in bySheet) {
            val sliced = try {
                slicer.sliceBatch(AssetLoader.loadBytes("res/$sheet.png"), frames.toMap())
            } catch (t: Throwable) {
                // 整张图集缺失 / 解码失败：全部帧回退占位图，保证内存表与进度计数完整。
                failedSheets += 1
                AppLog.e("图集切片失败: res/$sheet.png（${frames.size} 帧回退占位图）", t)
                frames.associate { it.first to slicer.placeholder() }
            }
            mutex.withLock { spriteMemory.putAll(sliced) }
            done += frames.size
            onProgress(done, total)
        }
        if (total == 0) {
            AppLog.e("素材切片跳过：图集帧数为 0（plist 未加载成功）")
        } else {
            AppLog.i(
                "素材切片完成：图集成功 ${bySheet.size - failedSheets}/${bySheet.size}，" +
                    "帧 $total，内存表 ${spriteMemory.size}"
            )
        }
        if (failedSheets > 0) AppLog.w("有 $failedSheets 张图集切片失败（对应图标会显示为空白）")
        return failedSheets
    }

    /** 清空缓存（设置页「清理缓存」调用）：清内存切片表 + 删除磁盘旧残留。 */
    suspend fun clearCache() {
        mutex.withLock {
            spriteMemory.clear()
            npcMemory.clear()
            starMemory.clear()
            assetMemory.clear()
        }
        cache.clear()
    }

    /** 当前磁盘缓存残留字节数（设置页展示用；去磁盘化后新切片不写盘，此值恒趋近 0）。 */
    fun cacheSizeBytes(): Long = cache.cacheSizeBytes()

    /** 当前应用版本号（用于展示）。 */
    fun currentVersion(): Int = cache.currentVersion()
}
