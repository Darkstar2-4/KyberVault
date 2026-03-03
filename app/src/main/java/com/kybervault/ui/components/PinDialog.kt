package com.kybervault.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kybervault.i18n.LocalStrings

/**
 * PIN dialog with two modes:
 * - SETUP: User sets a new PIN (enter + confirm)
 * - VERIFY: User enters existing PIN to authenticate
 */
@Composable
fun PinDialog(
    mode: PinMode,
    onPinConfirmed: (String) -> Unit,
    onDismiss: () -> Unit,
    onVerifyFailed: (() -> Unit)? = null,
    verifyPin: ((String) -> Boolean)? = null
) {
    val S = LocalStrings.current
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableIntStateOf(0) }

    val isSetup = mode == PinMode.SETUP
    val maxAttempts = 5

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Filled.Lock, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        },
        title = {
            Text(
                if (isSetup) "Set PIN Code" else "Enter PIN",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (isSetup)
                        "Choose a 4\u20138 digit PIN to protect saved keys. You\u2019ll need this PIN to save or recall keys from device storage."
                    else
                        "Enter your PIN to access key storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) { pin = it; error = null } },
                    label = { Text(if (isSetup) "New PIN" else "PIN") },
                    placeholder = { Text("4\u20138 digits") },
                    singleLine = true,
                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    trailingIcon = {
                        IconButton(onClick = { showPin = !showPin }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                if (showPin) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null, modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                if (isSetup) {
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) { confirmPin = it; error = null } },
                        label = { Text("Confirm PIN") },
                        singleLine = true,
                        visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth())
                }

                if (!isSetup && attempts > 0) {
                    Text("${maxAttempts - attempts} attempts remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSetup) {
                        when {
                            pin.length < 4 -> error = "PIN must be at least 4 digits"
                            pin != confirmPin -> error = "PINs don\u2019t match"
                            else -> onPinConfirmed(pin)
                        }
                    } else {
                        if (verifyPin?.invoke(pin) == true) {
                            onPinConfirmed(pin)
                        } else {
                            attempts++
                            error = "Incorrect PIN"
                            pin = ""
                            if (attempts >= maxAttempts) {
                                onVerifyFailed?.invoke()
                                onDismiss()
                            }
                        }
                    }
                },
                enabled = pin.length >= 4 && (!isSetup || confirmPin.length >= 4)
            ) {
                Text(if (isSetup) "Set PIN" else "Unlock", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(S.cancel) }
        }
    )
}

enum class PinMode { SETUP, VERIFY }