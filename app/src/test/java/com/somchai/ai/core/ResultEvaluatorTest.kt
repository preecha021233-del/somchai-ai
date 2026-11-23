package com.somchai.ai.core

import com.somchai.ai.core.processing.ProcessingResult
import com.somchai.ai.core.processing.ResultEvaluator
import org.junit.Assert.*
import org.junit.Test

class ResultEvaluatorTest {
    @Test fun blocksUnsupportedSideEffectClaims() {
        val r = ResultEvaluator().evaluate(ProcessingResult("ได้จองให้แล้ว", "task.plan"))
        assertFalse(r.safe)
        assertTrue(r.text.contains("ระงับ"))
    }
    @Test fun addsPlanDisclaimer() {
        val r = ResultEvaluator().evaluate(ProcessingResult("แผน: ศึกษา", "task.plan"))
        assertTrue(r.safe)
        assertTrue(r.text.contains("ยังไม่ได้ลงมือทำ"))
    }
    @Test fun emptyAnswerIsUnsafe() {
        assertFalse(ResultEvaluator().evaluate(ProcessingResult(" ", "unknown")).safe)
    }
}
