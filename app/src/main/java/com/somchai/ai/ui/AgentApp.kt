package com.somchai.ai.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.somchai.ai.core.CoreRuntime
import com.somchai.ai.core.brain.BrainRequest
import com.somchai.ai.core.processing.TaskPlan\nimport com.somchai.ai.core.skills.SkillTestCase
import com.somchai.ai.core.plugins.PluginCandidate
import com.somchai.ai.ui.theme.*
import kotlinx.coroutines.launch

private enum class Tab(val label: String) { CHAT("แชต"), PLUGINS("ปลั๊กอิน"), MEMORY("บันทึก"), SETTINGS("ตั้งค่า") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentApp(runtime: CoreRuntime) {
    var tab by remember { mutableStateOf(Tab.CHAT) }
    var menuExpanded by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var messages by remember { mutableStateOf(listOf(
        when {
            runtime.localInference.isModelLoaded() -> "SOMCHAI: สมอง AI ภายในพร้อมทำงานครับ"
            runtime.modelStore.modelFile().isFile -> "SOMCHAI: พบโมเดลในเครื่อง กำลังเตรียมระบบสมองภายในครับ"
            else -> "SOMCHAI: ระบบพื้นฐานพร้อมทำงานครับ แต่ยังไม่ได้ติดตั้งโมเดลสมอง AI ภายใน ไปที่ ตั้งค่า เพื่อดาวน์โหลดโมเดลก่อนครับ"
        }
    )) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingPlan by remember { mutableStateOf<TaskPlan?>(null) }
    val scope = rememberCoroutineScope()
    val appContext = LocalContext.current

    Scaffold(
        containerColor = SomchaiBackground,
        topBar = {
            TopAppBar(
                title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF7438E8), Color(0xFF356DEB)))), contentAlignment = Alignment.Center) {
                        Text("S", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    }
                    Column {
                        Text("SOMCHAI AI", color = SomchaiTextPrimary, style = MaterialTheme.typography.titleLarge)
                        Text("● กำลังใช้งานอยู่", style = MaterialTheme.typography.labelMedium, color = Color(0xFF20A464))
                    }
                } },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Outlined.Menu, contentDescription = "เปิดเมนู", tint = SomchaiTextPrimary)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            Tab.entries.forEach { item ->
                                val icon = when (item) {
                                    Tab.CHAT -> Icons.Outlined.Chat
                                    Tab.PLUGINS -> Icons.Outlined.Extension
                                    Tab.MEMORY -> Icons.Outlined.Bookmarks
                                    Tab.SETTINGS -> Icons.Outlined.Settings
                                }
                                DropdownMenuItem(
                                    text = { Text(item.label, color = SomchaiTextPrimary) },
                                    leadingIcon = { Icon(icon, contentDescription = null, tint = Color(0xFF6557E8)) },
                                    onClick = {
                                        tab = item
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        when (tab) {
            Tab.CHAT -> ChatPage(
                modifier = Modifier.padding(padding), messages = messages, input = input,
                onInput = { input = it }, busy = busy, error = error,
                pendingPlan = pendingPlan,
                onStartPlan = {
                    val plan = pendingPlan
                    if (plan != null) {
                        val task = runtime.tasks.create(plan)
                        val started = runtime.tasks.startStep(task.id, 0)
                        runtime.logger.record("TASK_CREATED", "id=${task.id} type=${task.type} steps=${task.steps.size}")
                        runtime.logger.record("TASK_STEP_STARTED", "id=${task.id} step=0")
                        messages = messages + "SOMCHAI: สร้างงานและบันทึกลงเครื่องแล้ว รหัส ${task.id.take(8)}\nขั้นตอนที่ 1: ${started.steps.first().title}\nสถานะ: กำลังติดตามงาน (ยังไม่ได้ดำเนินการภายนอก)"
                        pendingPlan = null
                    }
                },
                onFeedback = { answer, positive ->
                    runCatching { runtime.memory.add("feedback=${if (positive) "positive" else "negative"}; answer=$answer", "response-feedback") }
                        .onSuccess { Toast.makeText(appContext, if (positive) "ขอบคุณสำหรับคำติชม" else "รับทราบคำติชมแล้ว", Toast.LENGTH_SHORT).show() }
                        .onFailure { runtime.logger.record("FEEDBACK_ERROR", it.message ?: "unknown") }
                },
                onSend = {
                    val text = input.trim()
                    if (text.isNotEmpty() && !busy) {
                        input = ""; messages = messages + "คุณ: $text"; busy = true; error = null
                        pendingPlan = null
                        scope.launch {
                            try {
                                val answer = if (text.startsWith("/skill", true)) {
                                    val command = text.substringAfter(" ", "").trim()
                                    when {
                                        command.equals("list", true) -> {
                                            val skills = runtime.skills.all()
                                            if (skills.isEmpty()) "ยังไม่มีทักษะที่บันทึกไว้" else skills.joinToString("\\n") { skill ->
                                                val status = if (skill.approved) "อนุมัติแล้ว" else "รอทดสอบ/อนุมัติ"
                                                "• " + skill.name + " [" + skill.id.take(8) + "] — " + status +
                                                    " — ผ่าน " + skill.results.count { it.passed } + "/" + skill.tests.size
                                            }
                                        }
                                        command.startsWith("propose ", true) -> {
                                            val fields = command.substringAfter(" ").trim().split("|", limit = 4).map { it.trim() }
                                            require(fields.size == 4) {
                                                "รูปแบบ: /skill propose ชื่อ | คำกระตุ้น,คำกระตุ้น | ขั้นตอน | input1 => expected1 ;; input2 => expected2"
                                            }
                                            val tests = fields[3].split(";;").map { raw ->
                                                val pair = raw.split("=>", limit = 2).map { it.trim() }
                                                require(pair.size == 2) { "กรณีทดสอบต้องเขียน input => expected" }
                                                SkillTestCase(pair[0], pair[1])
                                            }
                                            val skill = runtime.skills.propose(
                                                fields[0], fields[1].split(",").map { it.trim() }, fields[2], tests
                                            )
                                            "บันทึกทักษะเป็นฉบับร่างแล้ว รหัส " + skill.id.take(8) +
                                                "\\nต้องทดสอบด้วย /skill test " + skill.id +
                                                " และเจ้าของต้องอนุมัติด้วย /skill approve " + skill.id
                                        }
                                        command.startsWith("test ", true) -> {
                                            val id = command.substringAfter(" ").trim()
                                            val results = runtime.skills.evaluate(id, runtime.localInference)
                                            "ผลทดสอบทักษะ: ผ่าน " + results.count { it.passed } + "/" + results.size +
                                                "\\n" + results.mapIndexed { index, result ->
                                                    (index + 1).toString() + ". " + (if (result.passed) "ผ่าน" else "ไม่ผ่าน") +
                                                        " — " + result.output.take(240)
                                                }.joinToString("\\n")
                                        }
                                        command.startsWith("approve ", true) -> {
                                            val id = command.substringAfter(" ").trim()
                                            val skill = runtime.skills.approve(id)
                                            "เจ้าของอนุมัติทักษะ " + skill.name + " แล้ว ระบบจะใช้เมื่อพบคำกระตุ้นที่ตรงกัน"
                                        }
                                        command.startsWith("delete ", true) -> {
                                            val id = command.substringAfter(" ").trim()
                                            if (runtime.skills.delete(id)) "ลบทักษะแล้ว" else "ไม่พบทักษะ"
                                        }
                                        else -> "คำสั่งทักษะ:\\n/skill list\\n/skill propose ชื่อ | คำกระตุ้น | ขั้นตอน | input => expected ;; input => expected\\n/skill test รหัส\\n/skill approve รหัส\\n/skill delete รหัส"
                                    }
                                } else if (text.equals("/plugins", true) || text.equals("ตรวจสอบปลั๊กอิน", true) || text.equals("รายการปลั๊กอิน", true)) {
                                    val active = runtime.plugins.activePlugins()
                                    val candidates = runtime.plugins.candidates()
                                    val disabled = runtime.plugins.disabledPlugins()
                                    buildString {
                                        append("สถานะปลั๊กอิน SOMCHAI\n")
                                        append("ใช้งานอยู่: ").append(active.size).append("\n")
                                        active.forEach { append("• ").append(it.name).append(" (").append(it.id).append(") — เปิดใช้งาน\n") }
                                        append("รออนุมัติ: ").append(candidates.size).append("\n")
                                        candidates.forEach { append("• ").append(it.manifest.name).append(" (").append(it.manifest.id).append(") — รอเจ้าของอนุมัติ\n") }
                                        append("ปิดใช้งาน: ").append(disabled.size).append("\n")
                                        disabled.forEach { append("• ").append(it.name).append(" (").append(it.id).append(") — ปิดใช้งาน\n") }
                                        if (active.isEmpty() && candidates.isEmpty() && disabled.isEmpty()) append("ยังไม่มีปลั๊กอินในระบบ")
                                    }
                                } else if (text.startsWith("/search ", true)) {
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
                                } else if (
                                    text.startsWith("/") ||
                                    text.startsWith("จำไว้ว่า ") ||
                                    text.startsWith("จำว่า ") ||
                                    listOf("วางแผน", "แผนงาน", "แบ่งงาน", "ขั้นตอน").any { text.contains(it) }
                                ) {
                                    val result = runtime.processor.process(text)
                                    runtime.logger.record("INTERNAL_PROCESS", "intent=${result.intent}")
                                    if (result.intent == "task.plan") {
                                        val goal = if (text.startsWith("/plan ", true)) text.substringAfter(' ').trim() else text
                                        pendingPlan = runtime.planner.plan(goal)
                                    }
                                    result.answer
                                } else {
                                    val history = messages.takeLast(12).mapNotNull {
                                        when {
                                            it.startsWith("คุณ: ") -> "user" to it.removePrefix("คุณ: ")
                                            it.startsWith("SOMCHAI: ") -> "assistant" to it.removePrefix("SOMCHAI: ")
                                            else -> null
                                        }
                                    }
                                    val reply = runtime.brain.respond(
                                        BrainRequest(text, history, runtime.skills.relevantFor(text))
                                    )
                                    runtime.logger.record("BRAIN_ROUTE", "engine=${reply.engineId} fallback=${reply.usedFallback}")
                                    reply.text
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
    busy: Boolean, error: String?, pendingPlan: TaskPlan?, onStartPlan: () -> Unit,
    onFeedback: (String, Boolean) -> Unit, onSend: () -> Unit
) {
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var speechReady by remember { mutableStateOf(false) }
    val speech = remember(context) { TextToSpeech(context) { status -> speechReady = status == TextToSpeech.SUCCESS } }
    val feedbackState = remember { mutableStateMapOf<String, Boolean>() }
    DisposableEffect(speech) { onDispose { speech.stop(); speech.shutdown() } }

    LaunchedEffect(messages.size, busy) {
        val lastIndex = messages.size + if (busy) 1 else 0
        if (lastIndex > 0) listState.animateScrollToItem(lastIndex - 1)
    }

    Column(
        modifier.fillMaxSize().background(Color.White)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages) { message ->
                val isUser = message.startsWith("คุณ:")
                val body = when {
                    isUser -> message.removePrefix("คุณ:").trim()
                    message.startsWith("SOMCHAI:") -> message.removePrefix("SOMCHAI:").trim()
                    else -> message
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (!isUser) {
                        Box(
                            Modifier.padding(end = 8.dp).size(34.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF7438E8), Color(0xFF356DEB)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("S", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    if (isUser) {
                        Box(
                            Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(22.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF7438E8), Color(0xFF4267E8))))
                                .padding(horizontal = 17.dp, vertical = 12.dp)
                        ) {
                            Text(body, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        Column(Modifier.widthIn(max = 316.dp).fillMaxWidth()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFFEEF0F4),
                                shape = RoundedCornerShape(22.dp),
                                shadowElevation = 5.dp
                            ) {
                                Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
                                    Text(
                                        body,
                                        color = Color(0xFF17171B),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(Modifier.height(7.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            Modifier.height(33.dp).clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("SOMCHAI AI", body))
                                                    Toast.makeText(context, "คัดลอกแล้ว", Toast.LENGTH_SHORT).show()
                                                }.padding(horizontal = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = SomchaiTextSecondary, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text("คัดลอก", color = SomchaiTextSecondary, style = MaterialTheme.typography.labelSmall)
                                        }
                                        Row(
                                            Modifier.height(33.dp).clip(RoundedCornerShape(8.dp))
                                                .clickable { feedbackState[body] = true; onFeedback(body, true) }
                                                .padding(horizontal = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Outlined.ThumbUp, contentDescription = null, tint = if (feedbackState[body] == true) Color(0xFF6557E8) else SomchaiTextSecondary, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text("ถูกใจ", color = SomchaiTextSecondary, style = MaterialTheme.typography.labelSmall)
                                        }
                                        Row(
                                            Modifier.height(33.dp).clip(RoundedCornerShape(8.dp))
                                                .clickable { feedbackState[body] = false; onFeedback(body, false) }
                                                .padding(horizontal = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Outlined.ThumbDown, contentDescription = null, tint = if (feedbackState[body] == false) Color(0xFF6557E8) else SomchaiTextSecondary, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text("ไม่ถูกใจ", color = SomchaiTextSecondary, style = MaterialTheme.typography.labelSmall)
                                        }
                                        Row(
                                            Modifier.height(33.dp).clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    if (speechReady) {
                                                        val languageResult = speech.setLanguage(Locale("th", "TH"))
                                                        if (languageResult >= TextToSpeech.LANG_AVAILABLE) {
                                                            speech.speak(body, TextToSpeech.QUEUE_FLUSH, null, "somchai-${body.hashCode()}")
                                                        } else {
                                                            Toast.makeText(context, "อุปกรณ์ยังไม่มีเสียงภาษาไทย", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        Toast.makeText(context, "กำลังเตรียมระบบอ่านออกเสียง", Toast.LENGTH_SHORT).show()
                                                    }
                                                }.padding(horizontal = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Outlined.VolumeUp, contentDescription = null, tint = SomchaiTextSecondary, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text("อ่านเสียง", color = SomchaiTextSecondary, style = MaterialTheme.typography.labelSmall)
                                        }
                                        Row(
                                            Modifier.height(33.dp).clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, body)
                                                    }
                                                    context.startActivity(Intent.createChooser(intent, "แชร์คำตอบจาก SOMCHAI AI"))
                                                }.padding(horizontal = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Outlined.Share, contentDescription = null, tint = SomchaiTextSecondary, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text("แชร์", color = SomchaiTextSecondary, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }
            if (pendingPlan != null) {
                item {
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("พร้อมเริ่มติดตามงาน: ${pendingPlan.goal}", style = MaterialTheme.typography.titleSmall)
                            Text("${pendingPlan.steps.size} ขั้นตอน · ${pendingPlan.type}", style = MaterialTheme.typography.bodySmall)
                            Button(onClick = onStartPlan, modifier = Modifier.fillMaxWidth()) {
                                Text("เริ่มงานขั้นตอนที่ 1")
                            }
                            Text("ปุ่มนี้สร้างงานและบันทึกสถานะในเครื่อง ยังไม่สั่งงานภายนอกอัตโนมัติ", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (busy) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(28.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF7438E8), Color(0xFF356DEB)))),
                            contentAlignment = Alignment.Center
                        ) { Text("S", color = Color.White, style = MaterialTheme.typography.labelLarge) }
                        Text("  กำลังพิมพ์...", color = SomchaiTextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        error?.let {
            Text(it, color = SomchaiCritical, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp))
        }

        Surface(
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                    .imePadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                TextField(
                    value = input,
                    onValueChange = onInput,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("พิมพ์ข้อความ...") },
                    enabled = !busy,
                    maxLines = 4,
                    shape = RoundedCornerShape(28.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (input.isNotBlank() && !busy) {
                            keyboard?.hide()
                            focusManager.clearFocus()
                            onSend()
                        }
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF17171B),
                        unfocusedTextColor = Color(0xFF17171B),
                        disabledTextColor = Color(0xFF77777D),
                        cursorColor = Color(0xFF7438E8),
                        focusedPlaceholderColor = Color(0xFF85858C),
                        unfocusedPlaceholderColor = Color(0xFF85858C),
                        focusedContainerColor = Color(0xFFF0F0F2),
                        unfocusedContainerColor = Color(0xFFF0F0F2),
                        disabledContainerColor = Color(0xFFF0F0F2),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                Box(
                    Modifier.padding(bottom = 0.dp).size(58.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF7438E8), Color(0xFF356DEB)))),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            keyboard?.hide()
                            focusManager.clearFocus()
                            onSend()
                        },
                        enabled = input.isNotBlank() && !busy
                    ) {
                        Icon(Icons.Outlined.Send, contentDescription = "ส่งข้อความ", tint = Color.White)
                    }
                }
            }
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
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) runCatching {
            val payload = runtime.memory.exportEncrypted()
            val stream = requireNotNull(context.contentResolver.openOutputStream(uri)) { "เปิดไฟล์ปลายทางไม่ได้" }
            stream.use { it.write(payload) }
        }.onSuccess {
            memoryMessage = "ส่งออกไฟล์ความจำที่เข้ารหัสแล้ว (ใช้ได้กับเครื่องและกุญแจเดิม)"
        }.onFailure { memoryMessage = it.message ?: "ส่งออกไม่สำเร็จ" }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) runCatching {
            val stream = requireNotNull(context.contentResolver.openInputStream(uri)) { "เปิดไฟล์นำเข้าไม่ได้" }
            val bytes = stream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= 1_048_576) { "ไฟล์นำเข้ามีขนาดเกิน 1 MB" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            runtime.memory.importEncrypted(bytes)
        }.onSuccess {
            memoryMessage = "นำเข้าความจำ $it รายการแล้ว"
            refresh++
        }.onFailure { memoryMessage = it.message ?: "นำเข้าไม่สำเร็จ" }
    }
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
            }) { Text("สำรองในแอป") }
            OutlinedButton(onClick = { showRestoreConfirm = true }) { Text("กู้คืนในแอป") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { exportLauncher.launch("somchai-memory.enc") }) {
                Text("ส่งออกไฟล์")
            }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) }) {
                Text("นำเข้าไฟล์")
            }
        }
        Text(
            "ไฟล์ส่งออกเข้ารหัสและผูกกับกุญแจ Android Keystore ของการติดตั้งนี้ จึงกู้คืนได้บนแอปติดตั้งเดิมที่ยังมีกุญแจอยู่เท่านั้น ไม่สามารถย้ายไปเครื่องใหม่ได้",
            color = SomchaiTextTertiary,
            style = MaterialTheme.typography.bodySmall
        )
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
    val scope = rememberCoroutineScope()
    var modelReady by remember { mutableStateOf(runtime.localInference.isModelLoaded()) }
    var modelDownloading by remember { mutableStateOf(false) }
    var modelProgress by remember { mutableIntStateOf(0) }
    var modelMessage by remember { mutableStateOf<String?>(null) }
    var baseUrl by remember { mutableStateOf(provider?.baseUrl ?: "https://api.openai.com/v1") }
    var model by remember { mutableStateOf(provider?.model ?: "gpt-4o-mini") }
    var key by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("สมอง AI ภายใน", style = MaterialTheme.typography.titleLarge, color = SomchaiTextPrimary)
        Text(
            if (modelReady) "โมเดลภาษาในเครื่องพร้อมใช้งาน — คำถามทั่วไปจะประมวลผลบนโทรศัพท์ ไม่ส่งไป AI ภายนอก"
            else "ยังไม่มีโมเดลภาษาในเครื่อง ขณะนี้ระบบใช้ตัวประมวลผลสำรองแบบกฎ",
            color = if (modelReady) Color(0xFF16834A) else SomchaiTextSecondary
        )
        Text(
            "โมเดล Qwen2.5 0.5B Instruct (GGUF Q4_K_M) ขนาดประมาณ 491 MB • รองรับภาษาไทย • ใบอนุญาต Apache 2.0",
            color = SomchaiTextTertiary,
            style = MaterialTheme.typography.bodySmall
        )
        Button(
            onClick = {
                scope.launch {
                    modelDownloading = true
                    modelProgress = 0
                    modelMessage = "กำลังดาวน์โหลดโมเดลจาก Hugging Face..."
                    try {
                        if (runtime.modelStore.hasVerifiedModel()) {
                            runtime.loadInstalledLocalModel()
                        } else {
                            runtime.downloadAndLoadLocalModel { modelProgress = it }
                        }
                        modelReady = true
                        modelMessage = "โหลดโมเดลภายในสำเร็จ"
                    } catch (failure: Exception) {
                        modelMessage = failure.message ?: "ติดตั้งโมเดลไม่สำเร็จ"
                    } finally {
                        modelDownloading = false
                    }
                }
            },
            enabled = !modelDownloading && !modelReady
        ) {
            Text(
                if (modelDownloading) "กำลังติดตั้งโมเดล..."
                else if (modelReady) "โมเดลพร้อมใช้งาน"
                else if (runtime.modelStore.modelFile().isFile) "ตรวจสอบ/โหลดโมเดลที่ติดตั้งไว้"
                else "ดาวน์โหลดและติดตั้งสมอง AI"
            )
        }
        if (modelDownloading) {
            LinearProgressIndicator(progress = modelProgress / 100f, modifier = Modifier.fillMaxWidth())
            Text("$modelProgress%", color = SomchaiTextSecondary)
        }
        modelMessage?.let { Text(it, color = SomchaiPrimaryLight) }
        Text(
            "ต้องใช้อินเทอร์เน็ตและพื้นที่ว่างอย่างน้อย 650 MB แนะนำให้ใช้ Wi‑Fi การดาวน์โหลดตรวจ SHA-256 ก่อนติดตั้ง",
            color = SomchaiTextTertiary,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(24.dp))
        Text("การเชื่อมต่อ LLM ภายนอก (ไม่บังคับ)", style = MaterialTheme.typography.titleLarge, color = SomchaiTextPrimary)
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
