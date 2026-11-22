package com.somchai.ai.core.processing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskPlannerTest {
    private val planner = TaskPlanner()

    @Test fun createsDistinctExamPlan() {
        val plan = planner.plan("วางแผนสอบปลายภาค")
        assertEquals(TaskType.EXAM, plan.type)
        assertTrue(plan.steps.any { it.contains("ข้อสอบเก่า") })
        assertTrue(plan.steps.any { it.contains("พักผ่อน") })
    }

    @Test fun createsDistinctTravelPlan() {
        val plan = planner.plan("วางแผนเดินทางไปเชียงใหม่")
        assertEquals(TaskType.TRAVEL, plan.type)
        assertTrue(plan.steps.any { it.contains("จอง") })
        assertTrue(plan.steps.any { it.contains("สัมภาระ") })
    }

    @Test fun decomposesDevelopmentWorkIntoVerifiableSteps() {
        val plan = planner.plan("พัฒนาแอปและทดสอบ")
        assertEquals(TaskType.DEVELOPMENT, plan.type)
        assertEquals(6, plan.steps.size)
        assertTrue(plan.steps.first().contains("เกณฑ์ยอมรับ"))
        assertTrue(plan.steps.last().contains("ยังไม่ผ่าน"))
    }

    @Test fun decomposesResearchWorkAndRequiresSourceComparison() {
        val plan = planner.plan("ค้นคว้าความปลอดภัย Android")
        assertEquals(TaskType.RESEARCH, plan.type)
        assertEquals(5, plan.steps.size)
        assertTrue(plan.steps.any { it.contains("อย่างน้อยสองแหล่ง") })
    }

    @Test fun classifiesTroubleshootingAndWriting() {
        assertEquals(TaskType.TROUBLESHOOTING, planner.classify("แก้ปัญหาแอปเปิดไม่ได้"))
        assertEquals(TaskType.WRITING, planner.classify("เขียนรายงานผลการทดสอบ"))
    }

    @Test fun rejectsBlankGoal() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            planner.plan("   ")
        }
    }
}
