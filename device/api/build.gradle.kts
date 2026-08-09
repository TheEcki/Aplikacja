import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins { alias(libs.plugins.kotlin.jvm) }
java { sourceCompatibility = JavaVersion.VERSION_1_8; targetCompatibility = JavaVersion.VERSION_1_8 }
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_1_8) } }
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
    implementation(project(":core:common"))
    implementation(project(":core:model"))
}
