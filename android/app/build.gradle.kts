import java.io.ByteArrayOutputStream
import groovy.json.JsonSlurper

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android { namespace = "com.rustgram.app"; compileSdk = 35
    defaultConfig { applicationId = "com.rustgram.app"; minSdk = 26; targetSdk = 35; versionCode = 3; versionName = "0.3.1-beta" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release {
            // Development release artifact: installable without a separate
            // keystore. Replace with a production keystore before publishing.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    buildFeatures { compose = true }
    packaging { jniLibs { useLegacyPackaging = false } }
    sourceSets["main"].jniLibs.srcDir("../../target/aarch64-linux-android/release/jniLibs")
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")
}

tasks.register<Exec>("rustAndroidArm64") {
    val cargoOutput = ByteArrayOutputStream()
    workingDir("../..")
    commandLine("cargo", "build", "-p", "rustgram", "--release", "--target", "aarch64-linux-android", "--locked", "--message-format=json-render-diagnostics")
    standardOutput = cargoOutput
    doLast {
        val out = file("../../target/aarch64-linux-android/release/jniLibs/arm64-v8a")
        out.mkdirs()
        copy { from("../../target/aarch64-linux-android/release/librustgram.so"); into(out) }
        copy {
            val tdlibBuild = cargoOutput.toString(Charsets.UTF_8.name()).lineSequence()
                .filter { it.startsWith("{") }
                .map { JsonSlurper().parseText(it) as Map<*, *> }
                .firstOrNull {
                    it["reason"] == "build-script-executed" &&
                        it["package_id"].toString().contains("#tdlib-rs@")
                } ?: error("Cargo did not report the tdlib-rs build output")
            val tdjson = file("${tdlibBuild["out_dir"]}/tdlib/lib/libtdjson.so")
            check(tdjson.isFile) { "libtdjson.so was not produced by tdlib-rs: $tdjson" }
            from(tdjson)
            into(out)
        }
    }
}
tasks.named("preBuild") { dependsOn("rustAndroidArm64") }
