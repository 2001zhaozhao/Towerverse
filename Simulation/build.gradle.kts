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
                api(project(":Common"))

                // Crypto library for taking a hash of the game state
                implementation("org.kotlincrypto.hash:sha2:0.5.6")
            }
        }

        val jvmMain by getting
        val wasmJsMain by getting
    }
}