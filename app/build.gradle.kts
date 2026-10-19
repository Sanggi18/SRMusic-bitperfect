plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
}

ksp {
  arg("room.generateKotlin", "true")
}

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.sr.pccsim"
    minSdk = 26
    targetSdk = 36
    versionCode = 6
    versionName = "1.1.0-beta4"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    ndk {
      abiFilters.clear()
      abiFilters += listOf("arm64-v8a")
    }

    externalNativeBuild {
      cmake {
        arguments("-DANDROID_STL=c++_shared")
      }
    }
  }

  androidResources {
    localeFilters += listOf("en", "id", "in")
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
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
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      isDebuggable = true
      isMinifyEnabled = false
      isShrinkResources = false
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }

  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
      version = "3.22.1"
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
    prefab = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
  packaging {
    jniLibs {
      useLegacyPackaging = false
    }
    resources {
      excludes += listOf(
        "META-INF/*.version",
        "META-INF/DEPENDENCIES",
        "META-INF/LICENSE*",
        "META-INF/NOTICE*",
        "META-INF/*.kotlin_module",
        "DebugProbesKt.bin"
      )
    }
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)
  implementation(libs.taglib)
  implementation(libs.androidx.palette.ktx)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.session)
  implementation(libs.androidx.media3.common)
  implementation(libs.jellyfin.media3.ffmpeg.decoder)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.oboe)

  testImplementation(libs.junit)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)

  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
