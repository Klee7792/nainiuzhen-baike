package com.harvesttown.encyclopedia.data.source

import kotlin.text.decodeToString
import kotlinx.coroutines.runBlocking

/**
 * 资源读取器：统一经平台实现 [readAssetBytes] 读取 `assets/` 下的
 * JSON / 图集 PNG / plist 等二进制与文本资源，跨平台一致。
 *
 * Android 侧从 `app/src/main/assets/` 经 [android.content.res.AssetManager] 读取，
 * 保证资源必定随 APK 打包——CMP 库 `composeResources` 经新版 KMP Android Library
 * 插件合并进纯 Android 消费的已知坑会导致资源缺失（运行时 MissingResourceException），
 * 故改用原生 assets。
 *
 * 注：底层读取为同步实现，这里用 [runBlocking] 桥接（同原 Compose Resources 做法），
 * 以兼容切片缓存与数据解析等同步调用方（[com.harvesttown.encyclopedia.data.repository.SpriteRepository]、
 * [com.harvesttown.encyclopedia.data.AssetManager]）。
 */
object AssetLoader {
    /** 读取 `assets/<path>` 下的原始字节（如 `items.png`、`items.plist`）。 */
    fun loadBytes(path: String): ByteArray = runBlocking { readAssetBytes(path) }

    /** 读取 `assets/<path>` 下的文本（默认 UTF-8），如 `item_database.json`。 */
    fun loadText(path: String): String = runBlocking { readAssetBytes(path) }.decodeToString()
}

/**
 * 平台资源读取：Android 经原生 [android.content.res.AssetManager] 实现。
 * 真正的实现见 androidMain 的 actual。
 */
internal expect fun readAssetBytes(path: String): ByteArray
