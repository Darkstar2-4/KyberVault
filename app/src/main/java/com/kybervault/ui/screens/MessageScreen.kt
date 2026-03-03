package com.kybervault.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kybervault.i18n.LocalStrings
import com.kybervault.ui.VaultUiState
import com.kybervault.ui.components.CopyShareBar

@Composable
fun MessageScreen(
    uiState: VaultUiState,
    onEncrypt: (String) -> Unit,
    onDecrypt: (String) -> Unit,
    onHideCopyWarning: () -> Unit,
    modifier: Modifier = Modifier
) {
    val S = LocalStrings.current
    var plaintext by remember { mutableStateOf("") }
    var ciphertext by remember { mutableStateOf("") }
    var isEncryptMode by remember { mutableStateOf(true) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp).imePadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(S.secureMessage, style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        Text(S.aesSubtitle, style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (!uiState.hasSessionKey) {
            Surface(color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                Text(S.noSessionKey, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(14.dp))
            }
        } else {
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                Text(S.sessionReady, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp))
            }
        }

        uiState.error?.let { err ->
            Surface(color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                Text(err, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
            }
        }

        Surface(color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(selected = isEncryptMode, onClick = { isEncryptMode = true },
                    label = { Text(S.encrypt) }, modifier = Modifier.weight(1f))
                FilterChip(selected = !isEncryptMode, onClick = { isEncryptMode = false },
                    label = { Text(S.decrypt) }, modifier = Modifier.weight(1f))
            }
        }

        if (isEncryptMode) {
            OutlinedTextField(value = plaintext, onValueChange = { plaintext = it },
                label = { Text(S.tabMessage) }, placeholder = { Text(S.messagePlaceholder) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), maxLines = 10)
            Button(onClick = { onEncrypt(plaintext) },
                enabled = plaintext.isNotBlank() && uiState.hasSessionKey && !uiState.isProcessing,
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.medium) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp)); Text(S.encrypting)
                } else {
                    Icon(Icons.Filled.Lock, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(S.encrypt, fontWeight = FontWeight.Bold)
                }
            }
            if (uiState.lastCiphertext.isNotBlank()) {
                MsgOutputCard(S.encrypted, uiState.lastCiphertext, uiState.hideCopyWarning, onHideCopyWarning)
            }
        } else {
            OutlinedTextField(value = ciphertext, onValueChange = { ciphertext = it },
                label = { Text(S.ciphertext) }, placeholder = { Text(S.ciphertextPlaceholder) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), maxLines = 10)
            Button(onClick = { onDecrypt(ciphertext) },
                enabled = ciphertext.isNotBlank() && uiState.hasSessionKey && !uiState.isProcessing,
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp)); Text(S.decrypting)
                } else {
                    Icon(Icons.Filled.LockOpen, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(S.decrypt, fontWeight = FontWeight.Bold)
                }
            }
            if (uiState.lastDecryptedText.isNotBlank()) {
                MsgOutputCard(S.decrypted, uiState.lastDecryptedText, uiState.hideCopyWarning, onHideCopyWarning)
            }
        }

        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
            Text(S.messageTip, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun MsgOutputCard(label: String, text: String, hideCopyWarning: Boolean, onHideCopyWarning: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            CopyShareBar(label = label, text = text, hideCopyWarning = hideCopyWarning,
                onCopyConfirmed = {}, onHideCopyWarning = onHideCopyWarning)
        }
    }
}
