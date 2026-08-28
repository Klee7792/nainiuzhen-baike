package com.nainiuzhen.wiki.platform

import com.nainiuzhen.wiki.data.repository.SpriteCacheManager
import java.io.File

/**
 * 基于 `java.io.File` 的切片缓存管理器（Android）。
 *
 * 缓存结构：`{cacheDir}/nainiuzhen/sprites/{frameKey}.png` + 版本标记 `version.txt`。
 * 版本不符时由 [clear] 清空重切。
 */
class AndroidSpriteCacheManager(
    private val cacheDir: String,
    private val version: Int,
) : SpriteCacheManager {
    private val root: File get() = File(cacheDir, "nainiuzhen")
    private val spritesDir: File get() = File(root, "sprites")
    private val versionFile: File get() = File(root, "version.txt")

    override fun needsRebuild(): Boolean {
        val f = versionFile
        if (!f.exists()) return true
        return (f.readText().trim().toIntOrNull() ?: -1) != version
    }

    override fun markBuilt() {
        root.mkdirs()
        versionFile.writeText(version.toString())
    }

    override fun clear() {
        spritesDir.deleteRecursively()
        versionFile.delete()
    }

    override fun exists(frameKey: String): Boolean = spriteFile(frameKey).exists()

    override fun read(frameKey: String): ByteArray? {
        val f = spriteFile(frameKey)
        return if (f.exists()) f.readBytes() else null
    }

    override fun write(frameKey: String, bytes: ByteArray) {
        val f = spriteFile(frameKey)
        f.parentFile?.mkdirs()
        f.writeBytes(bytes)
    }

    override fun cacheSizeBytes(): Long =
        if (spritesDir.exists()) {
            spritesDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else {
            0L
        }

    override fun currentVersion(): Int = version

    private fun spriteFile(frameKey: String): File {
        val base = frameKey.removeSuffix(".png")
        return File(spritesDir, "$base.png")
    }
}
