plugins {
    id("com.android.application")
    id("com.google.gms.google-services") // Bắt buộc để Firebase nhận google-services.json
}

android {
    namespace = "com.calmpuchia.userapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.calmpuchia.userapp"
        minSdk = 26
        targetSdk = 35
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
    // ✅ Firebase BoM: quản lý version đồng bộ
    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))

    // ✅ Firebase SDK (không cần version nếu dùng BoM)
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")

    // ✅ AndroidX & UI
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.gridlayout)

    // ✅ Google Sign-In & Credentials API
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation("com.google.firebase:firebase-database:20.3.0")
    implementation(libs.play.services.analytics.impl)
    // ✅ Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.google.firebase:firebase-firestore")
    implementation ("com.google.android.material:material:1.8.0")
    implementation ("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
        // Firebase
    implementation ("com.google.firebase:firebase-auth:21.1.0")
    implementation ("com.google.firebase:firebase-database:20.1.0")
    implementation ("com.google.firebase:firebase-storage:20.1.0")

        // Image handling
    implementation ("com.github.bumptech.glide:glide:4.14.2")
    implementation ("com.github.yalantis:ucrop:2.2.6")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
        // Material Design
    implementation ("com.google.android.material:material:1.8.0")
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
    // For HTTP requests (OkHttp)
    implementation ("com.squareup.okhttp3:okhttp:4.10.0")
    // RecyclerView và CardView
    implementation ("androidx.recyclerview:recyclerview:1.2.1")
    implementation ("androidx.cardview:cardview:1.0.0")
}

