package com.nainiuzhen.wiki.data.source

import java.util.zip.Inflater

/**
 * Android 端 PackDecoder 平台实现。
 *
 * 解析 / XOR / 缓存逻辑已下沉 commonMain（[PackDecoder]），本文件只剩两个平台点：
 * - [loadPackBytes]：经 [AppContextHolder] 注入的 Context 读 APK assets 里的 assets.pack；
 * - [zlibInflate]：`java.util.zip.Inflater`（zlib 包装格式，与 tools/pack_assets.py 对应）。
 */
internal actual fun loadPackBytes(): ByteArray? = try {
    AppContextHolder.current.assets.open("assets.pack").use { it.readBytes() }
} catch (_: Exception) {
    null
}

internal actual fun zlibInflate(data: ByteArray, rawSize: Int): ByteArray {
    val inflater = Inflater()
    inflater.setInput(data)
    val out = ByteArray(rawSize)
    var total = 0
    try {
        while (!inflater.finished()) {
            val n = inflater.inflate(out, total, out.size - total)
            if (n == 0 && inflater.needsInput()) error("assets.pack 条目数据截断")
            total += n
        }
    } finally {
        inflater.end()
    }
    check(total == rawSize) { "assets.pack 条目解压尺寸不符: $total != $rawSize" }
    return out
}
