package com.kybervault.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kybervault.i18n.LocalStrings
import com.kybervault.ui.VaultUiState
import com.kybervault.ui.components.CopyShareBar
import com.kybervault.ui.components.KeyStatusBar

@Composable
fun ExchangeScreen(
    uiState: VaultUiState,
    onImportRecipientKey: (String) -> Unit,
    onSend: (message: String) -> Unit,
    onReceive: (ciphertext: String, customKyberPriv: String, customX25519Priv: String) -> Unit,
    onGenerateSessionKey: () -> Unit,
    onSetSessionKeyManual: (String) -> Unit,
    onHideCopyWarning: () -> Unit,
    modifier: Modifier = Modifier
) {
    val S = LocalStrings.current
    var recipientKyberPubInput by remember { mutableStateOf("") }
    var recipientX25519PubInput by remember { mutableStateOf("") }
    var isHybridRecipient by remember { mutableStateOf(true) }
    var messageInput by remember { mutableStateOf("") }
    var kemCiphertextInput by remember { mutableStateOf("") }
    var customKyberPrivInput by remember { mutableStateOf("") }
    var customX25519PrivInput by remember { mutableStateOf("") }
    var customAesKeyInput by remember { mutableStateOf("") }
    var useCustomPrivKey by remember { mutableStateOf(false) }
    var isSendMode by remember { mutableStateOf(true) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp).imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(S.keyExchange, style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(S.kemSubtitle, style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (uiState.hasSessionKey) {
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                Text(S.sessionKeyActive, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp))
            }
        }

        KeyStatusBar(hasKey = uiState.hasActiveKey, keyCount = uiState.keyCount,
            statusMessage = uiState.statusMessage, error = uiState.error)

        // Session Key section
        Surface(color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(S.sessionKey, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Text(S.sessionKeyDescription, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onGenerateSessionKey, modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                    Icon(Icons.Filled.VpnKey, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                    Text(S.generateAes256, fontWeight = FontWeight.SemiBold)
                }
                if (uiState.generatedAesKey.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    OutputCard(S.sessionKey, uiState.generatedAesKey, uiState.hideCopyWarning, onHideCopyWarning)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = customAesKeyInput, onValueChange = { customAesKeyInput = it },
                    label = { Text(S.pasteAesKey) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline))
                if (customAesKeyInput.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = { onSetSessionKeyManual(customAesKeyInput) },
                        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                        Text(S.setCustomKey, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Text(S.kemExchange, style = MaterialTheme.typography.labelLarge)

        Surface(color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(selected = isSendMode, onClick = { isSendMode = true },
                    label = { Text(S.send) }, modifier = Modifier.weight(1f))
                FilterChip(selected = !isSendMode, onClick = { isSendMode = false },
                    label = { Text(S.receive) }, modifier = Modifier.weight(1f))
            }
        }

        if (isSendMode) {
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                Text(S.sendInstructions, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(selected = isHybridRecipient, onClick = { isHybridRecipient = true },
                    label = { Text(S.hybrid) }, modifier = Modifier.weight(1f))
                FilterChip(selected = !isHybridRecipient, onClick = { isHybridRecipient = false },
                    label = { Text(S.kyberOnly) }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = recipientKyberPubInput, onValueChange = { recipientKyberPubInput = it },
                label = { Text(S.recipientKyberPub) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp), maxLines = 4)
            if (isHybridRecipient) {
                OutlinedTextField(value = recipientX25519PubInput, onValueChange = { recipientX25519PubInput = it },
                    label = { Text(S.recipientX25519Pub) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp), maxLines = 4)
            }
            val importBlock = buildString {
                appendLine("--- KYBERVAULT PUBLIC KEY ---")
                appendLine("Kyber-1024: ${recipientKyberPubInput.trim()}")
                if (isHybridRecipient && recipientX25519PubInput.isNotBlank())
                    appendLine("X25519: ${recipientX25519PubInput.trim()}")
                appendLine("--- END PUBLIC KEY ---")
            }
            val canImport = recipientKyberPubInput.isNotBlank() &&
                    (!isHybridRecipient || recipientX25519PubInput.isNotBlank())
            Button(onClick = { onImportRecipientKey(importBlock) }, enabled = canImport,
                modifier = Modifier.fillMaxWidth().height(44.dp), shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                Icon(Icons.Filled.PersonAdd, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                Text(S.importRecipientKey, fontWeight = FontWeight.SemiBold)
            }
            if (uiState.hasRecipientKey) {
                Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Text("\u2713 ${uiState.recipientKeyPreview}",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            OutlinedTextField(value = messageInput, onValueChange = { messageInput = it },
                label = { Text(S.messageOptional) }, placeholder = { Text(S.leaveBlankForSession) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp), maxLines = 6)
            Button(onClick = { onSend(messageInput) },
                enabled = uiState.hasRecipientKey && !uiState.isProcessing,
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.medium) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp)); Text(S.encrypting)
                } else {
                    Icon(Icons.Filled.Lock, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(if (messageInput.isBlank()) S.establishSession else S.sendEncrypted,
                        fontWeight = FontWeight.SemiBold)
                }
            }
            if (uiState.lastCiphertext.isNotBlank()) {
                OutputCard(S.kemCiphertext, uiState.lastCiphertext, uiState.hideCopyWarning, onHideCopyWarning)
            }
        } else {
            Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
                shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                Text(S.receiveInstructions, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
            }
            OutlinedTextField(value = kemCiphertextInput, onValueChange = { kemCiphertextInput = it },
                label = { Text(S.kemCiphertext) }, placeholder = { Text(S.kemCiphertextPlaceholder) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp), maxLines = 8)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(selected = !useCustomPrivKey, onClick = { useCustomPrivKey = false },
                    label = { Text(S.generatedKey) }, modifier = Modifier.weight(1f))
                FilterChip(selected = useCustomPrivKey, onClick = { useCustomPrivKey = true },
                    label = { Text(S.customKey) }, modifier = Modifier.weight(1f))
            }
            if (useCustomPrivKey) {
                OutlinedTextField(value = customKyberPrivInput, onValueChange = { customKyberPrivInput = it },
                    label = { Text(S.kyberPrivBase64) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp), maxLines = 4)
                OutlinedTextField(value = customX25519PrivInput, onValueChange = { customX25519PrivInput = it },
                    label = { Text(S.x25519PrivBase64) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp), maxLines = 4)
            }
            val canDecrypt = kemCiphertextInput.isNotBlank() && !uiState.isProcessing &&
                    (uiState.hasActiveKey || (useCustomPrivKey && customKyberPrivInput.isNotBlank()))
            Button(onClick = {
                onReceive(kemCiphertextInput,
                    if (useCustomPrivKey) customKyberPrivInput else "",
                    if (useCustomPrivKey) customX25519PrivInput else "")
            }, enabled = canDecrypt,
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp)); Text(S.decrypting)
                } else {
                    Icon(Icons.Filled.LockOpen, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(S.decryptDeriveKey, fontWeight = FontWeight.SemiBold)
                }
            }
            if (uiState.lastDecryptedText.isNotBlank()) {
                OutputCard(S.decryptedMessage, uiState.lastDecryptedText, uiState.hideCopyWarning, onHideCopyWarning)
                if (uiState.hasSessionKey) {
                    Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                        Text(S.sessionKeySaved, style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OutputCard(label: String, text: String, hideCopyWarning: Boolean, onHideCopyWarning: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
                maxLines = 10, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            CopyShareBar(label = label, text = text, hideCopyWarning = hideCopyWarning,
                onCopyConfirmed = {}, onHideCopyWarning = onHideCopyWarning)
        }
    }
}