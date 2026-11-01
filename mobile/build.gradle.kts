import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    // Kotlin paketi/namespace ayrı; applicationId saat uygulamasıyla AYNI olmalı (Data Layer şartı, PLAN K1).
    namespace = "com.erenkng.akis.bridge"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.erenkng.akis"
        minSdk = libs.versions.minSdkMobile.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        // Paylaşılan DEBUG anahtarı (signing/README.md): CI'da ve her bilgisayarda aynı imza.
        // Data Layer, saat ve telefon uygulamasının aynı anahtarla imzalanmasını şart koşar;
        // yeni sürüm de eskisinin üstüne kaldırmadan kurulabilir. Play Store için KULLANILMAZ.
        getByName("debug") {
            storeFile = rootProject.file("signing/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            // BouncyCastle jar'larının çakışan meta dosyaları (saate kurulum, debug).
            excludes += setOf("META-INF/versions/9/OSGI-INF/MANIFEST.MF", "META-INF/BCKEY.DSA", "META-INF/BCKEY.SF")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

/**
 * Saat APK'sını telefon APK'sının içine gömer: `assets/wear/akis-wear.apk`.
 * Akış Köprü'deki "Saate yükle" ekranı bunu kablosuz ADB ile saate kurar (yalnızca debug).
 */
abstract class EmbedWearApkTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val wearApk: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun embed() {
        val target = outputDir.get().file("wear/akis-wear.apk").asFile
        target.parentFile.mkdirs()
        wearApk.get().asFile.copyTo(target, overwrite = true)
    }
}

androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        val embed = tasks.register<EmbedWearApkTask>("embed${variant.name.replaceFirstChar { it.uppercase() }}WearApk") {
            dependsOn(":wear:assembleDebug")
            wearApk.set(rootProject.layout.projectDirectory.file("wear/build/outputs/apk/debug/wear-debug.apk"))
        }
        variant.sources.assets?.addGeneratedSourceDirectory(embed, EmbedWearApkTask::outputDir)
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)

    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Saate kablosuz kurulum: yalnızca debug sürümünde (release çevrimdışı kalır, BRIEF Bölüm 11).
    debugImplementation(libs.libadb.android)
    debugImplementation(libs.conscrypt.android)
    debugImplementation(libs.bouncycastle.bcpkix)
    debugImplementation(libs.androidx.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    testImplementation(kotlin("test-junit"))
}
