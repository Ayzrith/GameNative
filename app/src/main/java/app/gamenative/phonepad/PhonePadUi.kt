package app.gamenative.phonepad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val R8 = RoundedCornerShape(8.dp)

@Composable
fun Section(
    title: String,
    open: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (open) "-" else "+", color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
            )
        }
        if (open) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

@Composable
fun SliderRow(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 12.sp, modifier = Modifier.width(110.dp))
        Slider(value = value, onValueChange = onChange, valueRange = min..max, modifier = Modifier.weight(1f))
        Text(
            String.format("%.2f", value), fontSize = 11.sp, modifier = Modifier.width(36.dp),
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
fun DropDown(cur: String, opts: List<String>, onPick: (String) -> Unit) {
    var exp by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { exp = true }, shape = R8) { Text(cur) }
        DropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
            opts.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { onPick(o); exp = false }) }
        }
    }
}

@Composable
fun MiniBtn(icon: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(R8).background(Color(0xAA22252A))
            .border(1.dp, Color(0xFF4A5058), R8).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(icon, fontSize = 20.sp)
    }
}

@Composable
fun PadKey(
    label: String, wide: Boolean, ghost: Boolean = false, selected: Boolean = false,
    onDown: () -> Unit, onUp: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val shape = if (wide) R8 else CircleShape
    Box(
        Modifier.fillMaxSize()
            .clip(shape)
            .background(
                if (pressed) Color(0xFF4A5058)
                else if (ghost) Color(0xFF22252A)
                else Color(0xFF2E3238),
            )
            .border(
                2.dp,
                if (selected) Color(0xFFFFC857)
                else if (ghost) Color(0xFF6E757E)
                else if (pressed) Color(0xFFC2C8D0)
                else Color(0xFF4A5058), shape,
            )
            .pointerInput(label) {
                detectTapGestures(
                    onPress = {
                        pressed = true; onDown()
                        try {
                            tryAwaitRelease()
                        } finally {
                            pressed = false; onUp()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, color = if (pressed) Color.White else Color(0xFFD6DAE0),
            fontWeight = FontWeight.Bold, fontSize = if (wide) 13.sp else 16.sp, maxLines = 1,
        )
    }
}

@Composable
fun BoxScope.RespStick(
    cx: Float, cy: Float, dFrac: Float, w: Dp, h: Dp, label: String,
    alpha: Float = 1f,
    selectMode: Boolean = false,
    absolute: Boolean = false,
    onBaseMove: (Float, Float) -> Unit = { _, _ -> },
    onBaseResize: (Float) -> Unit = {},
    onMove: (Float, Float) -> Unit,
) {
    val u = if (w < h) w else h
    val d = u * dFrac
    var knob by remember { mutableStateOf(Offset.Zero) }
    val dens = LocalDensity.current
    val rangePx = with(dens) { (d / 2 - d * 0.04f).toPx() }
    val gestMod = if (selectMode) {
        Modifier.pointerInput(cx, cy) {
            detectDragGestures { ch, drag ->
                ch.consume()
                with(dens) { onBaseMove(drag.x.toDp() / w, drag.y.toDp() / h) }
            }
        }
    } else if (!absolute) {
        Modifier.pointerInput(cx, cy, d) {
            detectDragGestures(
                onDragEnd = { knob = Offset.Zero; onMove(0f, 0f) },
                onDragCancel = { knob = Offset.Zero; onMove(0f, 0f) },
            ) { ch, drag ->
                ch.consume()
                val nx = (knob.x + drag.x).coerceIn(-rangePx, rangePx)
                val ny = (knob.y + drag.y).coerceIn(-rangePx, rangePx)
                knob = Offset(nx, ny); onMove(nx / rangePx, ny / rangePx)
            }
        }
    } else {
        Modifier.pointerInput(cx, cy, d) {
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()
                val cxPx = size.width / 2f
                val cyPx = size.height / 2f
                fun place(p: Offset) {
                    val dx = p.x - cxPx
                    val dy = p.y - cyPx
                    val dist = sqrt(dx * dx + dy * dy)
                    val nx: Float
                    val ny: Float
                    if (dist > rangePx && dist > 0f) {
                        nx = dx / dist * rangePx
                        ny = dy / dist * rangePx
                    } else {
                        nx = dx; ny = dy
                    }
                    knob = Offset(nx, ny)
                    onMove(nx / rangePx, ny / rangePx)
                }
                try {
                    place(down.position)
                    var up = false
                    while (!up) {
                        val ev = awaitPointerEvent()
                        ev.changes.forEach { ch ->
                            if (ch.id == down.id) {
                                if (!ch.pressed) up = true
                                else { ch.consume(); place(ch.position) }
                            }
                        }
                    }
                } finally {
                    knob = Offset.Zero
                    onMove(0f, 0f)
                }
            }
        }
    }
    Box(
        Modifier.align(Alignment.TopStart)
            .offset(x = w * cx - d / 2, y = h * cy - d / 2)
            .size(d)
            .clip(CircleShape).background(Color(0xFF22252A))
            .border(2.dp, Color(0xFF3C4148), CircleShape)
            .alpha(alpha)
            .then(gestMod), contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.offset { IntOffset(knob.x.roundToInt(), knob.y.roundToInt()) }
                .size(d * 0.47f)
                .clip(CircleShape).background(Color(0xFF4A5058))
                .border(2.dp, Color(0xFF6E757E), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, color = Color(0xFFD6DAE0), fontWeight = FontWeight.Bold)
        }
        if (selectMode) {
            val hs = 30.dp
            Box(
                Modifier.align(Alignment.TopStart)
                    .offset(x = d - hs / 2, y = d - hs / 2)
                    .size(hs)
                    .clip(CircleShape).background(Color(0xFFFFC857))
                    .border(2.dp, Color.White, CircleShape)
                    .pointerInput(cx, cy) {
                        detectDragGestures { ch, drag ->
                            ch.consume()
                            with(dens) { onBaseResize(((drag.x.toDp() + drag.y.toDp()) / 2) / u) }
                        }
                    },
            )
        }
    }
}

@Composable
fun BoxScope.MousePad(input: PhonePadInput, alpha: Float = 1f, enabled: Boolean) {
    Box(
        Modifier.align(Alignment.TopEnd).fillMaxHeight().fillMaxWidth(0.5f)
            .alpha(alpha)
            .then(if (enabled) Modifier
                .pointerInput(Unit) { detectTapGestures(onTap = { input.mouseClick(false) }) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { ch, drag -> ch.consume(); input.mouseMove(drag.x, drag.y) },
                    )
                }
            else Modifier.border(1.dp, Color(0xFF2E3238))),
        contentAlignment = Alignment.Center,
    ) {
        Text("FARE", fontSize = 12.sp, color = Color(0xFF4A5058), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BoxScope.RightTouchPad(w: Dp, h: Dp, alpha: Float = 1f, enabled: Boolean, onMove: (Float, Float) -> Unit) {
    val u = if (w < h) w else h
    val dens = LocalDensity.current
    val rangePx = with(dens) { (u * 0.18f).toPx() }
    val knobD = u * 0.14f
    val knobPx = with(dens) { knobD.toPx() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var cur by remember { mutableStateOf(Offset.Zero) }
    var active by remember { mutableStateOf(false) }
    Box(
        Modifier.align(Alignment.TopEnd).fillMaxHeight().fillMaxWidth(0.5f)
            .then(if (!enabled) Modifier.border(1.dp, Color(0xFF2E3238)) else Modifier)
            .then(if (enabled) Modifier.pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun emit(p: Offset) {
                        val nx = (p.x - origin.x).coerceIn(-rangePx, rangePx)
                        val ny = (p.y - origin.y).coerceIn(-rangePx, rangePx)
                        cur = Offset(origin.x + nx, origin.y + ny)
                        onMove(nx / rangePx, ny / rangePx)
                    }
                    origin = down.position; cur = down.position; active = true
                    down.consume()
                    emit(down.position)
                    var done = false
                    while (!done) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id }
                        if (c == null || !c.pressed) done = true
                        else { c.consume(); cur = c.position; emit(c.position) }
                    }
                    active = false; onMove(0f, 0f)
                }
            } else Modifier), contentAlignment = Alignment.Center,
    ) {
        if (!active && !enabled) Text(
            "SAĞ ALAN (oyunda aktif)",
            fontSize = 12.sp, color = Color(0xFF4A5058), fontWeight = FontWeight.Bold,
        )
    }
    if (active) {
        val halfWPx = with(dens) { (w / 2).toPx() }
        Box(
            Modifier.align(Alignment.TopStart)
                .offset { IntOffset((halfWPx + cur.x - knobPx / 2).roundToInt(), (cur.y - knobPx / 2).roundToInt()) }
                .size(knobD).alpha(alpha)
                .clip(CircleShape).background(Color(0xFF4A5058))
                .border(2.dp, Color(0xFF6E757E), CircleShape),
        )
    }
}

