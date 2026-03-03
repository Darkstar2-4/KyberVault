package com.kybervault.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kybervault.i18n.LocalStrings
import com.kybervault.util.ShareUtil

@Composable
fun CopyShareBar(
    label: String, text: String, hideCopyWarning: Boolean,
    onCopyConfirmed: () -> Unit, onHideCopyWarning: () -> Unit,
    modifier: Modifier = Modifier
) {
    val S = LocalStrings.current
    val context = LocalContext.current
    var showCopyDisclaimer by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    if (showCopyDisclaimer) {
        CopyDisclaimerDialog(
            visible = true, label = label,
            onConfirmCopy = {
                copyToClipboard(context, label, text, S.copiedTip)
                onCopyConfirmed(); showCopyDisclaimer = false
            },
            onDismiss = { showCopyDisclaimer = false },
            onNeverShowAgain = onHideCopyWarning
        )
    }

    if (showQrDialog) {
        QrDialog(label = label, text = text, onDismiss = { showQrDialog = false })
    }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilledTonalButton(
            onClick = {
                if (hideCopyWarning) { copyToClipboard(context, label, text, S.copiedTip); onCopyConfirmed() }
                else showCopyDisclaimer = true
            },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(Icons.Filled.ContentCopy, null, Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp)); Text(S.copy, style = MaterialTheme.typography.labelSmall)
        }
        FilledTonalButton(
            onClick = { showQrDialog = true },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(Icons.Filled.QrCode2, null, Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp)); Text("QR", style = MaterialTheme.typography.labelSmall)
        }
        FilledTonalButton(
            onClick = { ShareUtil.shareAsFile(context, label, text, "${label.replace(" ", "_").lowercase()}.txt") },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(Icons.Filled.Share, null, Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp)); Text(S.share, style = MaterialTheme.typography.labelSmall)
        }
    }
}