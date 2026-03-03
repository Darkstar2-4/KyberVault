package com.kybervault.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kybervault.i18n.AvailableLanguages
import com.kybervault.i18n.LocalStrings

@Composable
fun SettingsDialog(
    flagSecure: Boolean, onFlagSecureChange: (Boolean) -> Unit,
    wipeOnExit: Boolean, onWipeOnExitChange: (Boolean) -> Unit,
    hideCopyWarning: Boolean, onHideCopyWarningChange: (Boolean) -> Unit,
    displayEncoding: String, onDisplayEncodingChange: (String) -> Unit,
    useWireFormat: Boolean, onUseWireFormatChange: (Boolean) -> Unit,
    dyslexicFont: Boolean, onDyslexicFontChange: (Boolean) -> Unit,
    language: String, onLanguageChange: (String) -> Unit,
    requireBiometric: Boolean, onRequireBiometricChange: (Boolean) -> Unit,
    biometricAvailable: Boolean,
    requirePin: Boolean,
    onSetPin: (String) -> Unit, onDisablePin: () -> Unit,
    requireTotp: Boolean,
    onEnableTotp: (ByteArray) -> Unit, onDisableTotp: () -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    var showBiometricDisableWarning by remember { mutableStateOf(false) }
    var showPinDisableWarning by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }
    var showTotpSetup by remember { mutableStateOf(false) }
    var showTotpDisableWarning by remember { mutableStateOf(false) }

    if (showBiometricDisableWarning) {
        AlertDialog(
            onDismissRequest = { showBiometricDisableWarning = false },
            icon = { Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp)) },
            title = { Text("Disable Biometric Lock?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Disabling biometric authentication will permanently delete all saved keys from device storage. Keys currently in RAM will not be affected.\n\nThis cannot be undone.",
                    style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
            },
            confirmButton = {
                Button(
                    onClick = { showBiometricDisableWarning = false; onRequireBiometricChange(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete & Disable") }
            },
            dismissButton = { OutlinedButton(onClick = { showBiometricDisableWarning = false }) { Text(s.cancel) } }
        )
    }

    if (showPinDisableWarning) {
        AlertDialog(
            onDismissRequest = { showPinDisableWarning = false },
            icon = { Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp)) },
            title = { Text("Disable PIN Lock?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Disabling PIN authentication will permanently delete all saved keys from device storage and remove your PIN. Keys currently in RAM will not be affected.\n\nThis cannot be undone.",
                    style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
            },
            confirmButton = {
                Button(
                    onClick = { showPinDisableWarning = false; onDisablePin() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete & Disable") }
            },
            dismissButton = { OutlinedButton(onClick = { showPinDisableWarning = false }) { Text(s.cancel) } }
        )
    }

    if (showPinSetup) {
        PinDialog(
            mode = PinMode.SETUP,
            onPinConfirmed = { pin -> onSetPin(pin); showPinSetup = false },
            onDismiss = { showPinSetup = false }
        )
    }

    if (showTotpDisableWarning) {
        AlertDialog(
            onDismissRequest = { showTotpDisableWarning = false },
            icon = { Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp)) },
            title = { Text("Disable 2FA?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Disabling 2FA will permanently delete all saved keys from device storage and remove your authenticator link. Keys currently in RAM will not be affected.\n\nThis cannot be undone.",
                    style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
            },
            confirmButton = {
                Button(
                    onClick = { showTotpDisableWarning = false; onDisableTotp() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete & Disable") }
            },
            dismissButton = { OutlinedButton(onClick = { showTotpDisableWarning = false }) { Text(s.cancel) } }
        )
    }

    if (showTotpSetup) {
        TotpSetupDialog(
            onSetupComplete = { secret -> onEnableTotp(secret); showTotpSetup = false },
            onDismiss = { showTotpSetup = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Settings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
        title = { Text(s.settings, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {

                ExpandableSection(s.security, defaultExpanded = true) {
                    SettingsToggle(s.screenshotProtection, s.screenshotDescription, flagSecure, onFlagSecureChange)
                    SettingsToggle(s.wipeOnClose, s.wipeOnCloseDescription, wipeOnExit, onWipeOnExitChange)

                    // Biometric lock
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Fingerprint, null, modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Biometric Lock", style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold)
                                }
                                Text(
                                    if (biometricAvailable)
                                        "Require fingerprint to save or recall keys from device storage."
                                    else
                                        "No biometric hardware or enrolled fingerprints detected.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = requireBiometric,
                                onCheckedChange = { enabled ->
                                    if (enabled) onRequireBiometricChange(true)
                                    else showBiometricDisableWarning = true
                                },
                                enabled = biometricAvailable
                            )
                        }
                    }

                    // PIN lock
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Lock, null, modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(6.dp))
                                    Text("PIN Lock", style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold)
                                }
                                Text(
                                    "Require a 4\u20138 digit PIN to save or recall keys. Works on all devices, with or without biometrics.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = requirePin,
                                onCheckedChange = { enabled ->
                                    if (enabled) showPinSetup = true
                                    else showPinDisableWarning = true
                                }
                            )
                        }
                    }

                    // TOTP 2FA lock
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.QrCode2, null, modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(6.dp))
                                    Text("2FA (Authenticator)", style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold)
                                }
                                Text(
                                    "Require a time-based code from Google Authenticator, Authy, or similar apps to save or recall keys.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = requireTotp,
                                onCheckedChange = { enabled ->
                                    if (enabled) showTotpSetup = true
                                    else showTotpDisableWarning = true
                                }
                            )
                        }
                    }
                }

                ExpandableSection(s.encryption) {
                    SettingsToggle(s.wireFormatHeader, s.wireFormatDescription, useWireFormat, onUseWireFormatChange)
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                        Text(s.wireFormatNote, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp), lineHeight = 16.sp)
                    }
                }

                ExpandableSection(s.display) {
                    SettingsToggle(s.skipCopyWarning, s.skipCopyDescription, hideCopyWarning, onHideCopyWarningChange)
                    Text(s.keyEncoding, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = displayEncoding == "base64", onClick = { onDisplayEncodingChange("base64") },
                            label = { Text(s.base64) }, modifier = Modifier.weight(1f))
                        FilterChip(selected = displayEncoding == "hex", onClick = { onDisplayEncodingChange("hex") },
                            label = { Text(s.hex) }, modifier = Modifier.weight(1f))
                    }
                }

                ExpandableSection(s.accessibility) {
                    SettingsToggle(s.openDyslexicFont, s.openDyslexicDescription, dyslexicFont, onDyslexicFontChange)

                    Text(s.language, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp))
                    // Language grid — wraps to multiple rows
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AvailableLanguages.forEach { (code, displayName) ->
                            FilterChip(
                                selected = language == code,
                                onClick = { onLanguageChange(code) },
                                label = { Text(displayName, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(s.done) } }
    )
}

@Composable
private fun ExpandableSection(title: String, defaultExpanded: Boolean = false,
                              content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
            }
        }
    }
}

@Composable
private fun SettingsToggle(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}