plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.editlyai.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.editlyai.app"
        minSdk = 29 // Android 10+
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables { useSupportLibrary = true }
    }

    // NOT: Release build, GitHub Actions'ta test amaçlı olarak AGP'nin
    // otomatik oluşturduğu debug keystore ile imzalanır (böylece keystore
    // sırrı olmadan da kurulabilir bir APK üretilir). Play Store'a
    // yüklemeden önce mutlaka KENDİ release keystore'unuzla imzalayın
    // (bkz. README.md -> "Play Store için release imzalama").
    buildTypes {
        debug {
            isMinifyEnabled = false
            // NOT: applicationIdSuffix ".debug" KASITLI OLARAK KALDIRILDI.
            // Google Services (google-services.json) paket adına göre eşleşme
            // arar; suffix eklenirse "com.editlyai.app.debug" için de ayrı bir
            // client girdisi gerekirdi. Basitlik için debug/release aynı
            // applicationId'yi kullanıyor (aynı anda cihaza kurulamazlar).
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug") // AGP'nin otomatik debug keystore'u
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        // Material3'teki TopAppBar, RangeSlider gibi bazı bileşenler
        // @ExperimentalMaterial3Api ile işaretli; opt-in yapılmazsa Kotlin
        // derleme HATASI verir (sadece uyarı değil).
        freeCompilerArgs += listOf("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.material:material:1.12.0") // Theme.MaterialComponents için
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // CameraX
    val camerax = "1.3.4"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")

    // ML Kit - OCR (Türkçe dahil Latin alfabesi metin tanıma)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // Coil - görsel yükleme
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Google Play Billing
    implementation("com.android.billingclient:billing-ktx:7.1.1")

    // AdMob
    implementation("com.google.android.gms:play-services-ads:23.2.0")

    // Firebase (kimlik doğrulama + abonelik doğrulama backend'i için altyapı)
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Media3 Transformer/Effect - video üzerine metin katmanı "yakma" (export)
    // için. Resmi Android video düzenleme API'si (ExoPlayer'ın parçası).
    val media3 = "1.4.1"
    implementation("androidx.media3:media3-transformer:$media3")
    implementation("androidx.media3:media3-effect:$media3")
    implementation("androidx.media3:media3-common:$media3")
    implementation("com.google.guava:guava:33.2.1-android")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
