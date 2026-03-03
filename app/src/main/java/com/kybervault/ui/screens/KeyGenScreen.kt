package com.kybervault.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kybervault.crypto.EncryptionMode
import com.kybervault.i18n.LocalStrings
import com.kybervault.ui.VaultUiState
import com.kybervault.ui.components.CopyShareBar
import com.kybervault.ui.components.KeyStatusBar
import com.kybervault.ui.components.WipeDialog
import com.kybervault.util.displayAs

@Composable
fun KeyGenScreen(
    uiState: VaultUiState,
    onGenerate: (alias: String) -> Unit,
    onSetMode: (EncryptionMode) -> Unit,
    onTogglePrivateKeys: () -> Unit,
    onSaveRequested: () -> Unit,
    onLoadRequested: (String) -> Unit,
    onWipeRamOnly: () -> Unit,
    onWipeRamAndStorage: () -> Unit,
    onHideCopyWarning: () -> Unit,
    modifier: Modifier = Modifier
) {
    val S = LocalStrings.current
    var alias by remember { mutableStateOf("default") }
    var loadAlias by remember { mutableStateOf("default") }
    var showWipeDialog by remember { mutableStateOf(false) }
    val enc = uiState.displayEncoding

    if (showWipeDialog) {
        WipeDialog(
            onWipeRamOnly = { onWipeRamOnly(); showWipeDialog = false },
            onWipeRamAndStorage = { onWipeRamAndStorage(); showWipeDialog = false },
            onDismiss = { showWipeDialog = false }
        )
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp).imePadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(S.keyGenerator, style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        KeyStatusBar(hasKey = uiState.hasActiveKey, keyCount = uiState.keyCount,
            statusMessage = uiState.statusMessage, error = uiState.error)

        OutlinedTextField(value = alias, onValueChange = { alias = it },
            label = { Text(S.keyAlias) }, placeholder = { Text(S.keyAliasPlaceholder) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline))

        Surface(color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(S.exchangeMode, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                ModeChip(EncryptionMode.HYBRID, uiState.encryptionMode,
                    S.hybridTitle, S.hybridSubtitle, S.hybridBadge,
                    MaterialTheme.colorScheme.secondary, onSetMode)
                Spacer(Modifier.height(6.dp))
                ModeChip(EncryptionMode.KYBER, uiState.encryptionMode,
                    S.kyberTitle, S.kyberSubtitle, S.kyberBadge,
                    MaterialTheme.colorScheme.primary, onSetMode)
            }
        }

        Button(onClick = { onGenerate(alias.ifBlank { "default" }) }, enabled = !uiState.isGenerating,
            modifier = Modifier.fillMaxWidth().height(56.dp), shape = MaterialTheme.shapes.medium) {
            if (uiState.isGenerating) {
                CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp)); Text(S.generating)
            } else {
                Icon(Icons.Filled.Key, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text(when (uiState.encryptionMode) {
                    EncryptionMode.HYBRID -> S.generateHybridKeypair
                    EncryptionMode.KYBER -> S.generateKyberKeypair
                    else -> S.generateKeypair
                }, fontWeight = FontWeight.Bold)
            }
        }

        Surface(color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(S.recallSavedKeys, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(S.recallDescription, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = loadAlias, onValueChange = { loadAlias = it },
                    label = { Text(S.aliasToLoad) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline))
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onLoadRequested(loadAlias.ifBlank { "default" }) },
                    modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Icon(Icons.Filled.Restore, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp)); Text(S.recallKeys, fontWeight = FontWeight.Bold)
                }
            }
        }

        AnimatedVisibility(visible = uiState.hasActiveKey) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(S.activeAlias(uiState.activeAlias), style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary)

                Text(S.publicKeysSafe, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)

                KeyCard(S.kyberPublicKey, uiState.kyberPubKeyFull.displayAs(enc),
                    uiState.kyberPubKeyFull, false, uiState.hideCopyWarning, onHideCopyWarning)
                if (uiState.encryptionMode == EncryptionMode.HYBRID) {
                    KeyCard(S.x25519PublicKey, uiState.x25519PubKeyFull.displayAs(enc),
                        uiState.x25519PubKeyFull, false, uiState.hideCopyWarning, onHideCopyWarning)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(S.privateKeys, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onTogglePrivateKeys) {
                        Icon(if (uiState.showPrivateKeys) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(4.dp))
                        Text(if (uiState.showPrivateKeys) S.hide else S.reveal,
                            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                    }
                }

                AnimatedVisibility(visible = uiState.showPrivateKeys) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        KeyCard(S.kyberPrivateKey, uiState.kyberPrivKeyFull.displayAs(enc),
                            uiState.kyberPrivKeyFull, true, uiState.hideCopyWarning, onHideCopyWarning)
                        if (uiState.encryptionMode == EncryptionMode.HYBRID) {
                            KeyCard(S.x25519PrivateKey, uiState.x25519PrivKeyFull.displayAs(enc),
                                uiState.x25519PrivKeyFull, true, uiState.hideCopyWarning, onHideCopyWarning)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onSaveRequested, modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium) {
                        Icon(Icons.Filled.Save, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(S.save)
                    }
                    Button(onClick = { showWipeDialog = true }, modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Filled.DeleteForever, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(S.wipe)
                    }
                }
            }
        }

        if (!uiState.hasActiveKey) {
            OutlinedButton(onClick = { showWipeDialog = true }, modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)) {
                Icon(Icons.Filled.DeleteForever, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(4.dp)); Text(S.wipeStorage, color = MaterialTheme.colorScheme.error)
            }
        }

        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
            Text(S.keygenTip, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun KeyCard(label: String, displayText: String, rawText: String,
                    isSecret: Boolean, hideCopyWarning: Boolean, onHideCopyWarning: () -> Unit) {
    Surface(
        color = if (isSecret) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, if (isSecret) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                color = if (isSecret) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(displayText, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                fontFamily = FontFamily.Monospace, maxLines = 6, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            CopyShareBar(label = label, text = rawText, hideCopyWarning = hideCopyWarning,
                onCopyConfirmed = {}, onHideCopyWarning = onHideCopyWarning)
        }
    }
}

@Composable
private fun ModeChip(mode: EncryptionMode, selected: EncryptionMode,
                     title: String, subtitle: String, badge: String,
                     badgeColor: androidx.compose.ui.graphics.Color, onSelect: (EncryptionMode) -> Unit) {
    val isSel = selected == mode
    Surface(
        onClick = { onSelect(mode) },
        color = if (isSel) badgeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, if (isSel) badgeColor else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isSel, onClick = { onSelect(mode) },
                colors = RadioButtonDefaults.colors(selectedColor = badgeColor))
            Column(modifier = Modifier.padding(start = 4.dp).weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(color = badgeColor.copy(alpha = 0.2f), shape = MaterialTheme.shapes.extraSmall) {
                Text(badge, style = MaterialTheme.typography.labelSmall, color = badgeColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
            }
        }
    }
}