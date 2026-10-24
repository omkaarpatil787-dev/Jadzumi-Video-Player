import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Local properties are optional and ignored by Git; environment variables take precedence.
val keystoreProperties = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
fun signingValue(env: String, property: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(property)?.takeIf { it.isNotBlank() }
val releaseStoreFile = signingValue("ANDROID_KEYSTORE_FILE", "storeFile")
val releaseStorePassword = signingValue("ANDROID_KEYSTORE_PASSWORD", "storePassword")
val releaseKeyAlias = signingValue("ANDROID_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = signingValue("ANDROID_KEY_PASSWORD", "keyPassword")
val hasReleaseSigning = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
    .all { it != null }
if (!hasReleaseSigning) {
    logger.warn("Release signing is not configured: release output will be UNSIGNED. " +
        "Set ANDROID_KEYSTORE_FILE, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, " +
        "ANDROID_KEY_PASSWORD or the matching keystore.properties entries.")
}

android {
    namespace = "com.jadzumi.video"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jadzumi.video"
        minSdk = 26
        targetSdk = 35
        versionCode = 20
        versionName = "1.4.0-alpha.4"
        ndk { abiFilters += "arm64-v8a" }
    }

    sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/licenseAssets"))

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
        getByName("debug") {
            storeFile = rootProject.file("signing/development.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else null
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation("io.github.anilbeesetti:nextlib-media3ext:1.11.1-0.16.0")
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation("junit:junit:4.13.2")
}

val generateLicenseAssets by tasks.registering(Copy::class) {
    from(rootProject.projectDir) {
        include("LICENSE", "THIRD_PARTY_LICENSES.md")
    }
    from(rootProject.file("licenses")) { into("licenses") }
    into(layout.buildDirectory.dir("generated/licenseAssets"))
}
tasks.named("preBuild").configure { dependsOn(generateLicenseAssets) }
