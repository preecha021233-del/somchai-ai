package com.somchai.ai.core.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalMemoryStoreTest {
    private class PrefixCipher : MemoryCipher {
        override fun encrypt(plaintext: ByteArray): ByteArray =
            "ENC:".toByteArray() + plaintext
        override fun decrypt(ciphertext: ByteArray): ByteArray {
            val prefix = "ENC:".toByteArray()
            require(ciphertext.take(prefix.size).toByteArray().contentEquals(prefix))
            return ciphertext.copyOfRange(prefix.size, ciphertext.size)
        }
    }

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

        val store = LocalMemoryStore(directory, PrefixCipher())
        assertEquals("legacy memory", store.all().first().content)
        assertFalse(java.io.File(directory, "memory.jsonl").exists())
        val encrypted = java.io.File(directory, "memory.enc").readBytes()
        assertTrue(String(encrypted, 0, 4) == "ENC:")
        val migrated = org.json.JSONObject(
            String(PrefixCipher().decrypt(encrypted), Charsets.UTF_8).lineSequence().first()
        )
        assertEquals(1, migrated.getInt("schemaVersion"))
    }

    @Test fun importsLegacyOwnerNotesIntoEncryptedMemory() {
        val directory = temporaryFolder.newFolder("legacy-notes")
        val legacyFile = java.io.File(directory, "notes.txt")
        legacyFile.writeText("เจ้าของต้องการคำตอบกระชับ\nวิธีสำรองข้อมูล\n")

        val store = LocalMemoryStore(directory, PrefixCipher())
        val notes = store.all(500).filter { it.category == "owner-note" }
        assertTrue(String(java.io.File(directory, "memory.enc").readBytes(), 0, 4) == "ENC:")

        assertEquals(2, notes.size)
        assertTrue(notes.any { it.content == "เจ้าของต้องการคำตอบกระชับ" })
        assertTrue(notes.any { it.content == "วิธีสำรองข้อมูล" })
        assertFalse(legacyFile.exists())
        assertFalse(java.io.File(directory, "notes.txt.migrated").exists())
    }

    @Test fun exportsAndImportsAuthenticatedEncryptedMemory() {
        val sourceDir = temporaryFolder.newFolder("export-source")
        val source = LocalMemoryStore(sourceDir, PrefixCipher())
        val original = source.add("ข้อมูลสำรองที่เข้ารหัส", "owner-note")
        val exported = source.exportEncrypted()
        assertTrue(String(exported, 0, 4) == "ENC:")

        val destination = LocalMemoryStore(temporaryFolder.newFolder("export-destination"), PrefixCipher())
        destination.add("ข้อมูลเดิม")
        assertEquals(1, destination.importEncrypted(exported))
        assertEquals(listOf(original), destination.all(500))
    }

    @Test fun authenticatedExportImportPreservesExactIdsCategoriesAndContents() {
        val key = javax.crypto.spec.SecretKeySpec(ByteArray(32) { (it + 11).toByte() }, "AES")
        val cipher = AesGcmMemoryCipher { key }
        val source = LocalMemoryStore(temporaryFolder.newFolder("authenticated-export-source"), cipher)
        val first = source.add("สอบวันที่ 30", "owner-note")
        val second = source.add("โครงการ SOMCHAI", "experience")
        val exported = source.exportEncrypted()

        val destination = LocalMemoryStore(temporaryFolder.newFolder("authenticated-export-destination"), cipher)
        destination.add("ข้อมูลเก่าที่ต้องถูกแทนที่เมื่อ import สำเร็จ")
        assertEquals(2, destination.importEncrypted(exported))
        assertEquals(listOf(second, first), destination.all(500))
    }

    @Test fun rejectsTamperedImportWithoutReplacingExistingMemory() {
        val key = javax.crypto.spec.SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")
        val authenticatedCipher = AesGcmMemoryCipher { key }
        val source = LocalMemoryStore(temporaryFolder.newFolder("tamper-source"), authenticatedCipher)
        source.add("ต้นฉบับ")
        val tampered = source.exportEncrypted().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }

        val destination = LocalMemoryStore(temporaryFolder.newFolder("tamper-destination"), authenticatedCipher)
        destination.add("ข้อมูลที่ต้องเก็บไว้")
        org.junit.Assert.assertThrows(java.security.GeneralSecurityException::class.java) {
            destination.importEncrypted(tampered)
        }
        assertEquals("ข้อมูลที่ต้องเก็บไว้", destination.all().first().content)
    }

    @Test fun encryptsMemoryAndBackupThroughInjectedCipher() {
        val directory = temporaryFolder.newFolder("encrypted")
        val store = LocalMemoryStore(directory, PrefixCipher())
        val original = store.add("secret local note")
        val data = java.io.File(directory, "memory.enc").readBytes()
        assertTrue(String(data, 0, 4) == "ENC:")
        assertTrue(store.backup())
        store.clear()
        assertEquals(1, store.restoreBackup())
        assertEquals(listOf(original), store.all(500))
    }

    @Test fun exactMemoryIdLookupWinsOverAnyMatchingPrefix() {
        val store = LocalMemoryStore(temporaryFolder.newFolder("exact-id"))
        val entry = store.add("รายการที่ต้องเรียกคืนด้วยรหัส")
        val exact = store.search(entry.id)
        assertEquals(1, exact.size)
        assertEquals(entry.id, exact.single().id)
    }

    @Test fun searchesMemoryByDisplayedIdPrefix() {
        val store = LocalMemoryStore(temporaryFolder.newFolder("id-search"))
        val entry = store.add("เป้าหมายโครงการ SOMCHAI")
        assertEquals(entry.id, store.search(entry.id.take(8)).single().id)
        assertEquals(entry.id, store.search(entry.id).single().id)
    }

    @Test fun requiresAllSearchTermsToAvoidUnrelatedMatches() {
        val store = LocalMemoryStore(temporaryFolder.newFolder("strict-search"))
        store.add("SOMCHAI เป็นโครงการ AI")
        store.add("SOMCHAI ใช้ระบบความจำ")
        assertEquals(1, store.search("SOMCHAI โครงการ").size)
    }


    @Test fun searchesThaiTextWithoutDependingOnWhitespace() {
        val store = LocalMemoryStore(temporaryFolder.newFolder("thai-search"))
        val entry = store.add("สอบวันที่ 30 ต้องทบทวนบทเรียน")
        assertEquals(entry.id, store.search("สอบวันที่30").single().id)
        assertEquals(entry.id, store.search("  ${entry.id.take(8)}  ").single().id)
    }

    @Test fun retrievesOnlyRelevantMemoriesWithBoundedResults() {
        val store = LocalMemoryStore(temporaryFolder.newFolder("relevant"))
        val relevant = store.add("โครงการ SOMCHAI พัฒนา AI ภายใน", "owner-note")
        store.add("รายการซื้อของใช้ในบ้าน", "experience")
        val matches = store.relevantFor("SOMCHAI โครงการ", 3)
        assertEquals(relevant.id, matches.first().id)
        assertTrue(matches.size <= 3)
    }


    @Test fun failedCandidateVerificationPreservesExistingMemory() {
        class ToggleVerifyCipher : MemoryCipher {
            private val delegate = PrefixCipher()
            var failNextDecrypt = false
            override fun encrypt(plaintext: ByteArray): ByteArray = delegate.encrypt(plaintext)
            override fun decrypt(ciphertext: ByteArray): ByteArray {
                if (failNextDecrypt) {
                    failNextDecrypt = false
                    error("simulated candidate verification failure")
                }
                return delegate.decrypt(ciphertext)
            }
        }
        val directory = temporaryFolder.newFolder("transactional-memory")
        val cipher = ToggleVerifyCipher()
        val store = LocalMemoryStore(directory, cipher)
        store.add("ข้อมูลเดิมต้องไม่สูญหาย")
        val originalBytes = java.io.File(directory, "memory.enc").readBytes()

        cipher.failNextDecrypt = true
        org.junit.Assert.assertThrows(IllegalStateException::class.java) {
            store.add("ข้อมูลใหม่ที่ยังบันทึกไม่สำเร็จ")
        }

        assertTrue(originalBytes.contentEquals(java.io.File(directory, "memory.enc").readBytes()))
        assertEquals(listOf("ข้อมูลเดิมต้องไม่สูญหาย"), store.all().map { it.content })
        assertFalse(java.io.File(directory, "memory.enc.tmp").exists())
    }

    @Test fun restoresRollbackFileAfterInterruptedMemoryRewrite() {
        val directory = temporaryFolder.newFolder("memory-recovery")
        val original = LocalMemoryStore(directory)
        original.add("ข้อมูลก่อนระบบหยุด")
        val live = java.io.File(directory, "memory.enc")
        val rollback = java.io.File(directory, "memory.enc.rollback")
        live.renameTo(rollback)

        val recovered = LocalMemoryStore(directory)
        assertEquals("ข้อมูลก่อนระบบหยุด", recovered.all().single().content)
        assertFalse(rollback.exists())
    }



    @Test fun rejectsOversizedMemoryStoreBeforeReadingWholeFile() {
        val directory = temporaryFolder.newFolder("oversized-memory")
        val file = java.io.File(directory, "memory.enc")
        file.writeBytes(ByteArray(1_048_577))
        val store = LocalMemoryStore(directory, PlaintextMemoryCipher)

        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { store.all() }
        assertEquals(1_048_577L, file.length())
    }

    @Test fun rejectsDuplicateIdsInAuthenticatedMemoryImport() {
        val directory = temporaryFolder.newFolder("duplicate-memory-import")
        val id = java.util.UUID.randomUUID().toString()
        val line = org.json.JSONObject().put("schemaVersion", 1).put("id", id)
            .put("category", "experience").put("content", "รายการ").put("createdAt", "2026-11-23T00:00:00Z").toString()
        val duplicatePayload = (line + "\n" + line + "\n").toByteArray(Charsets.UTF_8)
        val store = LocalMemoryStore(directory, PlaintextMemoryCipher)
        store.add("ข้อมูลเดิม")

        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            store.importEncrypted(duplicatePayload)
        }
        assertEquals("ข้อมูลเดิม", store.all().single().content)
    }


}
