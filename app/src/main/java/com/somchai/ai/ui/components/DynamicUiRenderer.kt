package com.somchai.ai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.somchai.ai.ui.theme.SomchaiSurface
import com.somchai.ai.ui.theme.SomchaiTextPrimary
import com.somchai.ai.ui.theme.SomchaiTextSecondary
import org.json.JSONArray
import org.json.JSONObject

/** Renders a deliberately small declarative UI schema; HTML, JS and callbacks are never evaluated. */
@Composable
fun DynamicUiRenderer(configJson: String, modifier: Modifier = Modifier) {
    val components = runCatching {
        require(configJson.toByteArray().size <= 8_192)
        val root = JSONObject(configJson)
        val array = root.getJSONArray("components")
        require(array.length() <= 12)
        (0 until array.length()).map { array.getJSONObject(it) }
    }.getOrDefault(emptyList<JSONObject>())

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        components.forEach { item ->
            val type = item.optString("type")
            val title = item.optString("title").take(100)
            val body = item.optString("body").take(500)
            when (type) {
                "text" -> Text(body, color = SomchaiTextSecondary, style = MaterialTheme.typography.bodyMedium)
                "card", "metric", "status" -> Card(
                    colors = CardDefaults.cardColors(containerColor = SomchaiSurface)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        if (title.isNotBlank()) Text(title, color = SomchaiTextPrimary, style = MaterialTheme.typography.titleMedium)
                        if (body.isNotBlank()) Text(body, color = SomchaiTextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
