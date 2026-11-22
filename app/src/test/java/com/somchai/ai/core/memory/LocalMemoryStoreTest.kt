package com.somchai.ai.core.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalMemoryStoreTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun addsSearchesPersistsAndDeletesMemory() {
        val directory = temporaryFolder.newFolder("memory")
        val store = LocalMemoryStore(directory)
        val entry = store.add("ทดสอบการสำรองข้อมูล", "skill")

        assertEquals(1, store.all().size)
        assertEquals(entry.id, store.search("สำรอง").first().id)

        val reopened = LocalMemoryStore(directory)
        assertEquals(entry.content, reopened.all().first().content)
        assertTrue(reopened.delete(entry.id))
        assertTrue(reopened.all().isEmpty())
        assertFalse(reopened.delete(entry.id))
    }

    @Test fun limitsStoredEntriesToFiveHundred() {
        val store = LocalMemoryStore(temporaryFolder.newFolder("bounded"))
        repeat(505) { store.add("entry-$it") }
        assertEquals(500, store.all(500).size)
    }
    @Test fun migratesLegacyMemoryRecordsToVersionedSchema() {
        val directory = temporaryFolder.newFolder("legacy")
        val id = java.util.UUID.randomUUID().toString()
        val timestamp = java.time.Instant.parse("2026-01-01T00:00:00Z").toString()
        val legacy = org.json.JSONObject()
            .put("id", id).put("category", "skill")
            .put("content", "legacy memory").put("createdAt", timestamp)
        java.io.File(directory, "memory.jsonl").writeText(legacy.toString() + "\n")

        val store = LocalMemoryStore(directory)
        assertEquals("legacy memory", store.all().first().content)
        assertFalse(java.io.File(directory, "memory.jsonl").exists())
        val migrated = org.json.JSONObject(java.io.File(directory, "memory.enc").readLines().first())
        assertEquals(1, migrated.getInt("schemaVersion"))
    }

    @Test fun importsLegacyOwnerNotesIntoEncryptedMemory() {
        val directory = temporaryFolder.newFolder("legacy-notes")
        val legacyFile = java.io.File(directory, "notes.txt")
        legacyFile.writeText("เจ้าของต้องการคำตอบกระชับ\nวิธีสำรองข้อมูล\n")

        val store = LocalMemoryStore(directory)
        val notes = store.all(500).filter { it.category == "owner-note" }

        assertEquals(2, notes.size)
        assertTrue(notes.any { it.content == "เจ้าของต้องการคำตอบกระชับ" })
        assertTrue(notes.any { it.content == "วิธีสำรองข้อมูล" })
        assertFalse(legacyFile.exists())
        assertFalse(java.io.File(directory, "notes.txt.migrated").exists())
    }

}
