package com.somchai.ai.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
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
fun TasksScreen(
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
                text = "Tasks & Automation",
                style = MaterialTheme.typography.headlineMedium,
                color = SomchaiTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Active Summary
        SomchaiCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("2", style = MaterialTheme.typography.headlineMedium, color = SomchaiPrimaryLight, fontWeight = FontWeight.Bold)
                    Text("Running", style = MaterialTheme.typography.bodySmall, color = SomchaiTextSecondary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("5", style = MaterialTheme.typography.headlineMedium, color = SomchaiWarning, fontWeight = FontWeight.Bold)
                    Text("Queued", style = MaterialTheme.typography.bodySmall, color = SomchaiTextSecondary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("18", style = MaterialTheme.typography.headlineMedium, color = SomchaiSuccess, fontWeight = FontWeight.Bold)
                    Text("Completed", style = MaterialTheme.typography.bodySmall, color = SomchaiTextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Active Tasks",
            style = MaterialTheme.typography.titleMedium,
            color = SomchaiTextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        TaskItem(
            title = "Daily memory consolidation",
            subtitle = "Scheduled • Progress 67%",
            status = StatusType.NORMAL,
            statusText = "Running"
        )
        Spacer(modifier = Modifier.height(10.dp))
        TaskItem(
            title = "Security audit log export",
            subtitle = "Owner requested • Waiting approval",
            status = StatusType.WARNING,
            statusText = "Pending"
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Recent Completed",
            style = MaterialTheme.typography.titleMedium,
            color = SomchaiTextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        TaskItem(
            title = "Index new documents",
            subtitle = "Completed 14 min ago",
            status = StatusType.NORMAL,
            statusText = "Done"
        )
        Spacer(modifier = Modifier.height(10.dp))
        TaskItem(
            title = "Policy validation run",
            subtitle = "Completed 1 hour ago",
            status = StatusType.NORMAL,
            statusText = "Done"
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun TaskItem(
    title: String,
    subtitle: String,
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SomchaiTextSecondary
                )
            }
            StatusBadge(status = status, text = statusText)
        }
    }
}
