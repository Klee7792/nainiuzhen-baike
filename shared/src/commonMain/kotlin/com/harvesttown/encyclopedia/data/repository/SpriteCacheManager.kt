package com.harvesttown.encyclopedia.data.repository

/**
 * 切片缓存管理器（平台接口，由 androidMain 以 `java.io.File` 实现）。
 *
 * 缓存根：`{cacheDir}/nainiuzhen/`
 * - 切片图：`{cacheDir}/nainiuzhen/sprites/{sheetName}/{frameKey}.png`
 * - 版本标记：`{cacheDir}/nainiuzhen/version.txt`（存版本号）
 *
 * 版本比对：启动检测到版本号与 [currentVersion] 不符时由调用方触发 [clear] 重切。
 */
interface SpriteCacheManager {
    /** 是否需要重建（版本标记缺失或与当前版本不符）。 */
    fun needsRebuild(): Boolean

    /** 写入版本标记，记录已按当前版本构建。 */
    fun markBuilt()

    /** 清空切片缓存与版本标记（设置页「清理缓存」调用）。 */
    fun clear()

    /** 切片是否已缓存。 */
    fun exists(frameKey: String): Boolean

    /** 读取缓存切片 PNG 字节；不存在返回 null。 */
    fun read(frameKey: String): ByteArray?

    /** 写入缓存切片 PNG 字节。 */
    fun write(frameKey: String, bytes: ByteArray)

    /** 当前缓存占用字节数（设置页展示用）。 */
    fun cacheSizeBytes(): Long

    /** 当前应用版本号。 */
    fun currentVersion(): Int
}
