package com.harvesttown.encyclopedia.data.source

import com.harvesttown.encyclopedia.shared.generated.resources.Res
import kotlinx.coroutines.runBlocking

/**
 * 资源读取器：统一经 Compose Resources 的 [Res.readBytes] 读取 `files/` 下的
 * JSON / 图集 PNG / plist 等二进制与文本资源，跨平台一致。
 *
 * 注：Compose Resources 1.12 的 [Res.readBytes] 为挂起函数，这里用 [runBlocking] 桥接为同步，
 * 以兼容切片缓存与数据解析等同步调用方（[com.harvesttown.encyclopedia.data.repository.SpriteRepository]、
 * [com.harvesttown.encyclopedia.data.AssetManager]）。
 */
object AssetLoader {
    /** 读取 `files/<path>` 下的原始字节（如 `items.png`、`items.plist`）。 */
    fun loadBytes(path: String): ByteArray = runBlocking { Res.readBytes("files/$path") }

    /** 读取 `files/<path>` 下的文本（默认 UTF-8），如 `item_database.json`。 */
    fun loadText(path: String): String = runBlocking { Res.readBytes("files/$path") }.decodeToString()
}
