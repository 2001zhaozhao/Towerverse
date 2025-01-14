import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvmToolchain(17)
    jvm {}

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":ModdingSDK"))
                // Here, the example mod loads the official content.
                // One can load another mod just as easily by using its project as a Gradle dependency.
                api(project(":OfficialContent"))
            }
        }
    }
}