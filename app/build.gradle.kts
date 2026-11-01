plugins {
    id("com.android.application")
}

android {
    namespace = "com.hemogestion.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hemogestion.app"
        minSdk = 24            // Android 7.0 (Nougat)
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        // URL de la API: se cambia en gradle.properties (hemo.apiBaseUrl), no acá.
        buildConfigField("String", "API_BASE_URL", "\"${project.property("hemo.apiBaseUrl")}\"")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")

    // Pruebas unitarias (en la PC, sin emulador). org.json real: el de Android solo trae "stubs" que fallan en estas pruebas.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20231013")
}
