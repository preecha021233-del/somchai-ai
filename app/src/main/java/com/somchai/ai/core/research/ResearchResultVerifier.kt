package com.somchai.ai.core.research

import java.net.URI
import java.util.Locale

data class SourceSnippet(val sourceName:String,val title:String?=null,val text:String,val url:String?=null,val dateLabel:String?=null)
data class ReadPageEvidence(val title:String,val url:String,val text:String)
enum class EvidenceLevel { READ_MULTI_SOURCE, READ_SINGLE_SOURCE, DISCOVERY_ONLY, NONE }
data class VerifiedResearchReport(val query:String,val discoverySources:Int,val pagesRead:Int,val body:String,val evidenceLevel:EvidenceLevel,val comparison:String="")

/** Counts successfully read, distinct URLs. Discovery snippets are never read evidence. */
class ResearchResultVerifier {
    fun verify(query:String,discoveries:List<SourceSnippet>,readPages:List<ReadPageEvidence>):VerifiedResearchReport {
        val clean=query.trim()
        require(clean.isNotEmpty()) { "Research query cannot be empty" }
        val unique=readPages.distinctBy { canonical(it.url) }
        val level=when {
            unique.size>=2 -> EvidenceLevel.READ_MULTI_SOURCE
            unique.size==1 -> EvidenceLevel.READ_SINGLE_SOURCE
            discoveries.any { it.text.isNotBlank() } -> EvidenceLevel.DISCOVERY_ONLY
            else -> EvidenceLevel.NONE
        }
        val comparison=if(unique.size>=2) compare(unique[0],unique[1]) else ""
        val count=discoveries.count { it.text.isNotBlank() }
        val body=buildString {
            append("ผลการค้นคว้า: ").append(clean).append("\n")
            append("ระดับหลักฐาน: ").append(level.name).append("\n")
            append("ผลค้นพบที่มีข้อความ: ").append(count).append("\n")
            append("หน้าเว็บต้นทางที่อ่านจริง (URL ไม่ซ้ำ): ").append(unique.size).append("\n")
            when(level) {
                EvidenceLevel.READ_MULTI_SOURCE -> append("อ่านอย่างน้อยสองหน้าแล้ว แต่ยังต้องตรวจสอบความน่าเชื่อถือและวันที่\n")
                EvidenceLevel.READ_SINGLE_SOURCE -> append("อ่านได้หนึ่งหน้า ยังไม่พอสำหรับเปรียบเทียบหลายแหล่ง\n")
                EvidenceLevel.DISCOVERY_ONLY -> append("มีเพียงข้อมูลค้นพบ ยังไม่ได้อ่านหน้าเว็บต้นทาง\n")
                EvidenceLevel.NONE -> append("ไม่มีหลักฐานที่ใช้ได้\n")
            }
            if(comparison.isNotBlank()) append(comparison).append("\n")
        }
        return VerifiedResearchReport(clean,count,unique.size,body,level,comparison)
    }
    private fun compare(a:ReadPageEvidence,b:ReadPageEvidence):String {
        val x=tokens(a.text); val y=tokens(b.text)
        if(x.isEmpty()||y.isEmpty()) return "เปรียบเทียบเบื้องต้น: ข้อความสั้นเกินไป"
        val common=x.intersect(y)
        val ratio=common.size.toDouble()/maxOf(x.size,y.size).toDouble()
        return buildString {
            append("เปรียบเทียบคำสำคัญแบบง่าย: ").append(String.format(Locale.ROOT,"%.0f%%",ratio*100)).append(" คำร่วมโดยประมาณ\n")
            if(common.isNotEmpty()) append("ตัวอย่างคำร่วม: ").append(common.take(12).joinToString(", ")).append("\n")
            append("หมายเหตุ: การนับคำไม่ใช่การตรวจสอบความจริง")
        }
    }
    private fun tokens(s:String)=s.lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]")," ").split(Regex("\\s+")).map{it.trim()}.filter{it.length>=3}.toSet()
    private fun canonical(s:String):String=runCatching {
        val u=URI(s).normalize()
        val scheme=u.scheme?.lowercase() ?: return@runCatching s.substringBefore('#')
        val host=u.host?.lowercase()?.trimEnd('.') ?: return@runCatching s.substringBefore('#')
        val port=if(scheme=="https"&&u.port==443) -1 else u.port
        URI(scheme,null,host,port,u.path,u.query,null).toASCIIString()
    }.getOrDefault(s.substringBefore('#'))
}
