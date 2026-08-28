package com.nainiuzhen.wiki.platform

import android.content.Context
import com.nainiuzhen.wiki.data.source.CacheProvider

/**
 * 基于 `Context.cacheDir` 的缓存目录提供者。
 *
 * @param context Android 上下文（用于获取 `cacheDir`）。
 * @param versionCode 当前应用版本号（对应 `BuildConfig.VERSION_CODE`）。
 */
class AndroidCacheProvider(
    private val context: Context,
    override val versionCode: Int,
) : CacheProvider {
    override val cacheDir: String
        get() = context.cacheDir.absolutePath
}
