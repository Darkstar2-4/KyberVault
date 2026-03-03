package com.kybervault.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kybervault.i18n.LocalStrings

@Composable
fun CopyDisclaimerDialog(
    visible: Boolean, label: String,
    onConfirmCopy: () -> Unit, onDismiss: () -> Unit, onNeverShowAgain: () -> Unit
) {
    if (!visible) return
    val S = LocalStrings.current
    var neverShow by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp)) },
        title = { Text(S.clipboardWarning, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(S.copying(label), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(S.clipboardWarningBody, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        WarnLine(S.clipboardBullet3)
                        WarnLine(S.clipboardBullet2)
                        WarnLine(S.clipboardBullet1)
                    }
                }
                Text(S.clipboardAdvice, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Checkbox(checked = neverShow, onCheckedChange = { neverShow = it }, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(S.dontShowAgain, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { Button(onClick = { if (neverShow) onNeverShowAgain(); onConfirmCopy() }) { Text(S.copy, fontWeight = FontWeight.Bold) } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(S.cancel) } }
    )
}

@Composable
private fun WarnLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("\u2022 ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        Text(text, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
    }
}

fun copyToClipboard(context: Context, label: String, text: String, toastMsg: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        clip.description.extras = android.os.PersistableBundle().apply {
            putBoolean("android.content.extra.IS_SENSITIVE", true)
        }
    }
    cm.setPrimaryClip(clip)
    Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()
}

fun copyPlain(context: Context, label: String, text: String, toastMsg: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
}
