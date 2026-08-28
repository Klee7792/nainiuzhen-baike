// 奶牛镇百科 · :shared 模块（CMP 公共逻辑 + Android 平台实现）
// 通过 composite build 以坐标引用 miuix 各模块：
//   top.yukonga.miuix.kmp:miuix-ui / miuix-nav / miuix-preference / miuix-icons / miuix-blur / miuix-squircle

plugins {
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

group = "com.nainiuzhen.wiki"

kotlin {
    jvmToolchain(21)
    androidLibrary {
        namespace = "com.nainiuzhen.wiki.shared"
        compileSdk = 37
        minSdk = 24
    }

    sourceSets {
        commonMain.dependencies {
            api("top.yukonga.miuix.kmp:miuix-ui:0.9.4")
            api("top.yukonga.miuix.kmp:miuix-preference:0.9.4")
            implementation("top.yukonga.miuix.kmp:miuix-nav:0.9.4")
            implementation("top.yukonga.miuix.kmp:miuix-icons:0.9.4")
            implementation("top.yukonga.miuix.kmp:miuix-blur:0.9.4")
            implementation("top.yukonga.miuix.kmp:miuix-squircle:0.9.4")
            implementation(libs.jetbrains.compose.components.resources)
            implementation(libs.androidx.navigationevent)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.collections.immutable)
        }
        androidMain.dependencies {
            // OnResumeEffect.android 用到 ComponentActivity / Lifecycle（由 activity-compose 传递 lifecycle-runtime）
            implementation(libs.androidx.activity)
        }
    }
}

compose.resources {
    publicResClass = true
}
