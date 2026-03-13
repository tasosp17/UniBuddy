plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services) //firebase authentication
}

android {
    namespace = "com.it2021084.unibuddy"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.it2021084.unibuddy"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/LICENSE.md"
        }
    }
}

dependencies {

    dependencies {
        // --- AndroidX Core ---
        implementation("androidx.core:core-ktx:1.12.0")
        implementation("androidx.appcompat:appcompat:1.6.1")
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")
        implementation("androidx.legacy:legacy-support-v4:1.0.0")
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.ui.graphics)
        implementation(libs.androidx.compose.ui.tooling.preview)
        implementation(libs.androidx.compose.material3)
        implementation(libs.androidx.material3)
        implementation(libs.androidx.recyclerview)
        implementation("com.google.android.material:material:1.10.0")

        // --- Google Material ---
        implementation("com.google.android.material:material:1.12.0")

        // --- Google Sign-In ---
        implementation("com.google.android.gms:play-services-auth:21.4.0")
        implementation(libs.androidx.credentials)
        implementation(libs.androidx.credentials.play.services.auth)
        implementation(libs.googleid)

        // --- Firebase (using BoM for version alignment) ---
        implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
        implementation("com.google.firebase:firebase-auth-ktx")
        implementation("com.google.firebase:firebase-database-ktx")
        implementation("com.google.firebase:firebase-storage-ktx")

        // --- Glide for images ---
        implementation("com.github.bumptech.glide:glide:4.16.0")
        annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

        // --- Testing ---
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.compose.ui.test.junit4)
        debugImplementation(libs.androidx.compose.ui.tooling)
        debugImplementation(libs.androidx.compose.ui.test.manifest)

        // Firebase Cloud Messaging
        implementation("com.google.firebase:firebase-messaging:23.4.0")

        // Volley (for HTTP requests)
        implementation("com.android.volley:volley:1.2.1")

        // Google Auth (To read the JSON key and create the token)
        implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0") {
            exclude(group = "org.apache.httpcomponents", module = "httpclient")
        }

        // JavaMail API for sending emails in background
        implementation("com.sun.mail:android-mail:1.6.7")
        implementation("com.sun.mail:android-activation:1.6.7")
    }

}