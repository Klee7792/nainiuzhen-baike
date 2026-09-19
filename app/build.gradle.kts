// 奶牛镇百�?· :app 模块（仅 Android 入口：MainActivity + Manifest�?
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.nainiuzhen.wiki"
    compileSdk = 37

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.nainiuzhen.wiki"
        minSdk = 24
        targetSdk = 37
        // Version code single source of truth: builds/build_number.txt
        // (CI writes it back when workflow_dispatch input "bump_version" is checked.)
        // Priority: gradle properties -PncVersionCode / -PncVersionName over the ledger file.
        // Property override is REQUIRED: actions/checkout checks out the triggering sha, which
        // does NOT contain the value just pushed by the preceding "version" job.
        val ncCode = (project.findProperty("ncVersionCode") as String?)?.toIntOrNull()
            ?: rootProject.file("builds/build_number.txt").readText().trim().toIntOrNull() ?: 1
        versionCode = ncCode
        versionName = (project.findProperty("ncVersionName") as String?)
            ?: "${ncCode / 100 + 1}.${ncCode % 100 / 10}.${ncCode % 10}"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    // Internal test build: release APK uses the standard Android debug keystore so it
    // can be installed on test devices (Redmi K40) without a production signing cert.
    // This is NOT for app store distribution.
    signingConfigs {
        create("release") {
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity)
}
