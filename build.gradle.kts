group = "cool.yz.towerverse"
version = "0.0.1-SNAPSHOT"

plugins {
    kotlin("multiplatform") apply false
    kotlin("plugin.serialization") apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
    
    group = "cool.yz.towerverse"
    version = "0.0.1-SNAPSHOT"
}