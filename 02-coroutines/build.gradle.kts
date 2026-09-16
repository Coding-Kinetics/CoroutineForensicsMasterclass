plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(":sev01-toolkit"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.8.1")
}

tasks.withType<JavaExec>().configureEach {
    // Enables the coroutine debugger agent & names across runs
    jvmArgs("-Dkotlinx.coroutines.debug")
    standardInput = System.`in`
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("-Dkotlinx.coroutines.debug")
}