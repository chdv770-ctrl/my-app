plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val ksAlias: String? = System.getenv("KS_ALIAS")
val buildNumber: Int = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.chdv.haboox"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.chdv.haboox"
        minSdk = 26
        targetSdk = 30
        versionCode = 100 + buildNumber
        versionName = "2.0.$buildNumber"
    }

    signingConfigs {
        if (ksAlias != null) {
            create("fixed") {
                storeFile = file("haboox.p12")
                storeType = "pkcs12"
                storePassword = "haboox123"
                keyAlias = ksAlias
                keyPassword = "haboox123"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (ksAlias != null) signingConfigs.getByName("fixed") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
