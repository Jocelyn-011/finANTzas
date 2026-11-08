plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10" apply false
    alias(libs.plugins.ksp) apply false


    id("dev.detekt") version "2.0.0-alpha.6" apply false
}

