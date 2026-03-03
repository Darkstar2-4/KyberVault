package com.kybervault.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ShareUtil {
    /** Share text directly via Android share sheet. */
    fun shareText(context: Context, label: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "KyberVault: $label")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share $label"))
    }

    /** Share text as a .txt file via FileProvider. */
    fun shareAsFile(context: Context, label: String, text: String, filename: String = "kybervault_key.txt") {
        try {
            val dir = File(context.cacheDir, "shared")
            dir.mkdirs()
            val file = File(dir, filename)
            file.writeText(text)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "KyberVault: $label")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share $label as file"))
        } catch (_: Exception) {
            shareText(context, label, text)
        }
    }
}
