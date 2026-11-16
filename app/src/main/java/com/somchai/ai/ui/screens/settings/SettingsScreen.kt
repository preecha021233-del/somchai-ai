package com.somchai.ai.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.somchai.ai.ui.components.SomchaiCard
import com.somchai.ai.ui.theme.*

@Composable
fun SettingsScreen(
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
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = SomchaiTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = "Appearance") {
            SettingsItem(title = "Theme", value = "Dark (Forced)")
            SettingsItem(title = "Language", value = "ไทย / English")
        }

        Spacer(modifier = Modifier.height(20.dp))

        SettingsSection(title = "Agent Behavior") {
            SettingsItem(title = "Response Style", value = "Concise")
            SettingsItem(title = "Proactivity Level", value = "Low")
            SettingsItem(title = "Memory Retention", value = "90 days")
        }

        Spacer(modifier = Modifier.height(20.dp))

        SettingsSection(title = "System") {
            SettingsItem(title = "Version", value = "0.1.0-alpha")
            SettingsItem(title = "Build", value = "2026.11.16")
            SettingsItem(title = "Export Logs", value = "")
            SettingsItem(title = "Clear Working Memory", value = "")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = SomchaiTextPrimary,
        modifier = Modifier.padding(bottom = 10.dp)
    )
    SomchaiCard {
        content()
    }
}

@Composable
private fun SettingsItem(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = SomchaiTextPrimary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SomchaiTextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = SomchaiTextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
