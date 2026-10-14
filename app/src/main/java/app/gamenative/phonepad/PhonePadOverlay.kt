package app.gamenative.phonepad

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val R8 = RoundedCornerShape(8.dp)

// Oyun üstü şeffaf katman: boş alan dokunuşları alta (fare) düşer, tasarım ayrı paneldir.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhonePadRoot(input: PhonePadInput, onToggleMouse: (Boolean) -> Unit = {}) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var layout by remember { mutableStateOf(defaultLayoutF()) }
    var sticks by remember { mutableStateOf(Sticks()) }
    var alpha by remember { mutableStateOf(0.92f) }
    var sizeMul by remember { mutableStateOf(1f) }
    var selected by remember { mutableStateOf("A") }
    var selectMode by remember { mutableStateOf(false) }
    var rpad by remember { mutableStateOf("stick") }
    var lpad by remember { mutableStateOf("stick") }
    var dpadMerged by remember { mutableStateOf(false) }
    var showDesign by remember { mutableStateOf(false) }
    var designHidden by remember { mutableStateOf(false) }
    var panelLeft by remember { mutableStateOf(false) }
    var rightAsMouse by remember { mutableStateOf(false) }

    suspend fun loadAll() {
        val p = ctx.phonePadStore.data.first()
        input.volUpBit = xboxNameToBit(prettyXboxName(p[K_VOL_UP] ?: DEF_VOL_UP))
        input.volDownBit = xboxNameToBit(prettyXboxName(p[K_VOL_DOWN] ?: DEF_VOL_DOWN))
        alpha = p[K_ALPHA] ?: 0.92f
        dpadMerged = p[K_DPAD_MERGED] ?: false
        p[K_LAYOUT]?.let { js ->
            try {
                val parsed = parseLayoutJson(js)
                layout = parsed.btns; sticks = parsed.sticks; rpad = parsed.rpad; lpad = parsed.lpad
                rightAsMouse = parsed.rmouse
            } catch (_: Exception) {}
        }
    }

    fun persist() {
        scope.launch {
            ctx.phonePadStore.edit {
                it[K_LAYOUT] = buildLayoutJson(layout, sticks, rpad, lpad, sizeMul, rightAsMouse).toString()
                it[K_ALPHA] = alpha
                it[K_DPAD_MERGED] = dpadMerged
            }
            sizeMul = 1f
        }
    }

    fun exportLayout() {
        scope.launch {
            try {
                val txt = buildLayoutJson(layout, sticks, rpad, lpad, sizeMul, rightAsMouse)
                    .put("app", "PhoneGamepad").toString(2)
                val name = "phone-gamepad-duzen.json"
                if (Build.VERSION.SDK_INT >= 29) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, name)
                        put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    }
                    val uri = ctx.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, values,
                    ) ?: throw IllegalStateException("dosya açılamadı")
                    ctx.contentResolver.openOutputStream(uri)!!.use { it.write(txt.toByteArray()) }
                } else {
                    java.io.File(ctx.getExternalFilesDir(null), name).writeText(txt)
                }
            } catch (_: Exception) {}
        }
    }

    val importPick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val txt = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    ?: throw IllegalStateException("dosya okunamadı")
                val parsed = parseLayoutJson(txt)
                layout = parsed.btns; sticks = parsed.sticks; rpad = parsed.rpad; lpad = parsed.lpad
                persist()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) { loadAll() }
    DisposableEffect(Unit) { onDispose { input.releaseAll() } }

    Box(Modifier.fillMaxSize()) {
        PhonePadScreen(
            input = input, layout = layout, sticks = sticks, sizeMul = sizeMul, alpha = alpha,
            rpad = rpad, lpad = lpad, dpadMerged = dpadMerged, selectMode = selectMode, selected = selected,
            rightAsMouse = rightAsMouse, onToggleMouseLook = { rightAsMouse = !rightAsMouse; persist() },
            onToggleMouse = onToggleMouse,
            onSelect = { selected = it },
            onMove = { label, dx, dy ->
                layout = layout.map { b ->
                    if (b.label == label) b.copy(
                        x = (b.x + dx).coerceIn(0.03f, 0.97f),
                        y = (b.y + dy).coerceIn(0.03f, 0.97f),
                    )
                    else b
                }.toMutableList()
            },
            onResize = { label, dx, dy ->
                layout = layout.map { b ->
                    if (b.label == label) b.copy(
                        size = (b.size + dy).coerceIn(0.05f, 0.24f),
                        ratio = (b.ratio + dx * 6f).coerceIn(0.6f, 4f),
                    )
                    else b
                }.toMutableList()
            },
            onStickMove = { which, dx, dy ->
                sticks = (if (which == "L") sticks.copy(
                    lx = (sticks.lx + dx).coerceIn(0.05f, 0.45f),
                    ly = (sticks.ly + dy).coerceIn(0.4f, 0.95f),
                )
                else sticks.copy(
                    rx = (sticks.rx + dx).coerceIn(0.55f, 0.95f),
                    ry = (sticks.ry + dy).coerceIn(0.4f, 0.95f),
                ))
            },
            onStickResize = { which, d ->
                sticks = (if (which == "L") sticks.copy(ld = (sticks.ld + d).coerceIn(0.18f, 0.42f))
                else sticks.copy(rd = (sticks.rd + d).coerceIn(0.18f, 0.42f)))
            },
            onOpenDesign = { selected = "A"; selectMode = true; designHidden = false; showDesign = true },
        )
        if (showDesign) {
            if (designHidden) {
                Box(Modifier.align(Alignment.CenterEnd).padding(8.dp)) {
                    Button(onClick = { designHidden = false }, shape = R8) { Text("Tasarım") }
                }
            } else {
                Card(
                    modifier = Modifier.align(if (panelLeft) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(8.dp).widthIn(max = 420.dp).fillMaxHeight(0.94f),
                    shape = R8,
                    colors = CardDefaults.cardColors(containerColor = Color(0xF21D1F23)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    PhonePadDesign(
                        input = input, layout = layout, sticks = sticks, alpha = alpha, sizeMul = sizeMul,
                        rpad = rpad, lpad = lpad, dpadMerged = dpadMerged,
                        onDpadMerged = { dpadMerged = it },
                        selected = selected, selectMode = selectMode,
                        panelLeft = panelLeft, onToggleSide = { panelLeft = !panelLeft },
                        onSelect = { selected = it },
                        onLayout = { layout = it },
                        onSticks = { sticks = it },
                        onRpad = { rpad = it },
                        onLpad = { lpad = it },
                        onAlpha = { alpha = it },
                        onSizeMul = { sizeMul = it },
                        onSelectMode = { selectMode = it },
                        onSave = { persist() },
                        onHide = { persist(); designHidden = true },
                        onClose = { showDesign = false; selectMode = false; persist() },
                        onExport = { exportLayout() },
                        onImport = { importPick.launch("application/json") },
                    )
                }
            }
        }
    }
}

@Composable
fun PhonePadScreen(
    input: PhonePadInput, layout: List<PadBtnF>, sticks: Sticks, sizeMul: Float, alpha: Float,
    rpad: String, lpad: String, dpadMerged: Boolean, selectMode: Boolean, selected: String,
    rightAsMouse: Boolean, onToggleMouseLook: () -> Unit, onToggleMouse: (Boolean) -> Unit,
    onSelect: (String) -> Unit, onMove: (String, Float, Float) -> Unit,
    onResize: (String, Float, Float) -> Unit,
    onStickMove: (String, Float, Float) -> Unit, onStickResize: (String, Float) -> Unit,
    onOpenDesign: () -> Unit,
) {
    val dens = LocalDensity.current
    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = maxWidth; val h = maxHeight
            val u = if (w < h) w else h
            if (lpad == "touch") {
                LeftTouchPad(w = w, h = h, alpha = sticks.la, enabled = !selectMode) { x, y ->
                    input.lx = x; input.ly = y; input.pushNow()
                }
            } else {
                RespStick(
                    cx = sticks.lx, cy = sticks.ly, dFrac = sticks.ld, w = w, h = h, label = "L",
                    alpha = sticks.la, selectMode = selectMode, absolute = true,
                    onBaseMove = { dx, dy -> onStickMove("L", dx, dy) },
                    onBaseResize = { d -> onStickResize("L", d) },
                ) { x, y ->
                    input.lx = x; input.ly = y; input.pushNow()
                }
            }
            if (rightAsMouse) {
                MousePad(input = input, alpha = sticks.ra, enabled = !selectMode)
            } else {
            if (rpad == "touch") {
                RightTouchPad(w = w, h = h, alpha = sticks.ra, enabled = !selectMode) { x, y ->
                    input.rx = x; input.ry = y; input.pushNow()
                }
            } else {
                RespStick(
                    cx = sticks.rx, cy = sticks.ry, dFrac = sticks.rd, w = w, h = h, label = "R",
                    alpha = sticks.ra, selectMode = selectMode, absolute = true,
                    onBaseMove = { dx, dy -> onStickMove("R", dx, dy) },
                    onBaseResize = { d -> onStickResize("R", d) },
                ) { x, y ->
                    input.rx = x; input.ry = y; input.pushNow()
                }
            }
            }
            val dpadBtns = layout.filter {
                it.bit == Btn.DU || it.bit == Btn.DD || it.bit == Btn.DL || it.bit == Btn.DR
            }
            // Tasarımda 4 ayrı tuş taşınır, oyunda tek buton çizilir.
            val showMerged = dpadMerged && !selectMode && dpadBtns.any { it.vis }
            layout.forEach { b ->
                if (showMerged && (b.bit == Btn.DU || b.bit == Btn.DD || b.bit == Btn.DL || b.bit == Btn.DR)) return@forEach
                key(b.label) {
                    if (b.vis || selectMode) {
                        val bw = u * b.size * sizeMul * b.ratio
                        val bh = u * b.size * sizeMul
                        Box(
                            Modifier.offset(x = w * b.x - bw / 2, y = h * b.y - bh / 2)
                                .size(bw, bh)
                                .alpha(if (b.vis) alpha else 0.3f)
                                .then(if (selectMode) Modifier.pointerInput(b.label) {
                                    detectDragGestures(
                                        onDragStart = { onSelect(b.label) },
                                        onDragEnd = { },
                                        onDragCancel = { },
                                    ) { ch, drag ->
                                        ch.consume()
                                        onSelect(b.label)
                                        with(dens) {
                                            onMove(b.label, drag.x.toDp() / w, drag.y.toDp() / h)
                                        }
                                    }
                                } else Modifier),
                        ) {
                            PadKey(
                                label = if (b.bit == Btn.MOUSE) (if (rightAsMouse) "Stikler" else "Fare")
                                else b.txt.ifBlank { b.label } + if (b.vis && b.bit != b.out) "→${shortBitName(b.out)}" else "",
                                wide = b.wide,
                                ghost = !b.vis,
                                selected = selectMode && b.label == selected,
                                onDown = {
                                    if (selectMode) onSelect(b.label)
                                    else if (b.bit == Btn.MOUSE) { onToggleMouseLook(); onToggleMouse(!rightAsMouse) }
                                    else input.setButton(b.out, true)
                                },
                                onUp = { if (!selectMode && b.bit != Btn.MOUSE) input.setButton(b.out, false) },
                            )
                        }
                        if (selectMode && b.vis && b.label == selected) {
                            val hs = androidx.compose.ui.unit.Dp(30f)
                            Box(
                                Modifier.offset(x = w * b.x + bw / 2 - hs / 2, y = h * b.y + bh / 2 - hs / 2)
                                    .size(hs)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color(0xFFFFC857))
                                    .border(2.dp, Color.White, androidx.compose.foundation.shape.CircleShape)
                                    .pointerInput(b.label) {
                                        detectDragGestures { ch, drag ->
                                            ch.consume()
                                            with(dens) {
                                                onResize(b.label, drag.x.toDp() / u, drag.y.toDp() / u)
                                            }
                                        }
                                    },
                            )
                        }
                    }
                }
            }
            if (showMerged) {
                // 4 tuşun kapladığı alan kadar tek yuvarlak buton.
                val vis = dpadBtns.filter { it.vis }.ifEmpty { dpadBtns }
                val mcx = vis.map { it.x }.average().toFloat()
                val mcy = vis.map { it.y }.average().toFloat()
                val avgS = vis.map { it.size }.average().toFloat()
                val spanX = w * (vis.maxOf { it.x } - vis.minOf { it.x }) + u * avgS * sizeMul
                val spanY = h * (vis.maxOf { it.y } - vis.minOf { it.y }) + u * avgS * sizeMul
                val md = maxOf(spanX, spanY, u * 0.2f).coerceAtMost(u * 0.5f)
                MergedDpad(cx = mcx, cy = mcy, w = w, h = h, d = md, alpha = alpha) { dirs ->
                    input.setDpad(dirs)
                }
            }
        }
        var menuExp by remember { mutableStateOf(false) }
        val dbg = try { app.gamenative.PluviaApp.inputControlsView?.phonePadDebug() ?: "icv yok" } catch (_: Exception) { "dbg hata" }
        Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            androidx.compose.foundation.layout.Row {
                MiniBtn("☰") { menuExp = true }
            }
            DropdownMenu(expanded = menuExp, onDismissRequest = { menuExp = false }) {
                DropdownMenuItem(
                    text = { Text("Tasarım") },
                    onClick = { menuExp = false; onOpenDesign() },
                )
            }
        }
    }
}

