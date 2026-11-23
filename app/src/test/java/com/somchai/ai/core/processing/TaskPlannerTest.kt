package com.somchai.ai.core.processing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskPlannerTest {
    private val planner = TaskPlanner()

    @Test fun createsDistinctExamPlan() {
        val plan = planner.plan("วางแผนสอบปลายภาค")
        assertEquals(TaskType.EXAM, plan.type)
        val review = plan.steps.indexOfFirst { it.contains("ทบทวนบทเรียน") }
        val pastPapers = plan.steps.indexOfFirst { it.contains("ข้อสอบเก่า") }
        val errorReview = plan.steps.indexOfFirst { it.contains("ข้อผิดพลาด") }
        val rest = plan.steps.indexOfFirst { it.contains("พักผ่อน") }
        assertTrue(review >= 0)
        assertTrue(pastPapers > review)
        assertTrue(errorReview > pastPapers)
        assertTrue(rest > errorReview)
    }

    @Test fun createsDistinctTravelPlan() {
        val plan = planner.plan("วางแผนเดินทางไปเชียงใหม่")
        assertEquals(TaskType.TRAVEL, plan.type)
        val route = plan.steps.indexOfFirst { it.contains("เส้นทาง") }
        val booking = plan.steps.indexOfFirst { it.contains("จองการเดินทาง") }
        val packing = plan.steps.indexOfFirst { it.contains("สัมภาระ") }
        val departure = plan.steps.indexOfFirst { it.contains("ออกเดินทาง") }
        assertTrue(route >= 0)
        assertTrue(booking > route)
        assertTrue(packing > booking)
        assertTrue(departure > packing)
    }

    @Test fun decomposesDevelopmentWorkIntoVerifiableSteps() {
        val plan = planner.plan("พัฒนาแอปและทดสอบ")
        assertEquals(TaskType.DEVELOPMENT, plan.type)
        assertEquals(6, plan.steps.size)
        assertTrue(plan.steps.first().contains("ข้อกำหนด"))
        assertTrue(plan.steps.first().contains("เกณฑ์ยอมรับ"))
        assertTrue(plan.steps.any { it.contains("พัฒนา") })
        assertTrue(plan.steps.any { it.contains("ทดสอบ") })
        assertTrue(plan.steps.last().contains("ยังไม่ผ่าน"))
    }

    @Test fun decomposesResearchWorkAndRequiresSourceComparison() {
        val plan = planner.plan("ค้นคว้าความปลอดภัย Android")
        assertEquals(TaskType.RESEARCH, plan.type)
        assertEquals(5, plan.steps.size)
        assertTrue(plan.steps.any { it.contains("อย่างน้อยสองแหล่ง") })
    }

    @Test fun recognizesCommonThaiResearchRequests() {
        assertEquals(TaskType.RESEARCH, planner.classify("ช่วยหาข้อมูลเปรียบเทียบโทรศัพท์"))
        assertEquals(TaskType.RESEARCH, planner.classify("สืบค้นมาตรฐานความปลอดภัย"))
    }

    @Test fun prioritizesDevelopmentOverTheExamSubstringInsideTesting() {
        assertEquals(TaskType.DEVELOPMENT, planner.classify("พัฒนาแอป Android และทดสอบระบบ"))
        assertEquals(TaskType.EXAM, planner.classify("เตรียมสอบปลายภาค"))
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

    @Test fun rejectsOversizedGoalInsteadOfSilentlyTruncatingIt() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            planner.plan("ก".repeat(1001))
        }
    }

    @Test fun rejectsControlCharactersInGoal() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            planner.plan("วางแผน\u0000งาน")
        }
    }
}
