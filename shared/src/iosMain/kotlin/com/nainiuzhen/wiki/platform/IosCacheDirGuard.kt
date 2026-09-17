@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.nainiuzhen.wiki.platform

import com.nainiuzhen.wiki.utils.AppLog
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile

/**
 * iOS 缓存目录自愈 + 探针。**在 App 启动最早期调用（早于第一帧）**，见 `MainViewController`。
 *
 * 为什么需要它：Metal 的 GPU 管线二进制缓存（`Library/Caches/com.apple.metal/` 下的
 * `functions.data|list`、`libraries.data|list`）由系统代表进程写入，**App 无法配置其路径**。
 * 一旦该目录被越狱插件「重置数据」清掉、或权限/属主被改坏，缓存就写不进去 ⇒ 每次冷启动
 * 都要重新编译管线，表现为「先卡后流畅、重启即丢」（本项目真机实测）。
 *
 * 本类因此只做三件事，**全部失败路径都只记日志、不抛异常、不阻塞启动**：
 * ① 保证 `Library/Caches` 存在（`withIntermediateDirectories = true`，必要时连 `Library` 一并建）；
 * ② 写 8 字节探针文件再读回，判定目录**是否真的可写**；
 * ③ 把 `com.apple.metal` 下四个缓存文件是否存在及大小写进诊断日志，便于真机反馈直接判因。
 */
internal object IosCacheDirGuard {

    /** Metal 管线缓存的四个文件名（Apple 系统约定）。 */
    private val METAL_FILES = listOf(
        "functions.data", "functions.list", "libraries.data", "libraries.list",
    )

    private const val PROBE_NAME = ".cache_write_probe"
    private const val PROBE_BYTES = 8

    fun ensureAndProbe() {
        try {
            val home = NSHomeDirectory()
            val library = "$home/Library"
            val caches = cachesPath() ?: "$library/Caches"

            val fm = NSFileManager.defaultManager
            val libraryExisted = fm.fileExistsAtPath(library)
            val cachesExisted = fm.fileExistsAtPath(caches)

            val created = ensureDir(caches)
            val writable = probeWrite(caches)

            val metalDir = "$caches/com.apple.metal"
            val metalDirExists = fm.fileExistsAtPath(metalDir)
            val files = METAL_FILES.joinToString(" ") { name ->
                val size = fileSize("$metalDir/$name")
                "$name=" + (size?.let { "${it / 1024}KB" } ?: "无")
            }
            val totalKb = METAL_FILES.sumOf { fileSize("$metalDir/$it") ?: 0L } / 1024

            AppLog.i(
                "缓存目录自愈: Caches=$caches Library存在=$libraryExisted " +
                    "Caches存在=$cachesExisted 本次创建=$created 可写=$writable " +
                    "metal目录=$metalDirExists $files 合计=${totalKb}KB"
            )
            if (!writable) {
                AppLog.i("缓存目录不可写 ⇒ Metal 管线缓存无法落盘，冷启动会一直「先卡后流畅」")
            }
        } catch (t: Throwable) {
            AppLog.e("缓存目录自愈失败（不影响启动）", t)
        }
    }

    private fun cachesPath(): String? {
        val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        return paths.firstOrNull() as? String
    }

    /** 目录不存在则逐级创建；返回"本次是否真的创建了"。 */
    private fun ensureDir(path: String): Boolean {
        val fm = NSFileManager.defaultManager
        if (fm.fileExistsAtPath(path)) return false
        return memScoped {
            val err = alloc<ObjCObjectVar<NSError?>>()
            val ok = fm.createDirectoryAtPath(
                path,
                withIntermediateDirectories = true,
                attributes = null,
                error = err.ptr,
            )
            if (!ok) {
                AppLog.i("创建目录失败: $path err=${err.value?.localizedDescription ?: "未知"}")
            }
            ok
        }
    }

    /** 写 8 字节探针再读回，判定目录是否真的可写（只读权限的容器会让这里失败）。 */
    private fun probeWrite(dir: String): Boolean {
        val path = "$dir/$PROBE_NAME"
        val fm = NSFileManager.defaultManager
        return try {
            val data = ByteArray(PROBE_BYTES) { it.toByte() }.toNSData()
            val wrote = data.writeToFile(path, true)
            val back = NSData.create(contentsOfFile = path)
            val readOk = back != null && back.length.toInt() == PROBE_BYTES
            fm.removeItemAtPath(path, null)
            wrote && readOk
        } catch (t: Throwable) {
            AppLog.i("缓存可写探针异常: ${t.message}")
            false
        }
    }

    private fun fileSize(path: String): Long? {
        val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, null) ?: return null
        return (attrs[NSFileSize] as? NSNumber)?.longValue
    }
}
