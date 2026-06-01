plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.holderzone.foundation.core"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = false
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }

        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api(libs.androidx.fragment.ktx)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.viewmodel.ktx)
    api(libs.androidx.navigation.fragment.ktx)
    api(libs.androidx.recyclerview)
    api(libs.kotlinx.coroutines.android)
    api(libs.okhttp)
    api(libs.retrofit)
    api(libs.mmkv)
    api(libs.kotlinx.serialization.json)
    api(libs.permissionx)
    api(libs.github.logbook) {
        exclude(group = "com.google.code.gson", module = "gson")
    }
    implementation(libs.github.voice)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
