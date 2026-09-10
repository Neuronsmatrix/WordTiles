plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.wordtiles"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.wordtiles"
        minSdk = 26
        targetSdk = 35
        versionCode = providers.environmentVariable("WORDTILES_VERSION_CODE").map(String::toInt).orElse(2).get()
        versionName = providers.environmentVariable("WORDTILES_VERSION_NAME").orElse("0.2.0").get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    signingConfigs.getByName("debug") {
        val localKey = rootProject.file(".toolchain/android-user-home/debug.keystore")
        if (localKey.exists()) storeFile = localKey
    }
    val releaseSigningValues = listOf(
        "ANDROID_SIGNING_STORE_FILE", "ANDROID_KEYSTORE_PASSWORD", "ANDROID_KEY_ALIAS", "ANDROID_KEY_PASSWORD",
    ).associateWith { providers.environmentVariable(it).orNull }
    if (releaseSigningValues.values.all { !it.isNullOrBlank() }) {
        val releaseSigning = signingConfigs.create("release") {
            storeFile = file(releaseSigningValues.getValue("ANDROID_SIGNING_STORE_FILE")!!)
            storePassword = releaseSigningValues.getValue("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = releaseSigningValues.getValue("ANDROID_KEY_ALIAS")
            keyPassword = releaseSigningValues.getValue("ANDROID_KEY_PASSWORD")
        }
        buildTypes.getByName("release") { signingConfig = releaseSigning }
    }
    val verifyReleaseSigning = tasks.register("verifyReleaseSigning") {
        doLast {
            val missing = releaseSigningValues.filterValues { it.isNullOrBlank() }.keys
            check(missing.isEmpty()) { "Release signing requires: ${missing.joinToString()}. See docs/ci-cd.md." }
            check(file(releaseSigningValues.getValue("ANDROID_SIGNING_STORE_FILE")!!).isFile) {
                "Release signing keystore does not exist."
            }
        }
    }
    tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(verifyReleaseSigning) }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
dependencies {
    implementation(project(":core"))
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
