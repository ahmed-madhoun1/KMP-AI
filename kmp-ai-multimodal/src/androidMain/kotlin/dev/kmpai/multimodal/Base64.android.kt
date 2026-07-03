package dev.kmpai.multimodal

import android.util.Base64

actual fun ByteArray.toBase64(): String =
    Base64.encodeToString(this, Base64.NO_WRAP)
