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
                api(project(":Simulation"))
                api(project(":OfficialContent"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api("io.ktor:ktor-server-core:3.0.3")
                api("io.ktor:ktor-server-netty:3.0.3")
                api("io.ktor:ktor-server-cors:3.0.3")
                api("io.ktor:ktor-server-html-builder:3.0.3")
            }
        }
    }
}