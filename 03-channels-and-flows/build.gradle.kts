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
    // Project dependencies
    implementation(project(":sev01-toolkit"))

    // Kotlin Coroutines Core & Debugger Agent
    implementation(libs.kotlinx.coroutines.core)
    //  implementation(libs.kotlinx.coroutines.debug)
    testImplementation(libs.kotlinx.coroutines.test)

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