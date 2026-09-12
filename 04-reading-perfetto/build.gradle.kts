plugins {
    kotlin("jvm") version "2.0.0"
}

group = "com.codingkinetics"
version = "unspecified"

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}