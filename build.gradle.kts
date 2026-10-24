buildscript {
    // The Android Gradle Plugin is only put on the classpath when the Android app is part of the
    // build (-Pkiez.android=true). It lives in Google's Maven repository, which some build
    // environments cannot reach; the core and desktop preview build without it.
    if (project.findProperty("kiez.android") == "true") {
        repositories {
            google()
            mavenCentral()
        }
        dependencies {
            classpath("com.android.tools.build:gradle:8.10.1")
        }
    }
}

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