@Composable
fun BoxScope.LeftTouchPad(w: Dp, h: Dp, alpha: Float = 1f, enabled: Boolean, onMove: (Float, Float) -> Unit) {
    val u = if (w < h) w else h
    val dens = LocalDensity.current
    val rangePx = with(dens) { (u * 0.18f).toPx() }
    val knobD = u * 0.14f
    val knobPx = with(dens) { knobD.toPx() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var cur by remember { mutableStateOf(Offset.Zero) }
    var active by remember { mutableStateOf(false) }
    Box(
        Modifier.align(Alignment.TopStart).fillMaxHeight().fillMaxWidth(0.5f)
            .then(if (!enabled) Modifier.border(1.dp, Color(0xFF2E3238)) else Modifier)
            .then(if (enabled) Modifier.pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun emit(p: Offset) {
                        val nx = (p.x - origin.x).coerceIn(-rangePx, rangePx)
                        val ny = (p.y - origin.y).coerceIn(-rangePx, rangePx)
                        cur = Offset(origin.x + nx, origin.y + ny)
                        onMove(nx / rangePx, ny / rangePx)
                    }
                    origin = down.position; cur = down.position; active = true
                    down.consume()
                    emit(down.position)
                    var done = false
                    while (!done) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id }
                        if (c == null || !c.pressed) done = true
                        else { c.consume(); cur = c.position; emit(c.position) }
                    }
                    active = false; onMove(0f, 0f)
                }
            } else Modifier), contentAlignment = Alignment.Center,
    ) {
        if (!active && !enabled) Text(
            "SOL ALAN (oyunda aktif)",
            fontSize = 12.sp, color = Color(0xFF4A5058), fontWeight = FontWeight.Bold,
        )
    }
    if (active) {
        Box(
            Modifier.align(Alignment.TopStart)
                .offset { IntOffset((cur.x - knobPx / 2).roundToInt(), (cur.y - knobPx / 2).roundToInt()) }
                .size(knobD).alpha(alpha)
                .clip(CircleShape).background(Color(0xFF4A5058))
                .border(2.dp, Color(0xFF6E757E), CircleShape),
        )
    }
}

