import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation("org.mariuszgromada.math:MathParser.org-mXparser:6.1.0")
    implementation(libs.kotlinx.coroutinesSwing)
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "com.megakotl.app.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "MegaKotl"
            packageVersion = "1.0.0"
        }
    }
}
