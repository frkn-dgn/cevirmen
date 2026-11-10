plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// GitHub Actions her derlemede artan bir numara verir; guncelleme icin versionCode artmali
val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.ufuk.cevirmen"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ufuk.cevirmen"
        minSdk = 26
        // 34: Android 15'in zorunlu kenardan kenara cizimi devreye girmesin
        targetSdk = 34
        versionCode = buildNumber
        versionName = "1.$buildNumber"
    }

    signingConfigs {
        create("release") {
            // Kisisel uygulama: sabit anahtar sayesinde yeni APK eskisinin uzerine kurulur
            storeFile = file("cevirmen.jks")
            storePassword = "cevirmen123"
            keyAlias = "cevirmen"
            keyPassword = "cevirmen123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.webkit:webkit:1.12.1")
}
