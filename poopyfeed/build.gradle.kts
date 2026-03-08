// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.spotless)
    id("com.google.gms.google-services") version "4.4.4" apply false
}

spotless {
    kotlin {
        target("**/*.kt")
        ktfmt() // Format only; no lint rules (use ktlint() for full style enforcement)
    }
}
