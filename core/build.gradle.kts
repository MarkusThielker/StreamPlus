plugins {
    kotlin("jvm")
}

dependencies {

    implementation(project(":shared"))

    // google gson
    implementation("com.google.code.gson:gson:2.8.6")

    // kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
}