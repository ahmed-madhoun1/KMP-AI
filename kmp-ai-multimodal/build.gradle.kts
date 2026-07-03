plugins {
    id("kmp-library")
    id("publishing")
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
