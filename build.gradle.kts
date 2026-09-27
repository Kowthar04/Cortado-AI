// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.ktlint)
}

val ktlintVersion: String = libs.versions.ktlint.get()

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        // Pin the ktlint engine so local runs and CI format identically.
        version.set(ktlintVersion)
        filter {
            exclude { element -> element.file.path.contains("${File.separator}build${File.separator}") }
        }
    }
}
