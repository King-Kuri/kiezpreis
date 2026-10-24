plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
}

// Development host: compiles the shared Compose UI against Compose Desktop (Maven Central only)
// so the UI can be type-checked, tested and screenshotted without the Android SDK.
// Not shipped to users. See DECISIONS.md (D-03).
sourceSets {
    main {
        kotlin.srcDir("../shared-ui/src")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

val cmp = "1.5.12"

dependencies {
    implementation(project(":core"))
    implementation("org.jetbrains.compose.desktop:desktop-jvm-linux-x64:$cmp")
    implementation("org.jetbrains.compose.material3:material3-desktop:$cmp")
    implementation("org.jetbrains.compose.material:material-icons-extended-desktop:$cmp")
    implementation(libs.coroutines.swing)

    testImplementation("org.jetbrains.compose.ui:ui-test-junit4-desktop:$cmp")
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
}

tasks.register<JavaExec>("screenshots") {
    group = "verification"
    description = "Renders every screen in light and dark mode to build/screenshots"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("de.kiezpreis.desktop.ScreenshotsKt")
    args(layout.buildDirectory.dir("screenshots").get().asFile.absolutePath)
}

// Compose Multiplatform 1.6.2 is the newest line whose desktop artifacts do not need androidx
// artifacts from Google's Maven (unreachable in the build container). Pin every compose module.
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group.startsWith("org.jetbrains.compose")) useVersion(cmp)
    }
}
