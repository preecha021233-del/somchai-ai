package com.somchai.ai.ui.screens.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.somchai.ai.ui.components.SomchaiCard
import com.somchai.ai.ui.components.StatusBadge
import com.somchai.ai.ui.components.StatusType
import com.somchai.ai.ui.theme.*

@Composable
fun MemoryScreen(
    onBack: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

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
                text = "Memory Management",
                style = MaterialTheme.typography.headlineMedium,
                color = SomchaiTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search memories...", color = SomchaiTextTertiary) },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = SomchaiTextSecondary)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SomchaiPrimary,
                unfocusedBorderColor = SomchaiBorder,
                focusedContainerColor = SomchaiSurface,
                unfocusedContainerColor = SomchaiSurface,
                cursorColor = SomchaiPrimaryLight,
                focusedTextColor = SomchaiTextPrimary,
                unfocusedTextColor = SomchaiTextPrimary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Memory Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MemoryStatCard(
                label = "Long-term",
                value = "1,284",
                modifier = Modifier.weight(1f)
            )
            MemoryStatCard(
                label = "Working",
                value = "47",
                modifier = Modifier.weight(1f)
            )
            MemoryStatCard(
                label = "Pending",
                value = "3",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Recent Memories",
            style = MaterialTheme.typography.titleMedium,
            color = SomchaiTextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        MemoryItem(
            title = "User preference: prefers concise answers",
            category = "Preference",
            time = "Today",
            status = StatusType.NORMAL
        )
        Spacer(modifier = Modifier.height(10.dp))
        MemoryItem(
            title = "Project context: Somchai full system design",
            category = "Context",
            time = "Today",
            status = StatusType.NORMAL
        )
        Spacer(modifier = Modifier.height(10.dp))
        MemoryItem(
            title = "Tool result: web search about Android security",
            category = "Tool Output",
            time = "Yesterday",
            status = StatusType.INFO
        )
        Spacer(modifier = Modifier.height(10.dp))
        MemoryItem(
            title = "Policy change: restricted network by default",
            category = "System",
            time = "2 days ago",
            status = StatusType.WARNING
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun MemoryStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    SomchaiCard(modifier = modifier, elevated = true) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = SomchaiPrimaryLight,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = SomchaiTextSecondary
        )
    }
}

@Composable
private fun MemoryItem(
    title: String,
    category: String,
    time: String,
    status: StatusType
) {
    SomchaiCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SomchaiTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(status = status, text = category)
                    Text(
                        text = time,
                        style = MaterialTheme.typography.bodySmall,
                        color = SomchaiTextTertiary
                    )
                }
            }
        }
    }
}
