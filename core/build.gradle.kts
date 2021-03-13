plugins {
    kotlin("jvm")
}

dependencies {

    implementation(project(":shared"))

    implementation("io.ktor:ktor-client-core:1.5.2")
    implementation("io.ktor:ktor-client-cio:1.5.2")
    implementation("io.ktor:ktor-client-core-jvm:1.5.2")
    implementation("io.ktor:ktor-client-apache:1.5.2")
    implementation("io.ktor:ktor-client-json-jvm:1.5.2")
    implementation("io.ktor:ktor-client-gson:1.5.2")
    implementation("io.ktor:ktor-gson:1.5.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
}