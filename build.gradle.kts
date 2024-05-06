import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.23"
    id("application")
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("com.google.cloud.tools.jib") version "3.4.1"
    id("com.github.ben-manes.versions") version "0.51.0"
}

group = "com.valensas"
version = "1.1.2-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.valensas.hoenir.MainKt")
}

dependencies {
    implementation("io.kubernetes:client-java-extended:20.0.1")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
