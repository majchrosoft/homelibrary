import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.googleServices)
}

val homelibraryVersionCode: Int =
    (project.findProperty("homelibrary.versionCode") as String).toInt()
val homelibraryVersionName: String =
    project.findProperty("homelibrary.versionName") as String

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    // Web (Wasm) target temporarily disabled — see shared/build.gradle.kts for the
    // rationale. Tracked in docs/RUN_LOCAL.md "What works" matrix.
    //
    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.napier)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.runtime)
            implementation(libs.koin.android)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.majchrosoft.homelibrary"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.majchrosoft.homelibrary"
        minSdk = 24
        targetSdk = 35
        versionCode = homelibraryVersionCode
        versionName = homelibraryVersionName
    }

    val keystoreEnvPath: String? = System.getenv("ANDROID_KEYSTORE_PATH")

    signingConfigs {
        // Only create the release signing config when the env vars are present
        // (i.e. on CI). Locally, `./gradlew assembleRelease` falls back to the
        // debug keystore so contributors don't need a real keystore to do a
        // release-flavor build smoke test.
        if (keystoreEnvPath != null) {
            create("release") {
                storeFile = file(keystoreEnvPath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/DEPENDENCIES")
    }
}

// Compose Multiplatform resources are auto-discovered from
// `composeApp/src/commonMain/composeResources/`. The `Res` class is generated
// as `com.majchrosoft.homelibrary.resources.Res` once the first resource lands.
// No explicit `compose.resources { ... }` block needed for the default layout.
