rootProject.name = "kmp-ai"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(
    ":kmp-ai-core",
    ":kmp-ai-openai",
    ":kmp-ai-anthropic",
    ":kmp-ai-gemini",
    ":kmp-ai-ollama",
    ":kmp-ai-streaming",
    ":kmp-ai-tools",
    ":kmp-ai-multimodal",
)