// Sürükleme vektöründen basılacak D-Pad bitleri (ölü bölge 0.25, çaprazda iki bit).
fun dpadBitsFor(x: Float, y: Float): Set<Int> {
    if (x * x + y * y < 0.0625f) return emptySet()
    val s = mutableSetOf<Int>()
    if (y < -0.25f) s.add(Btn.DU)
    if (y > 0.25f) s.add(Btn.DD)
    if (x < -0.25f) s.add(Btn.DL)
    if (x > 0.25f) s.add(Btn.DR)
    return s
}

@Composable
private fun DirGlyph(text: String, on: Boolean, modifier: Modifier) {
    Box(
        modifier.clip(CircleShape)
            .background(if (on) Color(0xFFFFC857) else Color(0xFF2E3238))
            .border(1.dp, if (on) Color.White else Color(0xFF4A5058), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, color = if (on) Color.Black else Color(0xFFD6DAE0),
            fontWeight = FontWeight.Bold, fontSize = 14.sp,
        )
    }
}

// Birleşik D-Pad: tek buton, basılı tutup kaydırılan yön(ler)e basar.
@Composable
fun BoxScope.MergedDpad(
    cx: Float, cy: Float, w: Dp, h: Dp, d: Dp,
    alpha: Float = 1f,
    onDirs: (Set<Int>) -> Unit,
) {
    var active by remember { mutableStateOf(emptySet<Int>()) }
    Box(
        Modifier.align(Alignment.TopStart)
            .offset(x = w * cx - d / 2, y = h * cy - d / 2)
            .size(d)
            .clip(CircleShape).background(Color(0xFF22252A))
            .border(2.dp, Color(0xFF3C4148), CircleShape)
            .alpha(alpha)
            .pointerInput(cx, cy, d) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val cxPx = size.width / 2f
                    val cyPx = size.height / 2f
                    val rangePx = if (cxPx < cyPx) cxPx else cyPx
                    fun update(p: Offset) {
                        val dirs = if (rangePx > 0f) {
                            dpadBitsFor((p.x - cxPx) / rangePx, (p.y - cyPx) / rangePx)
                        } else emptySet()
                        if (dirs != active) {
                            active = dirs; onDirs(dirs)
                        }
                    }
                    try {
                        update(down.position)
                        var up = false
                        while (!up) {
                            val ev = awaitPointerEvent()
                            ev.changes.forEach { ch ->
                                if (ch.id == down.id) {
                                    if (!ch.pressed) up = true
                                    else { ch.consume(); update(ch.position) }
                                }
                            }
                        }
                    } finally {
                        active = emptySet<Int>()
                        onDirs(emptySet())
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val g = d * 0.26f
        DirGlyph("▲", Btn.DU in active, Modifier.align(Alignment.TopCenter).size(g))
        DirGlyph("▼", Btn.DD in active, Modifier.align(Alignment.BottomCenter).size(g))
        DirGlyph("◀", Btn.DL in active, Modifier.align(Alignment.CenterStart).size(g))
        DirGlyph("▶", Btn.DR in active, Modifier.align(Alignment.CenterEnd).size(g))
        Text("✛", color = Color(0xFF6E757E), fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}
