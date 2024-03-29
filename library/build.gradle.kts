import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    java
    id("maven-publish")
    kotlin("jvm")
    id("com.google.protobuf") version "0.8.13"
}

sourceSets {
    getByName("main").java {
        srcDir("build/generated/source/proto/main/java")
    }
}

dependencies {
    implementation("com.github.timscriptov:apkparser:1.2.7")
    implementation("com.github.timscriptov:preferences:1.0.4")
    implementation("com.android.tools.smali:smali-dexlib2:3.0.3")
    implementation("org.jetbrains:annotations:24.0.1")
    implementation("com.google.guava:guava:32.0.0-jre")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.ow2.asm:asm:9.5")
    implementation("com.android.tools:r8:8.1.56")
    implementation("com.google.protobuf:protobuf-java:3.22.2")
    implementation("com.google.protobuf:protobuf-java:3.19.6")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.19.6"
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.mcal"
            artifactId = "nmmp"
            version = "1.3.2"

            afterEvaluate {
                from(components["java"])
            }
        }
    }
}
