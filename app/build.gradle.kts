plugins { alias(libs.plugins.android.application); alias(libs.plugins.kotlin.android) }
android {
    namespace = "pl.vitaltrack"
    compileSdk = 36
    defaultConfig {
        applicationId = "pl.vitaltrack"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
    buildTypes { release { isMinifyEnabled = true; isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
    buildFeatures { buildConfig = false }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_1_8; targetCompatibility = JavaVersion.VERSION_1_8 }
    kotlinOptions { jvmTarget = "1.8" }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    // Health Connect (androidx) — odkomentuj w środowisku z Mavenem, docs/HEALTH_CONNECT.md
    // implementation(libs.androidx.health.connect)
    testImplementation(libs.junit)
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:analytics"))
    implementation(project(":core:testing"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:database"))
    implementation(project(":core:settings"))
    implementation(project(":core:security"))
    implementation(project(":core:bluetooth"))
    implementation(project(":core:healthconnect"))
    implementation(project(":core:sync"))
    implementation(project(":core:log"))
    implementation(project(":device:api"))
    implementation(project(":device:amazfit"))
    implementation(project(":feature:today"))
    implementation(project(":feature:activity"))
    implementation(project(":feature:heartrate"))
    implementation(project(":feature:sleep"))
    implementation(project(":feature:devices"))
    implementation(project(":feature:trends"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:onboarding"))
}
