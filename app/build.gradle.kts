plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
    alias(libs.plugins.secrets.gradle.plugin)
}

android {
    namespace = "com.example.learnify"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.learnify"
        minSdk = 24
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
    buildFeatures {
        buildConfig = true
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

    implementation(libs.viewpager2)
    implementation(libs.gson)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.auth)
    implementation(libs.google.ai.client)
    implementation(libs.pdfbox.android)
    implementation(libs.okhttp)
    implementation(libs.guava)
    implementation(libs.listenablefuture)
    implementation(libs.play.services.mlkit.text.recognition)
    implementation(libs.jsoup)
    implementation(libs.pdfbox.android)
    implementation(libs.okhttp)
    implementation(libs.play.services.mlkit.text.recognition.v1900)
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.10.0")
}