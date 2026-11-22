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
}
