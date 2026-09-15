package com.nainiuzhen.wiki.data.source

import com.nainiuzhen.wiki.platform.bundleAssetBytes

/**
 * iOS 端 readAssetBytes 实现（对应 androidMain 的 PlatformAssetReader）：
 *
 * 1. 优先从 assets.pack（NZPK v2 纯内存解码，commonMain 通用实现）取；
 * 2. 包缺失或无此条目时回退 app bundle（CI 构建把 assets.pack / ic_launcher.png
 *    经 Xcode Resources phase 打入 bundle 根）。
 */
internal actual fun readAssetBytes(path: String): ByteArray {
    PackDecoder.get(path)?.let { return it }
    return bundleAssetBytes(path)
}
