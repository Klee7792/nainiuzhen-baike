// 奶牛镇百科 · 工程根构建脚本（最小配置，不依赖 miuix 内部 convention 插件）
//
// Gradle 9 严格类加载隔离：KGP 的共享构建服务（KotlinNativeBundleBuildService）若由
// 兄弟项目（:app / :shared）各自加载的插件类创建，会报
// "Cannot set the value of task ... using a provider ... loaded with <不同 ClassLoader>"。
// 修法（Gradle 官方提示）：所有插件在根项目 apply false 声明一次，类由根加载器统一加载，
// 两个子项目共用同一份类与 service provider。
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.composeMultiplatform) apply false
}
