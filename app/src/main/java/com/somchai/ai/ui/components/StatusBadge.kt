package com.somchai.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somchai.ai.ui.theme.*

enum class StatusType {
    NORMAL, WARNING, CRITICAL, INFO, IDLE
}

@Composable
fun StatusBadge(
    status: StatusType,
    text: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        StatusType.NORMAL -> SomchaiSuccessContainer to SomchaiSuccess
        StatusType.WARNING -> SomchaiWarningContainer to SomchaiWarning
        StatusType.CRITICAL -> SomchaiCriticalContainer to SomchaiCritical
        StatusType.INFO -> SomchaiInfoContainer to SomchaiInfo
        StatusType.IDLE -> SomchaiSurfaceElevated to SomchaiTextSecondary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}
