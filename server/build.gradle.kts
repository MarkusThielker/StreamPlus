plugins {
    kotlin("jvm")
}

dependencies {

    implementation(project(":shared"))

    implementation("khttp:khttp:0.1.0")

    implementation("io.ktor:ktor-gson:1.5.2")

    implementation("io.ktor:ktor-server-core:1.5.2")
    implementation("io.ktor:ktor-server-netty:1.5.2")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    testImplementation("io.ktor:ktor-server-tests:1.5.2")

    implementation("io.ktor:ktor-network-tls-certificates:1.5.2")
    implementation(kotlin("script-runtime"))
}