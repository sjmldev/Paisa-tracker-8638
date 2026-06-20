import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

fun loadKeystoreProperties(propertiesFile: java.io.File): Properties {
  val properties = Properties()
  if (propertiesFile.exists()) {
    propertiesFile.inputStream().use { properties.load(it) }
  }
  return properties
}

android {
  namespace = "com.example"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.paisatracker.app.pro"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystoreProperties = loadKeystoreProperties(rootProject.file("local.properties"))

      // Check local.properties first, then fallback to environment variables
      val keystorePath = keystoreProperties.getProperty("storeFile")
        ?: keystoreProperties.getProperty("signing.storeFile")
        ?: keystoreProperties.getProperty("keystore.file")
        ?: System.getenv("KEYSTORE_PATH")
        ?: "${rootDir}/my-upload-key.jks"
      val storePwd = keystoreProperties.getProperty("storePassword")
        ?: keystoreProperties.getProperty("signing.storePassword")
        ?: keystoreProperties.getProperty("keystore.password")
        ?: System.getenv("STORE_PASSWORD")
      val kAlias = keystoreProperties.getProperty("keyAlias")
        ?: keystoreProperties.getProperty("signing.keyAlias")
        ?: keystoreProperties.getProperty("keystore.alias")
        ?: System.getenv("KEY_ALIAS") ?: "upload"
      val kPassword = keystoreProperties.getProperty("keyPassword")
        ?: keystoreProperties.getProperty("signing.keyPassword")
        ?: keystoreProperties.getProperty("keystore.keyPassword")
        ?: System.getenv("KEY_PASSWORD")

      val hasKeystore = file(keystorePath).exists()
      if (hasKeystore && !storePwd.isNullOrEmpty() && !kPassword.isNullOrEmpty()) {
        storeFile = file(keystorePath)
        storePassword = storePwd
        keyAlias = kAlias
        keyPassword = kPassword
      } else {
        // Graceful fallback to debug signing keys when Play Store signing metadata is absent
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation("com.google.firebase:firebase-auth")
  implementation("com.google.firebase:firebase-database")
  implementation("com.google.firebase:firebase-messaging")
  implementation("com.google.firebase:firebase-analytics")
  // implementation("com.google.firebase:firebase-crashlytics")
  implementation("com.startapp:inapp-sdk:5.0.2")
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation("androidx.work:work-runtime-ktx:2.10.0")
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  // implementation(libs.androidx.room.ktx)
  // implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  // "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

afterEvaluate {
  val rootFolder = rootProject.rootDir
  val buildFolder = layout.buildDirectory.get().asFile

  tasks.findByName("assembleRelease")?.doLast {
    val releaseApkFolder = File(buildFolder, "outputs/apk/release")
    if (releaseApkFolder.exists()) {
      releaseApkFolder.listFiles()?.forEach { file ->
        if (file.name.endsWith(".apk")) {
          file.copyTo(File(rootFolder, "PaisaTracker_Release.apk"), overwrite = true)
        }
      }
    }
  }

  tasks.findByName("assembleDebug")?.doLast {
    val debugApkFolder = File(buildFolder, "outputs/apk/debug")
    if (debugApkFolder.exists()) {
      debugApkFolder.listFiles()?.forEach { file ->
        if (file.name.endsWith(".apk")) {
          file.copyTo(File(rootFolder, "PaisaTracker_Debug.apk"), overwrite = true)
        }
      }
    }
  }
}




