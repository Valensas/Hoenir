import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.20"
    id("application")
    id("org.jlleitschuh.gradle.ktlint") version "11.3.2"
    id("com.google.cloud.tools.jib") version "3.3.2"
    id("com.github.ben-manes.versions") version "0.46.0"
}

group = "com.valensas"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.valensas.hoenir.MainKt")
}

dependencies {
    implementation("io.kubernetes:client-java-extended:18.0.0")
    implementation("org.slf4j:slf4j-simple:2.0.7")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
