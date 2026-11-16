package com.somchai.ai.ui.screens.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.somchai.ai.ui.components.PrimaryButton
import com.somchai.ai.ui.components.SomchaiCard
import com.somchai.ai.ui.components.StatusBadge
import com.somchai.ai.ui.components.StatusType
import com.somchai.ai.ui.theme.*

@Composable
fun OwnerControlScreen(
    onBack: () -> Unit = {}
) {
    var selfEvolutionEnabled by remember { mutableStateOf(false) }
    var autoApproveLowRisk by remember { mutableStateOf(true) }

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
                text = "Owner Control",
                style = MaterialTheme.typography.headlineMedium,
                color = SomchaiTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Absolute control over system behavior",
            style = MaterialTheme.typography.bodyMedium,
            color = SomchaiTextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Critical Controls
        Text(
            text = "Critical Controls",
            style = MaterialTheme.typography.titleMedium,
            color = SomchaiTextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        SomchaiCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Governed Self-Evolution",
                        style = MaterialTheme.typography.titleMedium,
                        color = SomchaiTextPrimary
                    )
                    Text(
                        text = "Allow system to propose improvements",
                        style = MaterialTheme.typography.bodySmall,
                        color = SomchaiTextSecondary
                    )
                }
                Switch(
                    checked = selfEvolutionEnabled,
                    onCheckedChange = { selfEvolutionEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SomchaiPrimaryLight,
                        checkedTrackColor = SomchaiPrimary.copy(alpha = 0.5f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SomchaiCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-approve low-risk tools",
                        style = MaterialTheme.typography.titleMedium,
                        color = SomchaiTextPrimary
                    )
                    Text(
                        text = "Skip approval for read-only operations",
                        style = MaterialTheme.typography.bodySmall,
                        color = SomchaiTextSecondary
                    )
                }
                Switch(
                    checked = autoApproveLowRisk,
                    onCheckedChange = { autoApproveLowRisk = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SomchaiPrimaryLight,
                        checkedTrackColor = SomchaiPrimary.copy(alpha = 0.5f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pending Approvals
        Text(
            text = "Pending Approvals",
            style = MaterialTheme.typography.titleMedium,
            color = SomchaiTextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        SomchaiCard {
            Text(
                text = "Execute shell command: ls -la /data",
                style = MaterialTheme.typography.bodyMedium,
                color = SomchaiTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Requested 8 min ago • Risk: Medium",
                style = MaterialTheme.typography.bodySmall,
                color = SomchaiTextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(
                    text = "Approve",
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SomchaiCritical
                    )
                ) {
                    Text("Deny")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Emergency
        Text(
            text = "Emergency Controls",
            style = MaterialTheme.typography.titleMedium,
            color = SomchaiTextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        SomchaiCard {
            Text(
                text = "Kill Switch",
                style = MaterialTheme.typography.titleMedium,
                color = SomchaiCritical
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Immediately halt all agent activity and lock the system",
                style = MaterialTheme.typography.bodySmall,
                color = SomchaiTextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = SomchaiCritical,
                    contentColor = androidx.compose.ui.graphics.Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ACTIVATE KILL SWITCH", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
