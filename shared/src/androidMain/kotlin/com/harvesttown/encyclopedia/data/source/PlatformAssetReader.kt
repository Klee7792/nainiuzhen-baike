package com.harvesttown.encyclopedia.data.source

import android.content.Context

/**
 * 持有 Android [Context]，供 [readAssetBytes] 经原生 [android.content.res.AssetManager]
 * 读取 `assets/` 下的资源。
 *
 * 必须在 Application/Activity 创建时调用 [init] 注入（见 `:app` 的 MainActivity），
 * 因此 [init] 设为 public 以便跨模块（:app）调用；[current] 仅模块内部使用。
 */
object AppContextHolder {
    private var context: Context? = null

    /** 注入 Android Context（建议传 ApplicationContext）。 */
    fun init(context: Context) {
        this.context = context.applicationContext
    }

    internal val current: Context
        get() = context ?: error("AppContextHolder.init() 必须在读取资源前调用")
}

internal actual fun readAssetBytes(path: String): ByteArray {
    val assetManager = AppContextHolder.current.assets
    return assetManager.open(path).use { it.readBytes() }
}
