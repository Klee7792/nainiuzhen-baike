package com.nainiuzhen.wiki.utils

/**
 * 显示短 Toast。走 Compose 绘制的 [AppToastHost]，自动跟随 App 主题（深浅色 / Monet），
 * 不再使用系统 [android.widget.Toast]（深色模式下仍为白底，与 App 配色割裂）。
 * 用于暂时未实现的外部链接/功能占位提示，以及冷启动耗时提示。
 *
 * 必须在组合树中存在 [AppToastHost] 时调用才有效（由其注册 [ToastBridge.sink]）。
 */
fun showToast(message: String) {
    ToastBridge.sink?.invoke(message)
}

/**
 * 由 [AppToastHost] 在组合时注册、退出组合（或卸载）时解注册的桥接器。
 * [showToast] 通过它把文案投递给正在组合的宿主。
 */
object ToastBridge {
    var sink: ((String) -> Unit)? = null
}
