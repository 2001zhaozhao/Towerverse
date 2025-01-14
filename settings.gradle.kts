rootProject.name = "Towerverse"

include(
    ":Common", // Multiplatform common module, shared between server and client (simulation) as well as modding SDK
    ":Simulation", // Multiplatform simulation module, shared between server and client, using types from common module
    ":Server", // JVM server module
    ":Client", // Web-based client module which uses Kotlin/WASM and DOM API
    ":ModdingSDK", // Modding SDK. Mods (and official content) for the game are Kotlin projects that depend on this SDK
    ":OfficialContent", // Official game content. This is built-in to the game client and server
    ":ExampleMod", // An example mod that can be compiled into a JSON mod file and loaded by the client and server.
)

pluginManagement {
    repositories {
        gradlePluginPortal()
    }

    plugins {
        kotlin("multiplatform") version "2.1.0"
        kotlin("plugin.serialization") version "2.1.0"
    }
}