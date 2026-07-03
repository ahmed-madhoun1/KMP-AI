package dev.kmpai.multimodal

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.base64EncodedStringWithOptions

@OptIn(ExperimentalForeignApi::class)
actual fun ByteArray.toBase64(): String {
    if (isEmpty()) return ""
    val data: NSData = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
    return data.base64EncodedStringWithOptions(0u)
}
