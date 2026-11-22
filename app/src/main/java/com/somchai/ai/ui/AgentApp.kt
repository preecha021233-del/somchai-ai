package com.somchai.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.somchai.ai.core.CoreRuntime
import com.somchai.ai.core.plugins.PluginCandidate
import com.somchai.ai.ui.theme.*
import kotlinx.coroutines.launch

private enum class Tab(val label: String) { CHAT("แชต"), PLUGINS("ปลั๊กอิน"), MEMORY("บันทึก"), SETTINGS("ตั้งค่า") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentApp(runtime: CoreRuntime) {
    var tab by remember { mutableStateOf(Tab.CHAT) }
    var refresh by remember { mutableIntStateOf(0) }
    var messages by remember { mutableStateOf(listOf("SOMCHAI: พร้อมทำงานครับ ระบบประมวลผลภายในพร้อมใช้งาน ไม่จำเป็นต้องตั้งค่า API Key")) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = SomchaiBackground,
        topBar = {
            TopAppBar(
                title = { Column {
                    Text("SOMCHAI AI", color = SomchaiTextPrimary)
                    Text("Protected Core • Owner controlled", style = MaterialTheme.typography.labelSmall, color = SomchaiTextSecondary)
                } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SomchaiSurface)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = SomchaiSurface) {
                Tab.entries.forEach { item ->
                    val icon = when (item) {
                        Tab.CHAT -> Icons.Outlined.Chat
                        Tab.PLUGINS -> Icons.Outlined.Extension
                        Tab.MEMORY -> Icons.Outlined.Bookmarks
                        Tab.SETTINGS -> Icons.Outlined.Settings
                    }
                    NavigationBarItem(
                        selected = tab == item, onClick = { tab = item },
                        icon = { Icon(icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        when (tab) {
            Tab.CHAT -> ChatPage(
                modifier = Modifier.padding(padding), messages = messages, input = input,
                onInput = { input = it }, busy = busy, error = error,
                onSend = {
                    val text = input.trim()
                    if (text.isNotEmpty() && !busy) {
                        input = ""; messages = messages + "คุณ: $text"; busy = true; error = null
                        scope.launch {
                            try {
                                val answer = if (text.startsWith("/search ", true)) {
                                    check(runtime.plugins.activePlugins().any { it.id == "web-search" && it.operation == "web.search" }) {
                                        "ต้องอนุมัติปลั๊กอิน Web Search ในหน้า Plugins ก่อน"
                                    }
                                    runtime.tools.search(text.removePrefix("/search").trim())
                                } else if (text.startsWith("/ai ", true)) {
                                    check(runtime.secrets.hasKey()) { "ยังไม่ได้ตั้งค่า LLM Provider ครับ ใช้คำสั่งภายในได้โดยไม่ต้องใช้ API" }
                                    val history = messages.takeLast(12).mapNotNull {
                                        when {
                                            it.startsWith("คุณ: ") -> "user" to it.removePrefix("คุณ: ")
                                            it.startsWith("SOMCHAI: ") -> "assistant" to it.removePrefix("SOMCHAI: ")
                                            else -> null
                                        }
                                    }
                                    runtime.llm.ask(history, text.removeRange(0, 4).trim())
                                } else {
                                    val result = runtime.processor.process(text)
                                    runtime.logger.record("INTERNAL_PROCESS", "intent=${result.intent}")
                                    result.answer
                                }
                                messages = messages + "SOMCHAI: $answer"
                            } catch (e: Exception) {
                                error = e.message ?: "เกิดข้อผิดพลาด"
                                messages = messages + "SOMCHAI: ยังดำเนินการไม่ได้ — $error"
                                runtime.logger.record("CHAT_ERROR", error ?: "unknown")
                            } finally { busy = false }
                        }
                    }
                }
            )
            Tab.PLUGINS -> PluginsPage(runtime, Modifier.padding(padding), refresh) { refresh++ }
            Tab.MEMORY -> MemoryPage(runtime, Modifier.padding(padding))
            Tab.SETTINGS -> SettingsPage(runtime, Modifier.padding(padding)) { error = it }
        }
    }
}

@Composable
private fun ChatPage(
    modifier: Modifier, messages: List<String>, input: String, onInput: (String) -> Unit,
    busy: Boolean, error: String?, onSend: () -> Unit
) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(messages) { message ->
                Card(colors = CardDefaults.cardColors(containerColor = SomchaiSurface)) {
                    Text(message, Modifier.fillMaxWidth().padding(14.dp), color = SomchaiTextPrimary)
                }
            }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = SomchaiPrimaryLight) }
        }
        error?.let { Text(it, color = SomchaiCritical, style = MaterialTheme.typography.bodySmall) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = input, onValueChange = onInput, modifier = Modifier.weight(1f),
                placeholder = { Text("พิมพ์คำสั่ง หรือ /search คำค้น") }, maxLines = 4, enabled = !busy)
            Button(onClick = onSend, enabled = input.isNotBlank() && !busy) { Icon(Icons.Outlined.Send, "ส่ง") }
        }
    }
}

@Composable
private fun PluginsPage(runtime: CoreRuntime, modifier: Modifier, refresh: Int, onRefresh: () -> Unit) {
    val candidates = remember(refresh) { runtime.plugins.candidates() }
    val active = remember(refresh) { runtime.plugins.activePlugins() }
    val disabled = remember(refresh) { runtime.plugins.disabledPlugins() }
    var message by remember { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("รออนุมัติ", style = MaterialTheme.typography.titleLarge, color = SomchaiTextPrimary)
        Text("ตรวจสอบปลั๊กอินก่อนเปิดใช้งานทุกครั้ง", color = SomchaiTextSecondary)
        Spacer(Modifier.height(12.dp))
        if (candidates.isEmpty()) Text("ไม่มีปลั๊กอินรออนุมัติ", color = SomchaiTextSecondary)
        candidates.forEach { candidate ->
            CandidateCard(candidate, onApprove = {
                runCatching { runtime.plugins.approve(candidate.manifest.id, candidate.hash) }
                    .onSuccess { message = "อนุมัติและเปิดใช้งาน ${candidate.manifest.name} แล้ว" }
                    .onFailure { message = it.message }
                onRefresh()
            }, onReject = {
                runtime.plugins.reject(candidate.manifest.id); message = "ปฏิเสธปลั๊กอินแล้ว"; onRefresh()
            })
        }
        message?.let { Text(it, color = SomchaiPrimaryLight) }
        Spacer(Modifier.height(24.dp))
        Text("ปลั๊กอินที่ใช้งาน", style = MaterialTheme.typography.titleLarge, color = SomchaiTextPrimary)
        active.forEach { plugin ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("• ${plugin.name} v${plugin.version} — ${plugin.operation}",
                    color = SomchaiTextSecondary, modifier = Modifier.weight(1f).padding(8.dp))
                TextButton(onClick = {
                    runCatching { runtime.plugins.disable(plugin.id) }
                        .onSuccess { message = if (it) "ปิดใช้งาน ${plugin.name} แล้ว" else "ไม่พบปลั๊กอินที่เปิดใช้งาน" }
                        .onFailure { message = it.message }
                    onRefresh()
                }) { Text("ปิดใช้งาน") }
            }
        }
        if (disabled.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("ปลั๊กอินที่ปิดใช้งาน", style = MaterialTheme.typography.titleLarge, color = SomchaiTextPrimary)
            disabled.forEach { plugin ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• ${plugin.name} v${plugin.version} — ปิดใช้งาน",
                        color = SomchaiTextSecondary, modifier = Modifier.weight(1f).padding(8.dp))
                    TextButton(onClick = {
                        runCatching { runtime.plugins.enable(plugin.id) }
                            .onSuccess { message = if (it) "เปิดใช้งาน ${plugin.name} แล้ว" else "ไม่พบปลั๊กอินที่ปิดใช้งาน" }
                            .onFailure { message = it.message }
                        onRefresh()
                    }) { Text("เปิดใช้งาน") }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("การทำงานถูกจำกัดไว้ใน Core allowlist ไม่มีการรันโค้ดที่ดาวน์โหลดมา", color = SomchaiTextSecondary)
    }
}

@Composable
private fun CandidateCard(candidate: PluginCandidate, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = SomchaiSurface)) {
        Column(Modifier.padding(16.dp)) {
            Text(candidate.manifest.name, color = SomchaiTextPrimary, style = MaterialTheme.typography.titleMedium)
            Text(candidate.manifest.description, color = SomchaiTextSecondary)
            Text("Operation: ${candidate.manifest.operation}", color = SomchaiTextSecondary)
            Text("Permissions: ${candidate.manifest.permissions.joinToString()}", color = SomchaiWarning)
            Text("SHA-256: ${candidate.hash.take(24)}…", color = SomchaiTextTertiary, style = MaterialTheme.typography.labelSmall)
            Text("Smoke tests: ${candidate.smokeChecks.joinToString()}", color = SomchaiSuccess, style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApprove) { Text("อนุมัติ") }
                OutlinedButton(onClick = onReject) { Text("ปฏิเสธ") }
            }
        }
    }
}

