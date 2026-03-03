package com.kybervault.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kybervault.i18n.LocalStrings

@Composable
fun KeyStatusBar(hasKey: Boolean, keyCount: Int, statusMessage: String, error: String?) {
    val S = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val hasContent = statusMessage.isNotBlank() || error != null

    Surface(
        color = when {
            error != null -> MaterialTheme.colorScheme.errorContainer
            hasKey -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.clickable(enabled = hasContent) { expanded = !expanded }.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (error != null) Icons.Filled.Warning else if (hasKey) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    null,
                    tint = when {
                        error != null -> MaterialTheme.colorScheme.error
                        hasKey -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        error ?: statusMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            error != null -> MaterialTheme.colorScheme.error
                            hasKey -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = if (expanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (error != null) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                if (hasContent) {
                    Spacer(Modifier.width(4.dp))
                    Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        S.expand, modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    if (keyCount > 0) {
                        Text(S.aliasesInRam(keyCount), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (error != null && statusMessage.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(statusMessage, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (error != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(error, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
