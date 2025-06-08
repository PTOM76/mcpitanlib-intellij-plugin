import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij") version "1.17.4"
    id("java")
    kotlin("jvm") version "1.9.25"
}

group = "net.pitan76"
version = "0.0.8"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}

intellij {
    version.set("2024.3.5")
    type.set("IC")
    plugins.set(listOf("java"))
    updateSinceUntilBuild.set(false)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17" // Java 17を使用（21はまだ実験的）
        targetCompatibility = "17"
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}