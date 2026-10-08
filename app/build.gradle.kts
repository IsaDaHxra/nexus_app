import java.util.Properties
plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
val local = Properties().apply { rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) } }
android {
    namespace = "com.nexus.mobile"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.nexus.mobile"; minSdk = 26; targetSdk = 35
        versionCode = 1; versionName = "1.0.0"
        val key = local.getProperty("COMIC_VINE_API_KEY", "").replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "COMIC_VINE_API_KEY", "\"$key\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildTypes { release { isMinifyEnabled = false; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.01.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui"); implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation"); implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("io.coil-kt:coil-compose:2.7.0"); implementation("io.coil-kt:coil-svg:2.7.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0"); implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("androidx.media3:media3-exoplayer:1.5.1"); implementation("androidx.media3:media3-ui:1.5.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
