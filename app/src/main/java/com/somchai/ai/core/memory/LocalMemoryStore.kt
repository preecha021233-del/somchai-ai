package com.somchai.ai.core.memory

import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.text.BreakIterator
import java.util.Locale

data class MemoryEntry(
    val id: String,
    val category: String,
    val content: String,
    val createdAt: String
)

/** App-private, encrypted in production, local-only memory. Nothing is uploaded automatically. */
class LocalMemoryStore(
    memoryDirectory: File,
    private val cipher: MemoryCipher = PlaintextMemoryCipher,
    private val log: (String, String) -> Unit = { _, _ -> }
) {
    private val file = File(memoryDirectory, "memory.enc")
    private val legacyFile = File(memoryDirectory, "memory.jsonl")
    private val backupFile = File(memoryDirectory, "memory-backup.enc")
    private val lock = Any()

    init {
        migrateLegacyRecords()
        migrateLegacyNotes()
    }

    fun add(content: String, category: String = "experience"): MemoryEntry {
        val clean = content.trim()
        require(clean.isNotEmpty()) { "Memory cannot be empty" }
        require(clean.length <= 2000) { "Memory is limited to 2,000 characters" }
        require(category.matches(Regex("[a-z][a-z0-9_-]{1,23}"))) { "Invalid memory category" }
        val entry = MemoryEntry(UUID.randomUUID().toString(), category, clean, java.time.Instant.now().toString())
        synchronized(lock) {
            file.parentFile?.mkdirs()
            rewrite(readEntries() + entry)
        }
        log("MEMORY_ADDED", "id=${entry.id} category=${entry.category}")
        return entry
    }

    fun all(limit: Int = 200): List<MemoryEntry> = synchronized(lock) {
        readEntries().takeLast(limit.coerceIn(1, 500)).reversed()
    }

    fun search(query: String, limit: Int = 50): List<MemoryEntry> {
        val clean = query.trim().lowercase()
        if (clean.isEmpty()) return all(limit)
        return synchronized(lock) {
            val entries = readEntries().asReversed()
            val idMatches = entries.filter { it.id.trim().lowercase().startsWith(clean) }
            if (idMatches.isNotEmpty()) return@synchronized idMatches.take(limit.coerceIn(1, 200))

            // Thai queries are often typed without spaces. Compare a compact form first,
            // then retain all-term matching for queries that contain deliberate word breaks.
            val compactQuery = clean.filterNot { it.isWhitespace() }
            val compactMatches = entries.filter { entry ->
                entry.content.lowercase().filterNot { it.isWhitespace() }.contains(compactQuery) ||
                    entry.category.lowercase().filterNot { it.isWhitespace() }.contains(compactQuery)
            }
            if (compactMatches.isNotEmpty()) return@synchronized compactMatches.take(limit.coerceIn(1, 200))

            val terms = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
            entries.filter { entry ->
                terms.all { term ->
                    entry.content.lowercase().contains(term) || entry.category.lowercase().contains(term)
                }
            }.take(limit.coerceIn(1, 200))
        }
    }
    /**
     * Retrieves a small, ranked set of explicitly stored memories for chat context.
     * It never returns the entire store and never sends memory to a remote provider.
     */
    fun relevantFor(query: String, limit: Int = 3): List<MemoryEntry> {
        val clean = query.trim().lowercase()
        if (clean.isEmpty()) return emptyList()
        val iterator = BreakIterator.getWordInstance(Locale("th", "TH"))
        iterator.setText(clean)
        val tokens = linkedSetOf<String>()
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val token = clean.substring(start, end).trim()
            if (token.any { it.isLetterOrDigit() } && token.length >= 2) tokens.add(token)
            start = end
            end = iterator.next()
        }
        if (tokens.isEmpty()) {
            val compact = clean.filterNot { it.isWhitespace() }
            if (compact.length < 2) return emptyList()
            tokens.add(compact.take(24))
        }
        return synchronized(lock) {
            readEntries().asReversed().mapIndexedNotNull { index, entry ->
                val searchable = (entry.content + " " + entry.category).lowercase()
                val score = tokens.count { token -> searchable.contains(token) }
                if (score == 0) null else Triple(index, score, entry)
            }.sortedWith(compareByDescending<Triple<Int, Int, MemoryEntry>> { it.second }
                .thenBy { it.first })
                .take(limit.coerceIn(1, 5))
                .map { it.third }
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
        val temp = File(file.parentFile, "memory-backup.enc.tmp")
        file.copyTo(temp, overwrite = true)
        if (!temp.renameTo(backupFile)) {
            temp.copyTo(backupFile, overwrite = true)
            temp.delete()
        }
        log("MEMORY_BACKUP", "records=${readEntries().size}")
        true
    }

    fun restoreBackup(): Int = synchronized(lock) {
        require(backupFile.isFile) { "ไม่พบไฟล์สำรองความจำ" }
        val restored = decodeRecords(cipher.decrypt(backupFile.readBytes()))
        require(restored.size <= 500) { "ไฟล์สำรองมีจำนวนรายการเกินกำหนด" }
        require(restored.map { it.id }.distinct().size == restored.size) { "ไฟล์สำรองมีรหัสซ้ำ" }
        rewrite(restored)
        log("MEMORY_RESTORE", "records=${restored.size}")
        restored.size
    }

    /**
     * Returns the encrypted store bytes for owner-controlled export.
     * The payload is bound to this installation's Android Keystore key and cannot be
     * decrypted on another installation unless the same key is available there.
     */
    fun exportEncrypted(): ByteArray = synchronized(lock) {
        require(file.isFile) { "ยังไม่มีข้อมูลความจำให้ส่งออก" }
        val encrypted = file.readBytes()
        require(encrypted.size <= 1_048_576) { "ไฟล์ความจำมีขนาดเกินกำหนด" }
        // Authenticate and validate before allowing an export.
        val entries = decodeRecords(cipher.decrypt(encrypted))
        require(entries.size <= 500) { "จำนวนรายการความจำเกินกำหนด" }
        log("MEMORY_EXPORTED", "records=${entries.size} bytes=${encrypted.size}")
        encrypted.copyOf()
    }

    /** Imports only authenticated encrypted data and leaves the current store untouched on failure. */
    fun importEncrypted(encrypted: ByteArray): Int = synchronized(lock) {
        require(encrypted.isNotEmpty() && encrypted.size <= 1_048_576) { "ไฟล์นำเข้ามีขนาดไม่ถูกต้อง" }
        val restored = decodeRecords(cipher.decrypt(encrypted))
        require(restored.size <= 500) { "ไฟล์นำเข้ามีจำนวนรายการเกินกำหนด" }
        require(restored.map { it.id }.distinct().size == restored.size) { "ไฟล์นำเข้ามีรหัสซ้ำ" }
        // rewrite() encrypts with the current installation key, so only a successfully
        // authenticated payload can replace the current data.
        rewrite(restored)
        val verified = readEntries()
        check(verified == restored) { "ตรวจสอบข้อมูลหลังนำเข้าไม่สำเร็จ" }
        log("MEMORY_IMPORTED", "records=${restored.size}")
        restored.size
    }

    fun clear(): Int = synchronized(lock) {
        val count = readEntries().size
        rewrite(emptyList())
        log("MEMORY_CLEARED", "count=$count")
        count
    }

    private fun readEntries(): List<MemoryEntry> {
        if (!file.isFile) return emptyList()
        return decodeRecords(cipher.decrypt(file.readBytes()))
    }

    private fun decodeRecords(bytes: ByteArray): List<MemoryEntry> =
        String(bytes, Charsets.UTF_8).lineSequence()
            .filter { it.isNotBlank() }
            .map { decode(it) }
            .toList()

    private fun trimToLimit(entries: List<MemoryEntry>): List<MemoryEntry> =
        entries.takeLast(500)

    private fun rewrite(entries: List<MemoryEntry>) {
        val content = trimToLimit(entries).joinToString(
            separator = "\n",
            postfix = if (entries.isEmpty()) "" else "\n"
        ) { encode(it) }.toByteArray(Charsets.UTF_8)
        val encrypted = cipher.encrypt(content)
        val temp = File(file.parentFile, "memory.enc.tmp")
        temp.writeBytes(encrypted)
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }

    private fun migrateLegacyRecords() = synchronized(lock) {
        if (!legacyFile.isFile) return@synchronized
        val legacyLines = runCatching { legacyFile.readLines().filter { it.isNotBlank() } }.getOrNull()
            ?: return@synchronized
        val legacyEntries = runCatching { legacyLines.map { decode(it) } }.getOrNull()
            ?: return@synchronized
        val current = runCatching { readEntries() }.getOrNull() ?: return@synchronized
        val existingIds = current.map { it.id }.toSet()
        val merged = current + legacyEntries.filterNot { it.id in existingIds }
        rewrite(merged)
        val verified = runCatching { readEntries().map { it.id }.toSet() }.getOrNull()
            ?: return@synchronized
        if (legacyEntries.all { it.id in verified }) {
            check(legacyFile.delete()) { "Could not retire migrated plaintext memory file" }
            log("MEMORY_ENCRYPTED_MIGRATION", "records=${legacyEntries.size}")
        }
    }

    private fun migrateLegacyNotes() = synchronized(lock) {
        val notesFile = File(file.parentFile, "notes.txt")
        if (!notesFile.isFile) return@synchronized
        val notes = runCatching { notesFile.readLines().map { it.trim() }.filter { it.isNotEmpty() } }
            .getOrNull() ?: return@synchronized
        if (notes.isEmpty()) return@synchronized

        // If the Keystore key is unavailable, preserve legacy notes and avoid crashing startup.
        val existing = runCatching { readEntries() }.getOrNull() ?: return@synchronized
        val existingNotes = existing.filter { it.category == "owner-note" }.map { it.content }.toSet()
        val missing = notes.distinct().filterNot { it in existingNotes }
        val availableSlots = (500 - existing.size).coerceAtLeast(0)
        val toImport = missing.takeLast(availableSlots)
        if (toImport.isNotEmpty()) {
            val imported = toImport.map { note ->
                MemoryEntry(UUID.randomUUID().toString(), "owner-note", note.take(2000), java.time.Instant.now().toString())
            }
            rewrite(existing + imported)
        }

        val verified = runCatching {
            readEntries().filter { it.category == "owner-note" }.map { it.content }.toSet()
        }.getOrNull() ?: return@synchronized
        if (notes.all { it in verified }) {
            check(notesFile.delete()) { "Could not retire migrated plaintext notes file" }
            log("MEMORY_LEGACY_NOTES_MIGRATED", "records=${notes.size}")
        }
    }

    private fun encode(entry: MemoryEntry): String = JSONObject()
        .put("schemaVersion", 1).put("id", entry.id).put("category", entry.category)
        .put("content", entry.content).put("createdAt", entry.createdAt).toString()

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
