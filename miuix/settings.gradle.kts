// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("UnstableApiUsage")

rootProject.name = "compose-miuix-ui"

// 阿里云镜像仅本地启用（国内加速）；CI（GitHub Actions 自动设 CI=true）访问 aliyun 会
// 超时导致依赖解析整链失败（连 mavenCentral 都被连带禁用），故 CI 上直连官方源。
val isCi = System.getenv("CI") != null

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-plugins")
    repositories {
        // 阿里云镜像（国内加速，命中失败自动回退官方源）；CI 上跳过
        if (!isCi) {
            maven("https://maven.aliyun.com/repository/google") {
                content {
                    includeGroupAndSubgroups("androidx")
                    includeGroupAndSubgroups("com.android")
                    includeGroupAndSubgroups("com.google")
                }
            }
            maven("https://maven.aliyun.com/repository/public")
            maven("https://maven.aliyun.com/repository/gradle-plugin")
        }
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
        if (!isCi) {
            maven("https://maven.aliyun.com/repository/google") {
                content {
                    includeGroupAndSubgroups("androidx")
                    includeGroupAndSubgroups("com.android")
                    includeGroupAndSubgroups("com.google")
                }
            }
            maven("https://maven.aliyun.com/repository/public")
        }
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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":miuix-core")
include(":miuix-ui")
include(":miuix-preference")
include(":miuix-shader")
include(":miuix-blur")
include(":miuix-squircle")
include(":miuix-icons")
include(":miuix-nav")

include(":baselineprofile")

include(":example:shared")
include(":example:android")
include(":example:desktop")
include(":example:web")
include(":example:macos")

include(":docs:demo")
include(":docs:iconGen")
