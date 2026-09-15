package com.nainiuzhen.wiki.platform

import com.nainiuzhen.wiki.data.repository.SpriteCacheManager
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * 基于 `NSCachesDirectory` 的切片缓存管理器（iOS）。
 *
 * 逻辑对齐 `AndroidSpriteCacheManager`：去磁盘化后切片全量驻内存、**不再写盘**，
 * 只剩「清理旧版本残留」的职责——启动时由 [App] 调用 [clear] 删除旧目录
 * `{caches}/nainiuzhen/`，设置页「清理缓存」亦经此清理。
 * 读取（[read]）与写入（[write]）方法保留仅为兼容接口契约，新路径不再调用。
 */
class IosSpriteCacheManager(
    private val version: Int,
) : SpriteCacheManager {

    private val root: String by lazy { "${cachesRoot()}/nainiuzhen" }
    private val spritesDir: String get() = "$root/sprites"
    private val versionFile: String get() = "$root/version.txt"

    private fun cachesRoot(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        return (paths.firstOrNull() as? String) ?: NSHomeDirectory()
    }

    override fun needsRebuild(): Boolean {
        val text = readTextFile(versionFile) ?: return true
        return (text.trim().toIntOrNull() ?: -1) != version
    }

    override fun markBuilt() {
        mkdirs(root)
        writeTextFile(versionFile, version.toString())
    }

    override fun clear() {
        val fm = NSFileManager.defaultManager
        fm.removeItemAtPath(spritesDir, null)
        fm.removeItemAtPath(versionFile, null)
    }

    override fun exists(frameKey: String): Boolean =
        NSFileManager.defaultManager.fileExistsAtPath(fileFor(frameKey))

    override fun read(frameKey: String): ByteArray? {
        val path = fileFor(frameKey)
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return null
        return try {
            NSData.create(contentsOfFile = path)?.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    override fun write(frameKey: String, bytes: ByteArray) {
        val path = fileFor(frameKey)
        mkdirs(path.substringBeforeLast('/', ""))
        try {
            bytes.toNSData().writeToFile(path, true)
        } catch (_: Exception) {
            // 写缓存失败不影响主流程（切片内存驻留）
        }
    }

    override fun cacheSizeBytes(): Long {
        val fm = NSFileManager.defaultManager
        if (!fm.fileExistsAtPath(spritesDir)) return 0L
        val subpaths = fm.subpathsOfDirectoryAtPath(spritesDir, null) as? List<*> ?: return 0L
        var total = 0L
        for (p in subpaths) {
            val name = p as? String ?: continue
            val attrs = fm.attributesOfItemAtPath("$spritesDir/$name", null)
            val size = attrs?.get(NSFileSize) as? NSNumber
            total += size?.longValue ?: 0L
        }
        return total
    }

    override fun currentVersion(): Int = version

    private fun fileFor(frameKey: String): String {
        val base = frameKey.removeSuffix(".png")
        return "$spritesDir/$base.png"
    }

    private fun mkdirs(path: String) {
        NSFileManager.defaultManager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
    }

    private fun readTextFile(path: String): String? = try {
        NSData.create(contentsOfFile = path)?.toByteArray()?.decodeToString()
    } catch (_: Exception) {
        null
    }

    private fun writeTextFile(path: String, text: String) {
        try {
            text.encodeToByteArray().toNSData().writeToFile(path, true)
        } catch (_: Exception) {
            // 写版本标记失败不影响主流程
        }
    }
}
