package com.somchai.ai.core.plugins

import org.json.JSONArray
import org.json.JSONObject

data class PluginManifest(
    val id: String, val name: String, val version: Int, val description: String,
    val permissions: List<String>, val operation: String, val enabled: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject().put("id", id).put("name", name)
        .put("version", version).put("description", description)
        .put("permissions", JSONArray(permissions)).put("operation", operation).put("enabled", enabled)

    companion object {
        fun parse(text: String): PluginManifest {
            val json = JSONObject(text)
            val permissions = json.getJSONArray("permissions")
            return PluginManifest(
                json.getString("id"), json.getString("name"), json.getInt("version"),
                json.getString("description"),
                (0 until permissions.length()).map { permissions.getString(it) },
                json.getString("operation"), json.optBoolean("enabled", true)
            )
        }
    }
}
