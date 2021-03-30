import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "0.4.0-build177"
}

dependencies {

    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "de.markus_thielker.streamplus.ui.ApplicationKt"
        nativeDistributions {

            modules("java.instrument", "java.naming", "java.security.jgss", "java.sql", "jdk.unsupported")

            packageName = "StreamPlus"
            packageVersion = "1.0.0"
            description = "A Twitch chat bot created with Kotlin Compose Desktop"
            copyright = "StreamPlus © 2021 by Markus Thielker is licensed under CC BY-NC-ND 4.0"
            vendor = "Markus Thielker"

            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
        }
    }
}