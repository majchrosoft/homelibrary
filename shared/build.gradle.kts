import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        // Treat warnings as errors in CI by toggling this flag from the command line.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm() // Useful for unit tests + future desktop target.

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            // Export Koin so SwiftUI can call into the DI container.
            export(libs.koin.core)
        }
    }

    // ------------------------------------------------------------------
    // Web (Wasm) target — temporarily disabled.
    //
    // GitLive's `dev.gitlive:firebase-*:2.1.0` does not publish a wasmJs
    // variant (only Android/iOS/JS/JVM). Keeping `wasmJs()` declared while
    // Firebase deps live in commonMain produces an unsolvable
    // "No matching variant of dev.gitlive:firebase-* was found" at the
    // `:kotlinNpmInstall` step. Two ways to bring web back:
    //
    //   1. Wait for / track GitLive Firebase wasmJs support, then re-enable
    //      this block as-is.
    //   2. Refactor source sets so Firebase is only on android+ios+jvm
    //      (intermediate `nonWebMain` source set), and the web target uses
    //      the Firebase JS SDK directly via `external` interop. Substantial
    //      work — schedule it as its own change.
    //
    // Until then: legacy Angular app at ~/Documents/projects/majchrosoft/
    // home-library remains the production web frontend.
    // ------------------------------------------------------------------
    //
    // @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
    // wasmJs {
    //     moduleName = "shared"
    //     browser {
    //         commonWebpackConfig {
    //             outputFileName = "shared.js"
    //         }
    //     }
    //     binaries.executable()
    // }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.atomicfu)

            implementation(libs.koin.core)

            implementation(libs.firebase.common)
            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.database)
            implementation(libs.firebase.storage)

            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.napier)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        // wasmJsMain dependencies disabled along with the wasmJs() target above.
    }
}

android {
    namespace = "com.majchrosoft.homelibrary.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