@Composable
fun PhonePadDesign(
    input: PhonePadInput, layout: List<PadBtnF>, sticks: Sticks, alpha: Float, sizeMul: Float,
    rpad: String, lpad: String, dpadMerged: Boolean, onDpadMerged: (Boolean) -> Unit,
    selected: String, selectMode: Boolean,
    panelLeft: Boolean, onToggleSide: () -> Unit,
    onSelect: (String) -> Unit, onLayout: (MutableList<PadBtnF>) -> Unit, onSticks: (Sticks) -> Unit,
    onRpad: (String) -> Unit, onLpad: (String) -> Unit,
    onAlpha: (Float) -> Unit, onSizeMul: (Float) -> Unit, onSelectMode: (Boolean) -> Unit,
    onSave: () -> Unit, onHide: () -> Unit, onClose: () -> Unit,
    onExport: () -> Unit, onImport: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val sel = layout.firstOrNull { it.label == selected } ?: layout.first()
    var volUpSel by remember { mutableStateOf(DEF_VOL_UP) }
    var volDownSel by remember { mutableStateOf(DEF_VOL_DOWN) }
    var openSec by remember { mutableStateOf(setOf("tus")) }
    LaunchedEffect(Unit) {
        val p = ctx.phonePadStore.data.first()
        volUpSel = prettyXboxName(p[K_VOL_UP] ?: DEF_VOL_UP)
        volDownSel = prettyXboxName(p[K_VOL_DOWN] ?: DEF_VOL_DOWN)
    }
    fun mutate(label: String, fn: (PadBtnF) -> PadBtnF) {
        onLayout(layout.map { if (it.label == label) fn(it) else it }.toMutableList())
    }
    fun toggle(sec: String) { openSec = if (sec in openSec) openSec - sec else openSec + sec }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Tasarım", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onToggleSide, shape = R8) { Text(if (panelLeft) "Sağa" else "Sola") }
            TextButton(onClick = onHide, shape = R8) { Text("Gizle") }
            Button(onClick = onClose, shape = R8) { Text("Bitti") }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Düzenleme", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = selectMode, onCheckedChange = onSelectMode)
        }
        Text(
            "Dokun seçim yapar, sürükle taşır. Sarı nokta boyutlandırır.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Section(title = "Tuş", open = "tus" in openSec, onToggle = { toggle("tus") }) {
                DropDown(sel.label, layout.map { it.label }) { onSelect(it) }
                OutlinedTextField(
                    value = sel.txt, onValueChange = { mutate(sel.label) { b -> b.copy(txt = it.take(6)) } },
                    label = { Text("Yazı (boşsa varsayılan)") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                SliderRow("Yatay (X)", sel.x, 0.03f, 0.97f) { mutate(sel.label) { b -> b.copy(x = it) } }
                SliderRow("Dikey (Y)", sel.y, 0.03f, 0.97f) { mutate(sel.label) { b -> b.copy(y = it) } }
                SliderRow("Boyut", sel.size, 0.05f, 0.24f) { mutate(sel.label) { b -> b.copy(size = it) } }
                SliderRow("En (oran)", sel.ratio, 0.6f, 4f) { mutate(sel.label) { b -> b.copy(ratio = it) } }
                Text("Gönderdiği buton", style = MaterialTheme.typography.bodySmall)
                if (sel.bit == Btn.MOUSE) Text(
                    "Fare/Stikler geçişi (sabit, oyuna tuş göndermez).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
                ) else DropDown(bitToXboxName(sel.out), XBOX_NAMES) { mutate(sel.label) { b -> b.copy(out = xboxNameToBit(it)) } }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ekranda görünsün", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = sel.vis, onCheckedChange = { mutate(sel.label) { b -> b.copy(vis = it) } })
                }
                if (sel.bit != sel.out && sel.bit != Btn.MOUSE) Text(
                    "${sel.label} → ${bitToXboxName(sel.out)} olarak gönderiliyor.",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFC857),
                )
            }
            Section(title = "Analog", open = "analog" in openSec, onToggle = { toggle("analog") }) {
                Text("Sol taraf", style = MaterialTheme.typography.bodySmall)
                DropDown(
                    if (lpad == "touch") "Dokunmatik alan" else "Analog çubuk",
                    listOf("Analog çubuk", "Dokunmatik alan"),
                ) {
                    onLpad(if (it.startsWith("Dokun")) "touch" else "stick")
                }
                Text("Sağ taraf", style = MaterialTheme.typography.bodySmall)
                DropDown(
                    if (rpad == "touch") "Dokunmatik alan" else "Analog çubuk",
                    listOf("Analog çubuk", "Dokunmatik alan"),
                ) {
                    onRpad(if (it.startsWith("Dokun")) "touch" else "stick")
                }
                SliderRow("Sol X", sticks.lx, 0.05f, 0.45f) { onSticks(sticks.copy(lx = it)) }
                SliderRow("Sol Y", sticks.ly, 0.4f, 0.95f) { onSticks(sticks.copy(ly = it)) }
                SliderRow("Sol çap", sticks.ld, 0.18f, 0.42f) { onSticks(sticks.copy(ld = it)) }
                SliderRow("Sol saydamlık", sticks.la, 0.1f, 1f) { onSticks(sticks.copy(la = it)) }
                SliderRow("Sağ X", sticks.rx, 0.55f, 0.95f) { onSticks(sticks.copy(rx = it)) }
                SliderRow("Sağ Y", sticks.ry, 0.4f, 0.95f) { onSticks(sticks.copy(ry = it)) }
                SliderRow("Sağ çap", sticks.rd, 0.18f, 0.42f) { onSticks(sticks.copy(rd = it)) }
                SliderRow("Sağ saydamlık", sticks.ra, 0.1f, 1f) { onSticks(sticks.copy(ra = it)) }
            }
            Section(title = "D-Pad", open = "dpad" in openSec, onToggle = { toggle("dpad") }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Birleşik D-Pad", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = dpadMerged, onCheckedChange = onDpadMerged)
                }
                Text(
                    "Açıkken 4 yön tek butonda birleşir: basılı tutup kaydırdığın yöne basar (çaprazda ikisi birden). Konum için tasarımda 4 ayrı tuşu taşı.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
                )
            }
            Section(title = "Genel", open = "genel" in openSec, onToggle = { toggle("genel") }) {
                SliderRow("Tuş boyutu (hepsi)", sizeMul, 0.7f, 1.6f) { onSizeMul(it) }
                SliderRow("Saydamlık", alpha, 0.3f, 1f) { onAlpha(it) }
            }
            Section(title = "Ses tuşları", open = "ses" in openSec, onToggle = { toggle("ses") }) {
                Text("Ses Aç", style = MaterialTheme.typography.bodySmall)
                DropDown(volUpSel, XBOX_NAMES) { v ->
                    volUpSel = v; scope.launch { ctx.phonePadStore.edit { it[K_VOL_UP] = v } }
                    input.volUpBit = xboxNameToBit(v)
                }
                Text("Ses Kıs", style = MaterialTheme.typography.bodySmall)
                DropDown(volDownSel, XBOX_NAMES) { v ->
                    volDownSel = v; scope.launch { ctx.phonePadStore.edit { it[K_VOL_DOWN] = v } }
                    input.volDownBit = xboxNameToBit(v)
                }
            }
            Section(title = "Yedek", open = "yedek" in openSec, onToggle = { toggle("yedek") }) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f), shape = R8) { Text("Dışa aktar") }
                    OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f), shape = R8) { Text("İçe aktar") }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                onLayout(defaultLayoutF()); onSticks(Sticks()); onSizeMul(1f); onAlpha(0.92f)
                onRpad("stick"); onLpad("stick"); onDpadMerged(false)
            }, shape = R8) { Text("Sıfırla") }
            Button(onClick = onSave, modifier = Modifier.weight(1f), shape = R8) { Text("Kaydet") }
        }
    }
}
