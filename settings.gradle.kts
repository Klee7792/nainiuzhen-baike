// 奶牛镇百科 · 工程根配置
// 通过 Gradle 复合构建（composite build）引入 miuix 各模块，无需发布到 Maven。

@file:Suppress("UnstableApiUsage")

rootProject.name = "nainiuzhen-baike"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

// 复合构建：按坐标 top.yukonga.miuix.kmp:miuix-* 引用仓库内的 miuix 源码
// （2026-09-15：miuix 已 vendor 进本仓库 `miuix/`，用相对路径 ⇒ 克隆到任何机器/路径都能编）
includeBuild("miuix")

include(":shared")
include(":app")
