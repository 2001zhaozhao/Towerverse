import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvmToolchain(17)
    jvm {}
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {}
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // The official content is exposed a bit differently from mods,
                // not requiring a JSON serialization step to be loaded on the server since it is built-in.
                // However, the client will load it exactly like a mod.
                implementation(project(":ModdingSDK"))
            }
        }
    }
}