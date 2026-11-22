package com.somchai.ai.core.processing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskPlannerTest {
    private val planner = TaskPlanner()

    @Test fun decomposesDevelopmentWorkIntoVerifiableSteps() {
        val plan = planner.plan("พัฒนาแอปและทดสอบ")
        assertEquals(6, plan.steps.size)
        assertTrue(plan.steps.first().contains("ข้อกำหนด"))
        assertTrue(plan.steps.last().contains("ข้อจำกัด"))
    }

    @Test fun decomposesResearchWork() {
        val plan = planner.plan("ค้นคว้าความปลอดภัย Android")
        assertEquals(5, plan.steps.size)
        assertTrue(plan.steps.any { it.contains("แหล่งข้อมูล") })
    }

    @Test fun rejectsBlankGoal() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            planner.plan("   ")
        }
    }
}
