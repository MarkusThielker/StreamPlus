import org.jetbrains.compose.compose

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "0.2.0-build132"
}

dependencies {

    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "ApplicationKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "StreamPlus"
        }
    }
}