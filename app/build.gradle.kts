plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val releaseSigning = mapOf(
    "storeFile" to System.getenv("EASYPHOTO_KEYSTORE"),
    "storePassword" to System.getenv("EASYPHOTO_STORE_PASSWORD"),
    "keyAlias" to System.getenv("EASYPHOTO_KEY_ALIAS"),
    "keyPassword" to System.getenv("EASYPHOTO_KEY_PASSWORD"),
)
val hasReleaseSigning = releaseSigning.values.all { !it.isNullOrBlank() }
if (releaseSigning.values.any { !it.isNullOrBlank() } && !hasReleaseSigning) {
    throw GradleException("Release signing requires all four EASYPHOTO_* signing environment variables; see docs/development.md")
}

android {
    namespace = "com.kevin.easyphoto"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kevin.easyphoto"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseSigning.getValue("storeFile")!!)
                storePassword = releaseSigning.getValue("storePassword")
                keyAlias = releaseSigning.getValue("keyAlias")
                keyPassword = releaseSigning.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}

configurations.configureEach {
    resolutionStrategy {
        force("androidx.lifecycle:lifecycle-runtime-ktx:${libs.versions.lifecycle.get()}")
        force("androidx.lifecycle:lifecycle-runtime-compose:${libs.versions.lifecycle.get()}")
        force("androidx.lifecycle:lifecycle-viewmodel-compose:${libs.versions.lifecycle.get()}")
    }
}
