import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
val backendBaseUrl = localProperties.getProperty("kkokkkok.backend.baseUrl", "http://10.0.2.2:8080")
val pilotAccessRequired = localProperties.getProperty("kkokkkok.pilot.required", backendBaseUrl.startsWith("https://").toString()).toBooleanStrict()
// Kakao native app key is a public client identifier (restricted by package name + signing key hash in the Kakao console).
val kakaoNativeAppKey = localProperties.getProperty("kakao.nativeAppKey", "").trim()
require(kakaoNativeAppKey.matches(Regex("[0-9a-fA-F]*"))) { "kakao.nativeAppKey must be the hex native app key" }
val loginRequired = localProperties.getProperty("kkokkkok.login.required", "false").toBooleanStrict()
require(!(loginRequired && pilotAccessRequired)) { "kkokkkok.login.required and kkokkkok.pilot.required cannot both be true" }
val escapedBackendBaseUrl = backendBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.kkokkkok.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kkokkkok.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BACKEND_BASE_URL", "\"$escapedBackendBaseUrl\"")
        buildConfigField("boolean", "PILOT_ACCESS_REQUIRED", pilotAccessRequired.toString())
        buildConfigField("boolean", "LOGIN_REQUIRED", loginRequired.toString())
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        manifestPlaceholders["kakaoScheme"] = if (kakaoNativeAppKey.isBlank()) "kakaounconfigured" else "kakao$kakaoNativeAppKey"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.exifinterface)
    implementation(libs.kakao.sdk.user)
    implementation(libs.androidx.browser)
    implementation(libs.kakao.maps)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
