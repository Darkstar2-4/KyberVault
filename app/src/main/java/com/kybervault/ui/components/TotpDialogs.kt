package com.kybervault.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kybervault.i18n.LocalStrings
import com.kybervault.security.TotpEngine
import com.kybervault.util.QrUtil

/**
 * Two-phase TOTP setup dialog:
 * Phase 1: Show QR code + Base32 secret for authenticator app
 * Phase 2: Verify one code to confirm setup
 */
@Composable
fun TotpSetupDialog(
    onSetupComplete: (secret: ByteArray) -> Unit,
    onDismiss: () -> Unit
) {
    val S = LocalStrings.current
    val secret = remember { TotpEngine.generateSecret() }
    val base32 = remember { TotpEngine.toBase32(secret) }
    val otpUri = remember { TotpEngine.buildOtpAuthUri(secret) }
    val qrBitmap = remember { QrUtil.generate(otpUri, 480) }

    var verifyCode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var phase by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.QrCode2, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
        title = { Text(if (phase == 1) "Set Up Authenticator" else "Verify Code", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (phase == 1) {
                    Text(
                        "Scan this QR code with your authenticator app (Google Authenticator, Authy, etc.).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp
                    )

                    // QR Code
                    qrBitmap?.let { bmp ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.size(200.dp)
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "TOTP QR Code",
                                modifier = Modifier.padding(8.dp).fillMaxSize()
                            )
                        }
                    }

                    // Manual entry fallback
                    Text("Or enter manually:", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    val context = LocalContext.current
                    val formattedKey = base32.chunked(4).joinToString(" ")
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                formattedKey,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                letterSpacing = 1.sp
                            )
                            IconButton(
                                onClick = { copyPlain(context, "TOTP Secret", base32, "Key copied") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, "Copy key",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                } else {
                    Text(
                        "Enter the 6-digit code from your authenticator app to confirm setup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp
                    )

                    OutlinedTextField(
                        value = verifyCode,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) { verifyCode = it; error = null } },
                        label = { Text("6-digit code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 4.sp,
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    error?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            if (phase == 1) {
                Button(onClick = { phase = 2 }) { Text("Next", fontWeight = FontWeight.Bold) }
            } else {
                Button(
                    onClick = {
                        if (TotpEngine.verifyCode(secret, verifyCode)) {
                            onSetupComplete(secret)
                        } else {
                            verifyCode = ""
                        }
                    },
                    enabled = verifyCode.length == 6
                ) { Text("Verify & Enable", fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = {
            if (phase == 2) {
                OutlinedButton(onClick = { phase = 1; }) { Text("Back") }
            } else {
                OutlinedButton(onClick = onDismiss) { Text(S.cancel) }
            }
        }
    )
}

/**
 * TOTP verification dialog — enter current 6-digit code.
 */
@Composable
fun TotpVerifyDialog(
    onVerified: () -> Unit,
    onDismiss: () -> Unit,
    verifyCode: (String) -> Boolean
) {
    val S = LocalStrings.current
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableIntStateOf(0) }
    val maxAttempts = 5

    // Live countdown
    var secondsLeft by remember { mutableIntStateOf(TotpEngine.secondsRemaining()) }
    LaunchedEffect(Unit) {
        while (true) {
            secondsLeft = TotpEngine.secondsRemaining()
            kotlinx.coroutines.delay(1000)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.QrCode2, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
        title = { Text("Enter 2FA Code", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enter the 6-digit code from your authenticator app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)

                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) { code = it; error = null } },
                    label = { Text("6-digit code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Timer bar
                LinearProgressIndicator(
                    progress = { secondsLeft / 30f },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = if (secondsLeft <= 5) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text("${secondsLeft}s remaining", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth())
                }

                if (attempts > 0) {
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
                    if (verifyCode(code)) {
                        onVerified()
                    } else {
                        attempts++
                        code = ""
                        if (attempts >= maxAttempts) onDismiss()
                    }
                },
                enabled = code.length == 6
            ) { Text("Verify", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(S.cancel) } }
    )
}
