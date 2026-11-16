package com.somchai.ai.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.somchai.ai.ui.components.SomchaiCard
import com.somchai.ai.ui.components.StatusBadge
import com.somchai.ai.ui.components.StatusType
import com.somchai.ai.ui.theme.*

@Composable
fun SecurityCenterScreen(
    onBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SomchaiBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = SomchaiTextPrimary
                )
            }
            Text(
                text = "Security Center",
                style = MaterialTheme.typography.headlineMedium,
                color = SomchaiTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Overall Security Posture
        SomchaiCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Security Posture",
                        style = MaterialTheme.typography.titleMedium,
                        color = SomchaiTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All critical controls active",
                        style = MaterialTheme.typography.bodySmall,
                        color = SomchaiTextSecondary
                    )
                }
                StatusBadge(status = StatusType.NORMAL, text = "Secure")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Permission Gates
        Text(
            text = "Permission Gates",
            style = MaterialTheme.typography.titleMedium,
            color = SomchaiTextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        SecurityItem(
            title = "Tool Execution",
            description = "Requires explicit Owner approval",
            status = StatusType.NORMAL,
            statusText = "Enforced"
        )
        Spacer(modifier = Modifier.height(10.dp))
        SecurityItem(
            title = "External Network Access",
            description = "Blocked by default policy",
            status = StatusType.NORMAL,
            statusText = "Restricted"
        )
        Spacer(modifier = Modifier.height(10.dp))
        SecurityItem(
            title = "Memory Write",
            description = "Audited + Owner reviewable",
            status = StatusType.NORMAL,
            statusText = "Audited"
        )
        Spacer(modifier = Modifier.height(10.dp))
        SecurityItem(
            title = "Self-Evolution",
            description = "Governed by policy engine",
            status = StatusType.WARNING,
            statusText = "Monitored"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Security Events
        Text(
            text = "Recent Security Events",
            style = MaterialTheme.typography.titleMedium,
            color = SomchaiTextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        SomchaiCard {
            SecurityEvent(
                title = "Approval granted for web search tool",
                time = "12 min ago",
                level = StatusType.INFO
            )
            Divider(color = SomchaiBorder, modifier = Modifier.padding(vertical = 12.dp))
            SecurityEvent(
                title = "Blocked unauthorized file write attempt",
                time = "1 hour ago",
                level = StatusType.WARNING
            )
            Divider(color = SomchaiBorder, modifier = Modifier.padding(vertical = 12.dp))
            SecurityEvent(
                title = "Policy engine reloaded successfully",
                time = "3 hours ago",
                level = StatusType.NORMAL
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SecurityItem(
    title: String,
    description: String,
    status: StatusType,
    statusText: String
) {
    SomchaiCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = SomchaiTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = SomchaiTextSecondary
                )
            }
            StatusBadge(status = status, text = statusText)
        }
    }
}

@Composable
private fun SecurityEvent(
    title: String,
    time: String,
    level: StatusType
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = SomchaiTextPrimary
            )
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = SomchaiTextTertiary
            )
        }
        StatusBadge(
            status = level,
            text = when (level) {
                StatusType.NORMAL -> "OK"
                StatusType.WARNING -> "WARN"
                StatusType.CRITICAL -> "CRIT"
                else -> "INFO"
            }
        )
    }
}
