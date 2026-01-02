import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm() {
        withJava()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":library"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.koin.core.jvm)
            implementation(libs.bundles.voyager)

            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.2")

            implementation("org.slf4j:slf4j-simple:1.6.1")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.mcal.nmmp"
            packageVersion = "1.4.0"
            description = "Android DEX Protector"
            copyright = "© 2023-2026 timscriptov."
            vendor = "timscriptov"
        }
        buildTypes.release.proguard {
            version.set("7.3.2")
            configurationFiles.from("desktop-rules.pro")
            isEnabled.set(true)
            obfuscate.set(true)
        }
        jvmArgs("-Djdk.util.zip.disableZip64ExtraFieldValidation=true")
    }
}
