package com.somchai.ai.core.memory

import org.json.JSONObject
import java.io.File
import java.util.UUID

data class MemoryEntry(
    val id: String,
    val category: String,
    val content: String,
    val createdAt: String
)

/** App-private, local-only memory. Nothing is uploaded or collected automatically. */
class LocalMemoryStore(
    memoryDirectory: File,
    private val log: (String, String) -> Unit = { _, _ -> }
) {
    private val file = File(memoryDirectory, "memory.jsonl")
    private val lock = Any()

    init {
        migrateLegacyRecords()
    }

    fun add(content: String, category: String = "experience"): MemoryEntry {
        val clean = content.trim()
        require(clean.isNotEmpty()) { "Memory cannot be empty" }
        require(clean.length <= 2000) { "Memory is limited to 2,000 characters" }
        require(category.matches(Regex("[a-z][a-z0-9_-]{1,23}"))) { "Invalid memory category" }
        val entry = MemoryEntry(UUID.randomUUID().toString(), category, clean, java.time.Instant.now().toString())
        synchronized(lock) {
            file.parentFile?.mkdirs()
            file.appendText(encode(entry) + "\n")
            trimToLimit()
        }
        log("MEMORY_ADDED", "id=${entry.id} category=${entry.category}")
        return entry
    }

    fun all(limit: Int = 200): List<MemoryEntry> = synchronized(lock) {
        readEntries().takeLast(limit.coerceIn(1, 500)).reversed()
    }

    fun search(query: String, limit: Int = 50): List<MemoryEntry> {
        val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (terms.isEmpty()) return all(limit)
        return synchronized(lock) {
            readEntries().asReversed()
                .filter { entry -> terms.any { term -> entry.content.lowercase().contains(term) || entry.category.lowercase().contains(term) } }
                .take(limit.coerceIn(1, 200))
        }
    }

    fun delete(id: String): Boolean = synchronized(lock) {
        val entries = readEntries()
        if (entries.none { it.id == id }) return@synchronized false
        rewrite(entries.filterNot { it.id == id })
        log("MEMORY_DELETED", "id=$id")
        true
    }

    fun backup(): Boolean = synchronized(lock) {
        if (!file.exists()) return@synchronized false
        val backup = File(file.parentFile, "memory-backup.jsonl")
        val temp = File(file.parentFile, "memory-backup.jsonl.tmp")
        file.copyTo(temp, overwrite = true)
        if (!temp.renameTo(backup)) {
            temp.copyTo(backup, overwrite = true)
            temp.delete()
        }
        log("MEMORY_BACKUP", "records=${readEntries().size}")
        true
    }

    fun restoreBackup(): Int = synchronized(lock) {
        val backup = File(file.parentFile, "memory-backup.jsonl")
        require(backup.isFile) { "ไม่พบไฟล์สำรองความจำ" }
        val restored = backup.readLines().filter { it.isNotBlank() }.map { decode(it) }
        require(restored.size <= 500) { "ไฟล์สำรองมีจำนวนรายการเกินกำหนด" }
        require(restored.map { it.id }.distinct().size == restored.size) { "ไฟล์สำรองมีรหัสซ้ำ" }
        rewrite(restored)
        log("MEMORY_RESTORE", "records=${restored.size}")
        restored.size
    }

    fun clear(): Int = synchronized(lock) {
        val count = readEntries().size
        rewrite(emptyList())
        log("MEMORY_CLEARED", "count=$count")
        count
    }

    private fun readEntries(): List<MemoryEntry> = runCatching {
        if (!file.exists()) emptyList()
        else file.readLines().mapNotNull { line -> runCatching { decode(line) }.getOrNull() }
    }.getOrDefault(emptyList())

    private fun trimToLimit() {
        val entries = readEntries()
        if (entries.size > 500) rewrite(entries.takeLast(500))
    }

    private fun rewrite(entries: List<MemoryEntry>) {
        val temp = File(file.parentFile, "memory.jsonl.tmp")
        temp.writeText(entries.joinToString(separator = "\n", postfix = if (entries.isEmpty()) "" else "\n") { encode(it) })
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }

    private fun encode(entry: MemoryEntry): String = JSONObject()
        .put("schemaVersion", 1).put("id", entry.id).put("category", entry.category)
        .put("content", entry.content).put("createdAt", entry.createdAt).toString()

    private fun migrateLegacyRecords() = synchronized(lock) {
        val lines = if (file.isFile) {
            runCatching { file.readLines().filter { it.isNotBlank() } }.getOrNull()
                ?: return@synchronized
        } else emptyList()
        val entries = runCatching { lines.map { decode(it) } }.getOrNull()
            ?: return@synchronized
        val hasLegacyRecords = lines.any { line ->
            runCatching { JSONObject(line).optInt("schemaVersion", 0) < 1 }.getOrDefault(true)
        }
        if (hasLegacyRecords) {
            rewrite(entries)
            log("MEMORY_MIGRATED", "schema=1 records=${entries.size}")
        }
        migrateLegacyNotes(entries)
    }

    private fun migrateLegacyNotes(existing: List<MemoryEntry>) {
        val legacyFile = File(file.parentFile, "notes.txt")
        if (!legacyFile.isFile) return
        val notes = runCatching { legacyFile.readLines().map { it.trim() }.filter { it.isNotEmpty() } }
            .getOrNull() ?: return
        if (notes.isEmpty()) return

        val existingNotes = existing.filter { it.category == "owner-note" }.map { it.content }.toSet()
        val missing = notes.distinct().filterNot { it in existingNotes }
        val availableSlots = (500 - existing.size).coerceAtLeast(0)
        val toImport = missing.takeLast(availableSlots)
        if (toImport.isNotEmpty()) {
            val imported = toImport.map { note ->
                MemoryEntry(UUID.randomUUID().toString(), "owner-note", note.take(2000), java.time.Instant.now().toString())
            }
            rewrite((existing + imported).takeLast(500))
        }

        val verified = readEntries().filter { it.category == "owner-note" }.map { it.content }.toSet()
        if (notes.all { it in verified }) {
            val archive = File(file.parentFile, "notes.txt.migrated")
            legacyFile.copyTo(archive, overwrite = true)
            if (archive.readText() == legacyFile.readText()) {
                check(legacyFile.delete()) { "Could not retire migrated legacy notes file" }
                log("MEMORY_LEGACY_NOTES_MIGRATED", "records=${notes.size}")
            }
        }
    }

    private fun decode(line: String): MemoryEntry {
        val json = JSONObject(line)
        val schemaVersion = json.optInt("schemaVersion", 0)
        require(schemaVersion in 0..1) { "Unsupported memory schema version" }
        val entry = MemoryEntry(json.getString("id"), json.getString("category"),
            json.getString("content"), json.getString("createdAt"))
        require(runCatching { UUID.fromString(entry.id) }.isSuccess) { "Invalid memory ID" }
        require(entry.category.matches(Regex("[a-z][a-z0-9_-]{1,23}"))) { "Invalid memory category" }
        require(entry.content.isNotBlank() && entry.content.length <= 2000) { "Invalid memory content" }
        require(runCatching { java.time.Instant.parse(entry.createdAt) }.isSuccess) { "Invalid memory timestamp" }
        return entry
    }
}
