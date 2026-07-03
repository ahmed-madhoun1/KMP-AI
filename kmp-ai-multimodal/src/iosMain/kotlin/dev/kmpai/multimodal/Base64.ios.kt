package dev.kmpai.multimodal

import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.base64EncodedStringWithOptions

actual fun ByteArray.toBase64(): String {
    val data = NSData.create(bytes = this.toCPointer(), length = this.size.toULong())
    return data.base64EncodedStringWithOptions(0u)
}
