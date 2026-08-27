package com.harvesttown.encyclopedia.data.source

/**
 * 缓存目录提供者（平台接口）。
 *
 * 由 androidMain 以 `context.cacheDir` 实现，供 [com.harvesttown.encyclopedia.data.repository.SpriteCacheManager]
 * 读写切片缓存与版本标记。
 */
interface CacheProvider {
    /** 应用缓存根目录（绝对路径，如 `/data/user/0/包名/cache`）。 */
    val cacheDir: String

    /** 当前应用版本号（对应 BuildConfig.VERSION_CODE）。 */
    val versionCode: Int
}
