package com.somchai.ai.core.brain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GgufFileValidatorTest {
    @Test fun acceptsGgufMagic() {
        val file = File.createTempFile("somchai-model", ".gguf")
        try {
            file.writeBytes(byteArrayOf(0x47, 0x47, 0x55, 0x46, 0x01))
            assertTrue(GgufFileValidator.isValid(file))
        } finally { file.delete() }
    }

    @Test fun rejectsNonGgufFile() {
        val file = File.createTempFile("somchai-model", ".gguf")
        try {
            file.writeText("not a model")
            assertFalse(GgufFileValidator.isValid(file))
        } finally { file.delete() }
    }

    @Test fun computesSha256WithoutSignedByteExpansion() {
        val file = File.createTempFile("somchai-sha", ".bin")
        try {
            file.writeText("abc")
            assertTrue(GgufFileValidator.sha256(file) == "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
        } finally { file.delete() }
    }

    @Test fun rejectsMissingFile() {
        assertFalse(GgufFileValidator.isValid(File("missing-somchai-model.gguf")))
    }
}
