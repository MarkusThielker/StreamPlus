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
            windows.packageVersion = "0.1.0"
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "StreamPlus"
        }
    }
}