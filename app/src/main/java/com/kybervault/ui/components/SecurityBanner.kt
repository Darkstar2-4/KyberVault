package com.kybervault.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kybervault.security.SecurityHardening

@Composable
fun SecurityBanner(report: SecurityHardening.ThreatReport, onDismiss: () -> Unit) {
    if (report.threatLevel == SecurityHardening.ThreatLevel.NONE) return

    val isCritical = report.hasCriticalThreats
    val isHigh = report.hasHighThreats
    var expanded by remember { mutableStateOf(isCritical || isHigh) }

    val containerColor = when {
        isCritical -> MaterialTheme.colorScheme.error
        isHigh -> MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
        else -> MaterialTheme.colorScheme.secondary
    }
    val contentColor = when {
        isCritical || isHigh -> MaterialTheme.colorScheme.onError
        else -> MaterialTheme.colorScheme.onSecondary
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.clickable { expanded = !expanded }.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isCritical || isHigh) Icons.Filled.GppBad else Icons.Filled.GppMaybe,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        isCritical -> "CRITICAL: Active threat detected"
                        isHigh -> "WARNING: Elevated risk environment"
                        report.threatLevel == SecurityHardening.ThreatLevel.MEDIUM -> "NOTICE: Emulated environment"
                        else -> "NOTICE: Potential monitoring detected"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )
                if (!isCritical) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Filled.Close, "Dismiss", tint = contentColor, modifier = Modifier.size(16.dp))
                    }
                }
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        report.summary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.9f),
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (isCritical || isHigh)
                            "Key material may be compromised. Avoid entering sensitive data."
                        else
                            "Some services on this device could intercept screen content or input.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
