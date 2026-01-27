import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.kapt)
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("/Users/sahassawat/Documents/Android/KeyStore/key.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.indybrain.indypos_Android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.indybrain.indypos_Android"
        minSdk = 26
        targetSdk = 36
        versionCode = 20
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String?
            keyPassword = keystoreProperties["keyPassword"] as String?
            storeFile = keystoreProperties["storeFile"]?.let { file(it) }
            storePassword = keystoreProperties["storePassword"] as String?
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
    
    // Disable baseline profile for development builds
    androidResources {
        ignoreAssetsPattern = "!.svn:!.git:.*:!CVS:!thumbs.db:!picasa.ini:!*.scc:*~"
    }
    
    flavorDimensions += "environment"
    
    productFlavors {
        create("dev") {
            dimension = "environment"
//            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "INDYPOS Dev")
            
            buildConfigField("String", "BASE_API_URL", "\"https://dev.indy-pos.com/api/v1/\"")
            buildConfigField("String", "BASE_IMAGE_URL", "\"https://dev.indy-pos.com\"")
            buildConfigField("String", "ENVIRONMENT_NAME", "\"Development\"")
        }
        
        create("stg") {
            dimension = "environment"
//            applicationIdSuffix = ".stg"
            resValue("string", "app_name", "INDYPOS Staging")
            
            buildConfigField("String", "BASE_API_URL", "\"https://stg.indy-pos.com/api/v1/\"")
            buildConfigField("String", "BASE_IMAGE_URL", "\"https://stg.indy-pos.com\"")
            buildConfigField("String", "ENVIRONMENT_NAME", "\"Staging\"")
        }
        
        create("prod") {
            dimension = "environment"
            resValue("string", "app_name", "INDYPOS")
            
            buildConfigField("String", "BASE_API_URL", "\"https://indy-pos.com/api/v1/\"")
            buildConfigField("String", "BASE_IMAGE_URL", "\"https://indy-pos.com\"")
            buildConfigField("String", "ENVIRONMENT_NAME", "\"Production\"")
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    
    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.foundation.layout.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    
    // ViewModel
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    
    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)
    
    // Navigation Compose
    implementation(libs.navigation.compose)
    
    // Pager for swipe gestures
    implementation(libs.androidx.foundation)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    
    // Coil for image loading
    implementation(libs.coil.compose)
    
    // ExifInterface for image orientation
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    
    // Accompanist SwipeRefresh for pull-to-refresh
    implementation(libs.accompanist.swiperefresh)
    
    // CameraX
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    
    // ML Kit Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    
    // Permission handling
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    
    // ZXing for QR code generation
    implementation("com.google.zxing:core:3.5.2")
    
    // Printer Library
    implementation(files("libs/printer-lib-3.2.0.aar"))
    
    // CSV Export
    implementation("com.opencsv:opencsv:5.9")
    
    // Excel Export
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    implementation("org.apache.poi:poi:5.2.5")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}