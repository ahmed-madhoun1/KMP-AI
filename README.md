# KMP AI

[![Build](https://github.com/ahmed-madhoun1/KMP-AI/actions/workflows/build.yml/badge.svg)](https://github.com/ahmed-madhoun1/KMP-AI/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-blueviolet.svg?logo=kotlin)](https://kotlinlang.org/)

A Kotlin Multiplatform SDK that provides a unified, type-safe, and extensible API for integrating AI models into Kotlin applications.

## Overview

KMP AI simplifies AI integration across Kotlin Multiplatform applications. The SDK provides a consistent developer experience for interacting with multiple AI providers through a single, unified API — targeting **Android**, **iOS**, **JVM**, and **Desktop**.

## Supported Providers

| Provider | Chat | Streaming | Embeddings | Tool Calls | Vision |
|---|---|---|---|---|---|
| OpenAI | ✅ | ✅ | ✅ | ✅ | ✅ |
| Anthropic | ✅ | ✅ | ❌ | ✅ | ✅ |
| Google Gemini | ✅ | ✅ | ✅ | ✅ | ✅ |
| Ollama (local) | ✅ | ✅ | ✅ | ✅ | ✅ |

## Quick Start

### Installation

```kotlin
// build.gradle.kts
dependencies {
    // Core interfaces
    implementation("dev.kmpai:kmp-ai-core:0.1.0")

    // Choose your provider(s)
    implementation("dev.kmpai:kmp-ai-openai:0.1.0")
    implementation("dev.kmpai:kmp-ai-anthropic:0.1.0")
    implementation("dev.kmpai:kmp-ai-gemini:0.1.0")
    implementation("dev.kmpai:kmp-ai-ollama:0.1.0")

    // Optional extras
    implementation("dev.kmpai:kmp-ai-streaming:0.1.0")
    implementation("dev.kmpai:kmp-ai-tools:0.1.0")
    implementation("dev.kmpai:kmp-ai-multimodal:0.1.0")
}
```

### Basic Usage

```kotlin
// Create a client
val client = KmpAi.openAi {
    apiKey = "sk-..."
}

// Single response
val response = client.chat(
    ChatRequest(
        model = OpenAiModels.GPT_4_1,
        messages = listOf(
            Message(Role.user, MessageContent.Text("What is Kotlin Multiplatform?"))
        )
    )
)
println(response.choices.first().message.content)

// Streaming
client.chatStream(request).textDeltas().collect { token ->
    print(token)
}

client.close()
```

### Conversation DSL

```kotlin
val session = client.session(model = OpenAiModels.GPT_4_1_MINI)

val r1 = session.send("Hello! Who are you?")
val r2 = session.send("What did I just ask you?") // Remembers history
```

### Tool Calling

```kotlin
val weatherTool = tool("get_weather") {
    description("Get current weather for a location")
    parameters {
        string("city") { description("The city name"); required() }
        string("unit") { description("Unit"); enum("celsius", "fahrenheit") }
    }
}

val executor = ToolExecutor {
    handle("get_weather") { args ->
        val city = args.getString("city")
        """{"temperature": 22, "condition": "sunny", "city": "$city"}"""
    }
}

val response = client.chatWithTools(
    request  = ChatRequest(model = OpenAiModels.GPT_4_1, messages = messages, tools = listOf(weatherTool)),
    executor = executor,
)
```

### Image Analysis (Multimodal)

```kotlin
val request = ChatRequest(
    model = OpenAiModels.GPT_4_1,
    messages = listOf(
        message(Role.user) {
            text("Describe what you see in this image:")
            image("https://example.com/photo.jpg")
        }
    )
)
val response = client.chat(request)
```

## Modules

| Module | Description |
|---|---|
| `kmp-ai-core` | Core interfaces, models, error hierarchy |
| `kmp-ai-openai` | OpenAI provider (GPT-4.1, o3, o4-mini, embeddings) |
| `kmp-ai-anthropic` | Anthropic provider (Claude 4, Claude 3.7) |
| `kmp-ai-gemini` | Google Gemini provider (Gemini 2.5) |
| `kmp-ai-ollama` | Ollama local model provider |
| `kmp-ai-streaming` | Flow utilities for streaming responses |
| `kmp-ai-tools` | Type-safe tool/function calling DSL |
| `kmp-ai-multimodal` | Image and audio input helpers |

## Architecture

```
kmp-ai-core          ← Interfaces + models (no HTTP)
    ↑
kmp-ai-openai        ← OpenAI HTTP client + DTOs
kmp-ai-anthropic     ← Anthropic HTTP client + DTOs
kmp-ai-gemini        ← Gemini HTTP client + DTOs
kmp-ai-ollama        ← Ollama HTTP client + DTOs
    ↑
kmp-ai-streaming     ← Flow extensions
kmp-ai-tools         ← Tool calling DSL
kmp-ai-multimodal    ← Multimodal content helpers
```

## Requirements

- Kotlin **2.3.20**
- Gradle **9.3.1**
- Android: minSdk **24**, compileSdk **36**
- iOS: Xcode 16+ (macOS required for iOS targets)
- JVM: Java **17+**

## Status

🚧 **Active development — pre-release**

## License

```
Copyright 2026 Ahmed Madhoun

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
