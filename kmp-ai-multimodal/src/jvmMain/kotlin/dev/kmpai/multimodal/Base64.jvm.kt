package dev.kmpai.multimodal

import java.util.Base64

actual fun ByteArray.toBase64(): String =
    Base64.getEncoder().encodeToString(this)
