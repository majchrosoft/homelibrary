plugins {
    // Apply false at the root — modules opt-in.
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

allprojects {
    apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)
    apply(plugin = rootProject.libs.plugins.detekt.get().pluginId)

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.4.1")
        android.set(true)
        ignoreFailures.set(false)
        reporters {
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.SARIF)
        }
        filter {
            exclude { it.file.path.contains("build/") }
            exclude { it.file.path.contains("generated/") }
        }
    }

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        autoCorrect = false
        parallel = true
    }
}

// --- Firebase Configuration Generation ---

val generateFirebaseConfig by tasks.registering {
    group = "configuration"
    description = "Generates platform-specific Firebase configuration files from .env"

    val envFile = rootProject.file(".env")
    // Avoid marking missing file as input to prevent task failure
    if (envFile.exists()) {
        inputs.file(envFile)
    }
    
    val androidDest = rootProject.file("composeApp/google-services.json")
    val iosDest = rootProject.file("iosApp/iosApp/GoogleService-Info.plist")
    val webDest = rootProject.file("composeApp/src/wasmJsMain/resources/firebase-config.js")
    
    outputs.files(androidDest, iosDest, webDest)

    doLast {
        if (!envFile.exists()) {
            logger.warn(".env file not found at ${envFile.absolutePath}. Skipping Firebase config generation.")
            return@doLast
        }

        val env = envFile.readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { it.split("=", limit = 2) }
            .filter { it.size == 2 }
            .associate { it[0].trim() to it[1].trim() }

        fun getEnv(key: String) = env[key] ?: System.getenv(key) ?: "REPLACE_ME"

        // Android (google-services.json)
        androidDest.writeText("""
{
  "project_info": {
    "project_number": "${getEnv("FIREBASE_PROJECT_NUMBER")}",
    "project_id": "${getEnv("FIREBASE_PROJECT_ID")}",
    "storage_bucket": "${getEnv("FIREBASE_STORAGE_BUCKET")}"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "${getEnv("FIREBASE_ANDROID_APP_ID")}",
        "android_client_info": {
          "package_name": "${getEnv("FIREBASE_ANDROID_PACKAGE_NAME")}"
        }
      },
      "api_key": [
        {
          "current_key": "${getEnv("FIREBASE_API_KEY")}"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": []
        }
      }
    }
  ],
  "configuration_version": "1"
}
        """.trimIndent())

        // iOS (GoogleService-Info.plist)
        iosDest.writeText("""
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>API_KEY</key>
	<string>${getEnv("FIREBASE_API_KEY")}</string>
	<key>GCM_SENDER_ID</key>
	<string>${getEnv("FIREBASE_MESSAGING_SENDER_ID")}</string>
	<key>PLIST_VERSION</key>
	<string>1</string>
	<key>BUNDLE_ID</key>
	<string>${getEnv("FIREBASE_IOS_BUNDLE_ID")}</string>
	<key>PROJECT_ID</key>
	<string>${getEnv("FIREBASE_PROJECT_ID")}</string>
	<key>STORAGE_BUCKET</key>
	<string>${getEnv("FIREBASE_STORAGE_BUCKET")}</string>
	<key>IS_ADS_ENABLED</key>
	<false/>
	<key>IS_ANALYTICS_ENABLED</key>
	<false/>
	<key>IS_APPINVITE_ENABLED</key>
	<true/>
	<key>IS_GCM_ENABLED</key>
	<true/>
	<key>IS_SIGNIN_ENABLED</key>
	<true/>
	<key>GOOGLE_APP_ID</key>
	<string>${getEnv("FIREBASE_IOS_APP_ID")}</string>
	<key>DATABASE_URL</key>
	<string>${getEnv("FIREBASE_DATABASE_URL")}</string>
</dict>
</plist>
        """.trimIndent())

        // Web (firebase-config.js)
        webDest.parentFile.mkdirs()
        webDest.writeText("""
// Generated from .env via Gradle generateFirebaseConfig task
window.__FIREBASE_CONFIG__ = {
    apiKey: "${getEnv("FIREBASE_API_KEY")}",
    authDomain: "${getEnv("FIREBASE_AUTH_DOMAIN")}",
    databaseURL: "${getEnv("FIREBASE_DATABASE_URL")}",
    projectId: "${getEnv("FIREBASE_PROJECT_ID")}",
    storageBucket: "${getEnv("FIREBASE_STORAGE_BUCKET")}",
    messagingSenderId: "${getEnv("FIREBASE_MESSAGING_SENDER_ID")}",
    appId: "${getEnv("FIREBASE_WEB_APP_ID")}"
};
        """.trimIndent())
        
        logger.lifecycle("Firebase configuration files generated successfully.")
    }
}

// Hook into the build process
subprojects {
    afterEvaluate {
        tasks.findByName("preBuild")?.dependsOn(generateFirebaseConfig)
        // For iOS, xcodegen or other tasks might need it
    }
}

// --- End Firebase Configuration Generation ---

// `clean` is provided by LifecycleBasePlugin (pulled in by detekt via the
// `allprojects` block above). On Gradle 8.x manually re-registering a Delete
// task with the same name was tolerated; on Gradle 9.x it errors with
// "Cannot add task 'clean' as a task with that name already exists." If you
// need extra paths cleaned, use:
//
//   tasks.named<Delete>("clean") { delete(...) }
//
// rather than `tasks.register`.
