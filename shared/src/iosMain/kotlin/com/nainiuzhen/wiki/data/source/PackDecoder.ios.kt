package com.nainiuzhen.wiki.data.source

import com.nainiuzhen.wiki.platform.bundleAssetBytes
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.ULongVar
import kotlinx.cinterop.value
import platform.zlib.Z_OK
import platform.zlib.uncompress

/**
 * iOS 端 PackDecoder 平台实现：
 * - [loadPackBytes]：读 app bundle 根的 assets.pack（CI 注入），缺失返回 null；
 * - [zlibInflate]：iOS SDK 自带 zlib（K/N 预置 platform.zlib），`uncompress` 即
 *   zlib 包装格式解压，与 tools/pack_assets.py 的 zlib.compress 对应。
 */
internal actual fun loadPackBytes(): ByteArray? = try {
    bundleAssetBytes("assets.pack")
} catch (_: Exception) {
    null
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun zlibInflate(data: ByteArray, rawSize: Int): ByteArray {
    val out = ByteArray(rawSize)
    val rc = data.usePinned { src ->
        out.usePinned { dst ->
            memScoped {
                val destLen = alloc<ULongVar>()
                destLen.value = rawSize.toULong()
                uncompress(
                    dst.addressOf(0).reinterpret<UByteVar>(),
                    destLen.ptr,
                    src.addressOf(0).reinterpret<UByteVar>(),
                    data.size.toULong(),
                )
            }
        }
    }
    check(rc == Z_OK) { "assets.pack 条目解压失败（zlib rc=$rc）" }
    return out
}
