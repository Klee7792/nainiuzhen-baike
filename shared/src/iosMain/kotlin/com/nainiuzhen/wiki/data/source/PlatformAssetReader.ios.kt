package com.nainiuzhen.wiki.data.source

import com.nainiuzhen.wiki.platform.bundleAssetBytes
import com.nainiuzhen.wiki.utils.AppLog

/**
 * iOS 端 readAssetBytes 实现（对应 androidMain 的 PlatformAssetReader）：
 *
 * 1. 优先从 assets.pack（NZPK v2 纯内存解码，commonMain 通用实现）取；
 * 2. 包缺失或无此条目时回退 app bundle（CI 构建把 assets.pack / ic_launcher.png
 *    经 Xcode Resources phase 打入 bundle 根）。
 *
 * 两条路都拿不到时**必须留痕**：调用方（如 AssetManager.buildAtlas）会静默跳过
 * 缺失资源，表现为整页空白却无任何报错。
 */
internal actual fun readAssetBytes(path: String): ByteArray {
    PackDecoder.get(path)?.let { return it }
    return try {
        bundleAssetBytes(path)
    } catch (t: Throwable) {
        AppLog.e("资源读取失败（pack 未命中且 bundle 无此文件）: $path", t)
        throw t
    }
}
