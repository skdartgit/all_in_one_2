plugins {
    id("com.android.application")
}

android {
    namespace = "com.sanat.allinone"

    compileSdk = 37

    defaultConfig {
        applicationId = "com.sanat.allinone"

        minSdk = 23
        targetSdk = 37

        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }

        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core:1.16.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
}
