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

    // iOS 目标（2026-09-15 iOS 化）：与 miuix example 同构。
    // - 仅真机架构（arm64）+ Apple Silicon 模拟器；CI 出包只用 IosArm64。
    // - isStatic = true：静态框架直接链进主二进制，.app 内无额外 dylib（TrollStore 侧载友好）。
    // - smallBinary：裁剪 K/N 二进制体积。
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
            binaryOption("smallBinary", "true")
        }
    }

    sourceSets {
        // gradle.properties 关闭了默认层级模板（androidLibrary 插件要求），iOS 中间层源集需手工接线：
        // commonMain → iosMain → iosArm64Main / iosSimulatorArm64Main。
        // androidMain 由 AGP KMP 插件自行接线，无需处理。
        val iosMain = findByName("iosMain") ?: create("iosMain")
        iosMain.dependsOn(getByName("commonMain"))
        getByName("iosArm64Main").dependsOn(iosMain)
        getByName("iosSimulatorArm64Main").dependsOn(iosMain)

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
