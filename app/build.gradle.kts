plugins {
    alias(libs.plugins.android.application)
    // Add the dependency for the Google services Gradle plugin
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.semana4distribuidoraalimentos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.semana4distribuidoraalimentos"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("androidx.cardview:cardview:1.0.0")
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    // Firebase Authentication y Realtime Database
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    // Ubicación (Google Play Services Location)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // Si usas CardView/Material en la UI
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.gms:play-services-auth:21.1.0")
    implementation("com.google.firebase:firebase-auth:23.0.0")
    implementation("com.google.android.material:material:1.12.0")

}