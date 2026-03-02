import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

fun getProp(key: String): String {
    val prop = localProperties.getProperty(key)
    return if (!prop.isNullOrEmpty()) "\"$prop\"" else "\"\""
}

android {
    namespace = "com.antigravity.cryptowallet"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.antigravity.cryptowallet"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.2"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Enable multi-dex for Web3j
        multiDexEnabled = true

        buildConfigField("String", "ETHERSCAN_API_KEY", getProp("etherscan.api.key"))
        buildConfigField("String", "ARBISCAN_API_KEY", getProp("arbiscan.api.key"))
        buildConfigField("String", "BASESCAN_API_KEY", getProp("basescan.api.key"))
        buildConfigField("String", "BSCSCAN_API_KEY", getProp("bscscan.api.key"))
        buildConfigField("String", "POLYGONSCAN_API_KEY", getProp("polygonscan.api.key"))
        buildConfigField("String", "OPTIMISMSCAN_API_KEY", getProp("optimismscan.api.key"))
        buildConfigField("String", "WALLETCONNECT_PROJECT_ID", getProp("walletconnect.project.id"))
    }

    signingConfigs {
        create("localDebug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("localDebug")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("localDebug") // For demo purposes
        }
    }
    lint {
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/native-image/**"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // Icons
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Hilt - Dependency Injection
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-android-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.1.0")

    // Room - Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Web3j & Crypto (using lightweight core if possible, but standard is robust)
    implementation("org.web3j:core:4.10.3")
    implementation("org.bitcoinj:bitcoinj-core:0.16.2")
    
    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("io.coil-kt:coil-compose:2.5.0")

    // WalletConnect (Reown)
    implementation("com.walletconnect:android-core:2.1.2")
    implementation("com.walletconnect:web3wallet:1.1.2")
    implementation("org.bouncycastle:bcprov-jdk15to18:1.70")
    implementation("io.coil-kt:coil-compose:2.5.0")

    // WorkManager (Background sync)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // QR Code
    implementation("com.google.zxing:core:3.5.3")
}

configurations.all {
    exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
}
