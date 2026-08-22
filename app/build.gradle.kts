import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.git.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.git.app"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "0.0.0.1"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
        resConfigs("zh", "en")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = null
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = null
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/versions/**/module-info.class",
                "META-INF/versions/**/OSGI-INF/MANIFEST.MF",
                "META-INF/OSGI-INF/**",
                "module-info.class",
                "META-INF/INDEX.LIST",
                "META-INF/*.RSA",
                "META-INF/*.DSA",
                "META-INF/*.SF",
                "META-INF/MANIFEST.MF",
                "plugin.properties",
                "**/plugin.properties",
                "/OSGI-INF/**",
                "**/OSGI-INF/**"
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.3.202401111512-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.apache:5.13.3.202401111512-r") {
        exclude(group = "org.apache.sshd", module = "sshd-osgi")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
