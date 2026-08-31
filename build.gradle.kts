ext.apply {
    set("compileSdk", 35)
    set("targetSdk", 35)
    set("minSdk", 26)
    set("ndkVersion", "27.0.12077973")
    set("cmakeVersion", "3.22.1")
    set("ktx_version", "1.12.0")
    set("stdlib_version", "1.8.22")
    set("hiddenapibypass", "4.3")
    set("xcrashversion", "3.0.0")
    set("shadowhook", "1.0.8")
    set("googlematerial", "1.11.0")
}

buildscript {
    dependencies {
        classpath(libs.gradle)
    }
}

plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.agp.lib) apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    alias(libs.plugins.materialthemebuilder) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
