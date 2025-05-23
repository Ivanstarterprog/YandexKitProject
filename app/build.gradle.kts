import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.yandexkitproject"
    compileSdk = 35


    defaultConfig {
        applicationId = "com.example.yandexkitproject"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val properties = Properties()
        file("../local.properties").inputStream().use {
            properties.load(it)
        }

        val apiKey = properties.getProperty("YANDEX_MAPKIT_API_KEY")
        buildConfigField(type = "String", name = "YANDEX_API_KEY", value = "\"$apiKey\"")
        manifestPlaceholders["yandexMapKitApiKey"] = apiKey

    }

    buildFeatures {
        viewBinding=true
        buildConfig=true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.maps.mobile)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.ktx.v1120)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}