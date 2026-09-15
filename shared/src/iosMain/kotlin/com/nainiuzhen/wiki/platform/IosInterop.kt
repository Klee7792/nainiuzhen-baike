package com.nainiuzhen.wiki.platform

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.posix.memcpy

/**
 * iOS 平台公共桥接件（NSData ↔ ByteArray / bundle 资源读取）。
 */

/** [NSData] → [ByteArray]（内存拷贝一份，调用方持有独立数据）。 */
internal fun NSData.toByteArray(): ByteArray =
    ByteArray(length.toInt()).also { out ->
        out.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    }

/** [ByteArray] → [NSData]（拷贝进 NSData，可安全写盘）。 */
internal fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
} ?: error("NSData.create 失败")

/**
 * 从 app bundle 读取资源文件（CI 构建时经 Xcode Resources phase 打入）。
 *
 * 路径语义与 Android assets 一致：`config/item_database.json`、`res/npcs/1.png`、
 * `ic_launcher.png`（bundle 根）。
 *
 * @throws Exception 资源缺失或读取失败（调用方按需捕获）。
 */
internal fun bundleAssetBytes(path: String): ByteArray {
    val bundle = NSBundle.mainBundle
    val dir = path.substringBeforeLast('/', "")
    val name = path.substringAfterLast('/')
    val file = name.substringBeforeLast('.')
    val ext = name.substringAfterLast('.', "")
    val resolved = if (dir.isEmpty()) {
        bundle.pathForResource(file, ofType = ext.ifEmpty { null })
    } else {
        bundle.pathForResource(file, ofType = ext.ifEmpty { null }, inDirectory = dir)
    } ?: error("bundle 资源缺失: $path")
    val data = NSData.create(contentsOfFile = resolved)
        ?: error("bundle 资源读取失败: $path")
    return data.toByteArray()
}
