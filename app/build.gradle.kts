import com.android.build.gradle.internal.tasks.FinalizeBundleTask
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.googleGms)
    alias(libs.plugins.googleFirebaseCrashlytics)
}

android {
    namespace = "com.rycbar.rehearse"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rycbar.rehearse"
        minSdk = 26
        targetSdk = 34
        versionName = "1.0.0.2"
        versionCode = versionName?.split('.')?.fold(0) { acc, literal -> acc * 100 + literal.toInt() } ?: 1

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val finalName = "${applicationId?.replace(".", "_")}_$versionName($versionCode)"

        android.applicationVariants.all variant@{
            tasks.named<FinalizeBundleTask>("sign${name.capitalizeAsciiOnly()}Bundle") {
                val file = finalBundleFile.asFile.get()
                val suffix = file.extension
                val finalFile = File(file.parentFile, "$finalName.$suffix")
                finalBundleFile.set(finalFile)
            }
        }

        // change app name block below
        buildOutputs.all {
            val variantOutputImpl = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val suffix = variantOutputImpl.outputFileName.split(".").last()
            val outputFilename = "$finalName.$suffix"
            variantOutputImpl.outputFileName = outputFilename
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.google.firebase))
    implementation(libs.google.firebase.crashlytics)
    implementation(libs.google.firebase.analytics)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}