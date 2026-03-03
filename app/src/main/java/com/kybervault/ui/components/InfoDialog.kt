package com.kybervault.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kybervault.i18n.LocalStrings

@Composable
fun InfoDialog(onDismiss: () -> Unit) {
    val S = LocalStrings.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
        title = { Text(S.appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Sec(S.whatIsKyberVault, S.whatIsDescription)
                Sec(S.whyPostQuantum, S.whyPqDescription)
                Sec(S.hybridMode, S.hybridDescription)
                Sec(S.exchangeFlow, S.exchangeFlowDescription)
                Sec(S.wireFormat, S.wireFormatInfo)
                Sec(S.securityModel, S.securityModelInfo)
                Sec(S.clipboardSafety, S.clipboardSafetyInfo)
                Sec(S.keySizes, S.keySizesInfo)

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                TextButton(onClick = { try { uriHandler.openUri("https://github.com/Darkstar2-4/KyberVault/") } catch (_: Exception) {} },
                    modifier = Modifier.fillMaxWidth()) {
                    Text("github.com/Darkstar2-4/KyberVault", textDecoration = TextDecoration.Underline,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(S.supportDevelopment, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)

                Text(S.supportDescription, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)

                Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(S.btcLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(2.dp))
                            Text("bc1q28sq3z2z9mzyesey0023e9240tv48cyv8vw2v2",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                fontFamily = FontFamily.Monospace)
                        }
                        IconButton(onClick = { copyPlain(context, "BTC Address", "bc1q28sq3z2z9mzyesey0023e9240tv48cyv8vw2v2", S.copied) },
                            modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.ContentCopy, S.copy, Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }

                Text(S.freeOpenSource, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(S.close) } }
    )
}

@Composable
private fun Sec(title: String, body: String) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodySmall, lineHeight = 17.sp)
    }
}