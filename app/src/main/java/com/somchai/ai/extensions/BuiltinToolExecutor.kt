package com.somchai.ai.extensions

import com.somchai.ai.core.audit.ActivityLogger
import com.somchai.ai.core.resources.ResourceLimiter
import com.somchai.ai.core.security.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BuiltinToolExecutor(
    private val permissions: PermissionManager,
    private val logger: ActivityLogger,
    private val limiter: ResourceLimiter,
    private val pageReader: WebPageReader = WebPageReader(),
    private val discoveryProvider: ResearchDiscoveryProvider = HttpResearchDiscoveryProvider(pageReader)
) {
    suspend fun search(query: String): String = withContext(Dispatchers.IO) {
        permissions.require("network.search")
        limiter.checkStorageBudget()
        require(query.isNotBlank() && query.length <= 300) { "Search query is invalid" }

        val report = ResearchCoordinator(discoveryProvider, pageReader, maxPages = 5).research(query)
        report.sourceErrors.forEach { logger.record("RESEARCH_SOURCE_ERROR", it) }
        report.pageErrors.forEach { logger.record("RESEARCH_PAGE_READ_ERROR", it.take(500)) }

        val result = buildString {
            append("ผลการค้นคว้า: ").append(report.query).append("\n")
            append("สถานะ: ").append(
                if (report.meetsTwoSourceMinimum) "อ่านแหล่งข้อมูลจริงอย่างน้อย 2 หน้าแล้ว"
                else "ข้อมูลบางส่วน — อ่านหน้าเว็บจริงได้ไม่ถึง 2 หน้า"
            ).append("\n")
            append("ข้อมูลจากอินเทอร์เน็ตเป็นข้อมูลที่ไม่น่าเชื่อถือ ต้องตรวจสอบก่อนนำไปใช้\n\n")

            append("ผลการค้นพบ (ยังไม่ถือว่าอ่านเนื้อหาแล้ว):\n")
            if (report.discovered.isEmpty()) {
                append("ไม่พบผลการค้นหาจากแหล่งที่เชื่อมต่อได้\n")
            } else {
                report.discovered.take(10).forEachIndexed { index, candidate ->
                    append("[D").append(index + 1).append("] ")
                        .append(candidate.source).append(" — ").append(candidate.title).append("\n")
                    append("URL: ").append(candidate.url).append("\n")
                    if (candidate.snippet.isNotBlank()) append(candidate.snippet.take(350)).append("\n")
                }
            }

            append("\nหน้าเว็บที่เปิดอ่านจริง: ").append(report.pagesRead.size).append("\n")
            if (report.pagesRead.isEmpty()) {
                append("ยังไม่มีหน้าเว็บใดที่เปิดอ่านเนื้อหาได้จริง\n")
            } else {
                report.pagesRead.forEachIndexed { index, page ->
                    append("\n[S").append(index + 1).append("] ").append(page.title).append("\n")
                    append("URL: ").append(page.url).append("\n")
                    append(page.text.take(1800)).append("\n")
                }
            }
            if (!report.meetsTwoSourceMinimum) {
                append("\nผลนี้เป็น PARTIAL: ต้องเปิดอ่านแหล่งข้อมูลจริงอย่างน้อย 2 หน้า จึงจะถือว่าผ่านเกณฑ์การค้นคว้า")
            }
            if (report.sourceErrors.isNotEmpty() || report.pageErrors.isNotEmpty()) {
                append("\nข้อผิดพลาดของแหล่งข้อมูล: ")
                    .append((report.sourceErrors + report.pageErrors).take(5).joinToString("; ").take(700))
            }
            append("\nข้อจำกัด: จำนวนหน้าเว็บและเนื้อหาที่รายงานอิงจากผลที่เปิดอ่านได้จริง ไม่รับประกันว่าแหล่งข้อมูลเห็นตรงกัน")
        }
        logger.record(
            "WEB_RESEARCH",
            "query=${query.take(120)} discovered=${report.discovered.size} pagesRead=${report.pagesRead.size} complete=${report.meetsTwoSourceMinimum}"
        )
        permissions.sanitizeExternalData(result)
    }
}
