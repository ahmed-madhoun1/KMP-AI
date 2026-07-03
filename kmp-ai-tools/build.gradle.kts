plugins {
    id("kmp-library")
    id("publishing")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":kmp-ai-core"))
            }
        }
    }
}
