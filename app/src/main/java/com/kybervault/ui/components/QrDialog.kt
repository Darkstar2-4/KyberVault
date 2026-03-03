package com.kybervault.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kybervault.i18n.LocalStrings
import com.kybervault.util.QrUtil

@Composable
fun QrDialog(label: String, text: String, onDismiss: () -> Unit) {
    val S = LocalStrings.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var tooLarge by remember { mutableStateOf(false) }

    LaunchedEffect(text) {
        if (QrUtil.canGenerateQr(text)) bitmap = QrUtil.generate(text, 800)
        else tooLarge = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (tooLarge) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                        Text(S.qrTooLarge(text.length), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
                    }
                } else if (bitmap != null) {
                    Box(modifier = Modifier.size(280.dp).background(Color.White, MaterialTheme.shapes.small).padding(8.dp)) {
                        Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = S.qrCode,
                            modifier = Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(S.qrScanTip, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                } else {
                    CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(S.close) } }
    )
}
