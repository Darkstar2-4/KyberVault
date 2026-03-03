package com.kybervault.util

import android.util.Base64

/** Convert Base64 string to hex display if encoding is "hex", otherwise return as-is. */
fun String.displayAs(encoding: String): String {
    if (encoding != "hex" || this.isBlank()) return this
    return try {
        val bytes = Base64.decode(this, Base64.NO_WRAP)
        bytes.joinToString("") { "%02x".format(it) }
    } catch (_: Exception) { this }
}