@Composable
private fun MemoryPage(runtime: CoreRuntime, modifier: Modifier) {
    var note by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var memoryMessage by remember { mutableStateOf<String?>(null) }
    val memoryResult = remember(refresh, query) {
        runCatching {
            if (query.isBlank()) runtime.memory.all(200) else runtime.memory.search(query, 100)
        }
    }
    val entries = memoryResult.getOrDefault(emptyList())
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("ความจำภายใน", style = MaterialTheme.typography.titleLarge, color = SomchaiTextPrimary)
        Text("จัดเก็บในพื้นที่ส่วนตัวและเข้ารหัสด้วย Android Keystore ไม่ส่งขึ้นอินเทอร์เน็ตโดยอัตโนมัติ", color = SomchaiTextSecondary)
        if (memoryResult.isFailure) Text("อ่านความจำไม่ได้ ข้อมูลไม่ได้ถูกลบ กรุณาอย่าล้างข้อมูลแอป", color = SomchaiCritical)
        OutlinedTextField(
            value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("บันทึกความจำใหม่") }, maxLines = 3
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                runCatching { runtime.memory.backup() }
                    .onSuccess { memoryMessage = if (it) "สำรองความจำแล้ว" else "ยังไม่มีข้อมูลให้สำรอง" }
                    .onFailure { memoryMessage = it.message }
            }) { Text("สำรองความจำ") }
            OutlinedButton(onClick = { showRestoreConfirm = true }) { Text("กู้คืนข้อมูล") }
        }
        memoryMessage?.let { Text(it, color = SomchaiPrimaryLight) }
        if (showRestoreConfirm) {
            AlertDialog(
                onDismissRequest = { showRestoreConfirm = false },
                title = { Text("ยืนยันการกู้คืนความจำ") },
                text = { Text("การกู้คืนจะแทนที่รายการความจำปัจจุบันด้วยข้อมูลจากไฟล์สำรอง ต้องการดำเนินการหรือไม่?") },
                confirmButton = {
                    TextButton(onClick = {
                        runCatching { runtime.memory.restoreBackup() }
                            .onSuccess { memoryMessage = "กู้คืนความจำ $it รายการแล้ว"; refresh++ }
                            .onFailure { memoryMessage = it.message }
                        showRestoreConfirm = false
                    }) { Text("ยืนยันกู้คืน") }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreConfirm = false }) { Text("ยกเลิก") }
                }
            )
        }
        Button(onClick = {
            runCatching { runtime.memory.add(note) }
                .onSuccess { note = ""; refresh++ }
                .onFailure { runtime.logger.record("MEMORY_ERROR", it.message ?: "write failed") }
        }, enabled = note.isNotBlank()) { Text("บันทึก") }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("ค้นหาความจำ") }, singleLine = true
        )
        Text("รายการความจำ ${entries.size}", color = SomchaiTextSecondary, modifier = Modifier.padding(vertical = 8.dp))
        if (entries.isEmpty()) Text("ยังไม่มีความจำที่ตรงกับคำค้น", color = SomchaiTextSecondary)
        entries.forEach { entry ->
            Card(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = SomchaiSurface)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(entry.content, color = SomchaiTextPrimary)
                    Text("${entry.category} • ${entry.createdAt.take(10)} • ID ${entry.id.take(8)}",
                        color = SomchaiTextTertiary, style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = {
                        runtime.memory.delete(entry.id)
                        refresh++
                    }) { Text("ลบรายการนี้", color = SomchaiCritical) }
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(runtime: CoreRuntime, modifier: Modifier, onMessage: (String) -> Unit) {
    val provider = runtime.secrets.provider()
    var baseUrl by remember { mutableStateOf(provider?.baseUrl ?: "https://api.openai.com/v1") }
    var model by remember { mutableStateOf(provider?.model ?: "gpt-4o-mini") }
    var key by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("การเชื่อมต่อ LLM", style = MaterialTheme.typography.titleLarge, color = SomchaiTextPrimary)
        Text(if (runtime.secrets.hasKey()) "มี API Key บันทึกแบบเข้ารหัสใน Android Keystore" else "ยังไม่ได้ตั้งค่า API Key", color = SomchaiTextSecondary)
        OutlinedTextField(baseUrl, { baseUrl = it }, Modifier.fillMaxWidth(), label = { Text("API Base URL (HTTPS)") }, singleLine = true)
        OutlinedTextField(model, { model = it }, Modifier.fillMaxWidth(), label = { Text("Model") }, singleLine = true)
        OutlinedTextField(key, { key = it }, Modifier.fillMaxWidth(), label = { Text("API Key") }, singleLine = true,
            visualTransformation = if (showKey) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation())
        Row { Checkbox(showKey, { showKey = it }); Text("แสดง API Key", color = SomchaiTextSecondary) }
        Button(onClick = {
            runCatching { runtime.saveProviderSettings(baseUrl, model, key) }
                .onSuccess { key = ""; onMessage("บันทึกการตั้งค่าแล้ว") }
                .onFailure { onMessage(it.message ?: "บันทึกไม่สำเร็จ") }
        }, enabled = key.isNotBlank() && baseUrl.startsWith("https://") && model.isNotBlank()) { Text("บันทึกการเชื่อมต่อ") }
        Spacer(Modifier.height(24.dp))
        Text("ข้อจำกัดและความปลอดภัย", style = MaterialTheme.typography.titleLarge, color = SomchaiTextPrimary)
        Text("• API request จำกัด 40 ครั้งต่อวัน\n• ปลั๊กอินต้องผ่านการอนุมัติ\n• ข้อมูลเว็บเป็นข้อมูลที่ไม่น่าเชื่อถือ\n• ข้อมูลอยู่ในพื้นที่ส่วนตัวของแอป", color = SomchaiTextSecondary)
        Spacer(Modifier.height(24.dp))
        Text("บันทึกกิจกรรมล่าสุด", style = MaterialTheme.typography.titleMedium, color = SomchaiTextPrimary)
        runtime.logger.recent(20).forEach { line ->
            Text(line, color = SomchaiTextTertiary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 3.dp))
        }
    }
}
