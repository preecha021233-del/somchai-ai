package com.somchai.ai.ui.screens.dashboard

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somchai.ai.ui.components.SomchaiCard
import com.somchai.ai.ui.components.StatusBadge
import com.somchai.ai.ui.components.StatusType
import com.somchai.ai.ui.theme.*

@Composable
fun DashboardScreen(
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToMemory: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToOwner: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SomchaiBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Header
        Text(
            text = "Somchai",
            style = MaterialTheme.typography.headlineLarge,
            color = SomchaiTextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Owner-Controlled AI Assistant",
            style = MaterialTheme.typography.bodyMedium,
            color = SomchaiTextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // System Status Card
        SomchaiCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "System Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = SomchaiTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All systems operational",
                        style = MaterialTheme.typography.bodySmall,
                        color = SomchaiTextSecondary
                    )
                }
                StatusBadge(status = StatusType.NORMAL, text = "Active")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Actions Grid
        Text(
            text = "Quick Access",
            style = MaterialTheme.typography.titleMedium,
            color = SomchaiTextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = "Security",
                subtitle = "Center",
                icon = Icons.Outlined.Security,
                onClick = onNavigateToSecurity,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Memory",
                subtitle = "Management",
                icon = Icons.Outlined.Memory,
                onClick = onNavigateToMemory,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = "Tasks",
                subtitle = "Automation",
                icon = Icons.Outlined.TaskAlt,
                onClick = onNavigateToTasks,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Owner",
                subtitle = "Control",
                icon = Icons.Outlined.AdminPanelSettings,
                onClick = onNavigateToOwner,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Activity
        Text(
            text = "Recent Activity",
            style = MaterialTheme.typography.titleMedium,
            color = SomchaiTextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        SomchaiCard {
            ActivityItem(
                title = "Memory consolidation completed",
                time = "2 min ago",
                status = StatusType.NORMAL
            )
            Divider(color = SomchaiBorder, modifier = Modifier.padding(vertical = 12.dp))
            ActivityItem(
                title = "Tool execution approved by Owner",
                time = "15 min ago",
                status = StatusType.INFO
            )
            Divider(color = SomchaiBorder, modifier = Modifier.padding(vertical = 12.dp))
            ActivityItem(
                title = "Policy update applied",
                time = "1 hour ago",
                status = StatusType.NORMAL
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SomchaiCard(
        modifier = modifier,
        elevated = true
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SomchaiPrimaryLight,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = SomchaiTextPrimary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = SomchaiTextSecondary
        )
    }
}

@Composable
private fun ActivityItem(
    title: String,
    time: String,
    status: StatusType
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
        StatusBadge(status = status, text = when(status) {
            StatusType.NORMAL -> "OK"
            StatusType.INFO -> "INFO"
            else -> "!"
        })
    }
}
