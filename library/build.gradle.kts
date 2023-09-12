plugins {
    java
    id("maven-publish")
    kotlin("jvm")
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://www.jitpack.io" ) }
}

dependencies {
    implementation("com.github.TimScriptov:apkparser:1.2.4")
    implementation("com.github.TimScriptov:preferences:1.0.1")
    implementation("com.android.tools.smali:smali-dexlib2:3.0.3")
    implementation("org.jetbrains:annotations:24.0.1")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.ow2.asm:asm:9.5")
    implementation("com.android.tools:r8:8.1.56")
    implementation("com.android:zipflinger:8.1.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.mcal"
            artifactId = "nmmp"
            version = "1.2.4"

            afterEvaluate {
                from(components["java"])
            }
        }
    }
}
