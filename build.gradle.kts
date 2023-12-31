import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven { url = uri("https://www.jitpack.io") }
}

kotlin {
    jvm {
        jvmToolchain(17)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)

                implementation(project(":library"))

                implementation("com.github.TimScriptov:preferences:1.0.1")
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "nmmp"
            packageVersion = "1.3.0"
            description = "Android APK Protector"
            copyright = "© 2023 timscriptov."
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
