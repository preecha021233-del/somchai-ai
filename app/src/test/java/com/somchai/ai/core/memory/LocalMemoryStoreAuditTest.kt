package com.somchai.ai.core.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import javax.crypto.spec.SecretKeySpec

class LocalMemoryStoreAuditTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun recordsMemoryBackupRestoreAndMutationLifecycleEvents() {
        val key = SecretKeySpec(ByteArray(32) { (it + 7).toByte() }, "AES")
        val cipher = AesGcmMemoryCipher { key }
        val events = mutableListOf<String>()
        val source = LocalMemoryStore(
            temporaryFolder.newFolder("audit-source"),
            cipher
        ) { event, _ -> events += event }

        val entry = source.add("ข้อมูลทดสอบ")
        assertTrue(source.update(entry.id, "ข้อมูลที่แก้ไข"))
        assertTrue(source.backup())
        val backup = source.exportEncrypted()
        assertEquals(1, source.clear())
        assertEquals(1, source.restoreBackup())

        val destinationEvents = mutableListOf<String>()
        val destination = LocalMemoryStore(
            temporaryFolder.newFolder("audit-destination"),
            cipher
        ) { event, _ -> destinationEvents += event }
        assertEquals(1, destination.importEncrypted(backup))
        assertTrue(destination.delete(entry.id))

        val expectedSourceEvents = setOf(
            "MEMORY_ADDED",
            "MEMORY_UPDATED",
            "MEMORY_BACKUP",
            "MEMORY_EXPORTED",
            "MEMORY_CLEARED",
            "MEMORY_RESTORE"
        )
        assertTrue("Missing source audit events: ${expectedSourceEvents - events.toSet()}",
            events.toSet().containsAll(expectedSourceEvents))
        assertTrue(destinationEvents.contains("MEMORY_IMPORTED"))
        assertTrue(destinationEvents.contains("MEMORY_DELETED"))
    }
}
