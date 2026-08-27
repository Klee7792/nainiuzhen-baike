package com.harvesttown.encyclopedia.utils

/**
 * 应用设置持久化接口（与平台无关，便于在 :shared 中通过 [App] 注入使用）。
 *
 * 实现方负责把 [AppState] 落盘（Android 端用 `SharedPreferences`），
 * 使深色模式 / 圆角 / 模糊 / 转场等偏好在重启后依然保留。
 */
interface AppSettingsStore {
    /** 读取已保存的设置；无记录时返回 [AppState] 默认值。 */
    fun load(): AppState

    /** 保存当前设置。 */
    fun save(state: AppState)
}
