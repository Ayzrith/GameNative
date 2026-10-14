package app.gamenative.phonepad

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.winlator.inputcontrols.Binding
import org.json.JSONObject

// PhoneGamepad (com.phonegamepad) düzen verisinin birebir portu; ağ kodu yok.
val Context.phonePadStore by preferencesDataStore("phonepad")

val K_VOL_UP = stringPreferencesKey("vol_up")
val K_VOL_DOWN = stringPreferencesKey("vol_down")
val K_LAYOUT = stringPreferencesKey("layout")
val K_ALPHA = floatPreferencesKey("alpha")
val K_DPAD_MERGED = booleanPreferencesKey("dpad_merged")

const val DEF_VOL_UP = "L1"
const val DEF_VOL_DOWN = "L2"

object Btn {
    const val A = 0; const val B = 1; const val X = 2; const val Y = 3
    const val LB = 4; const val RB = 5; const val LTB = 6; const val RTB = 7
    const val BACK = 8; const val START = 9; const val L3 = 10; const val R3 = 11
    const val GUIDE = 12; const val DU = 13; const val DD = 14; const val DL = 15; const val DR = 16
    const val MOUSE = 19 // Fare/Stik geçiş tuşu (stok Enable Mouse/Sticks'in karşılığı)
}

val XBOX_FULL = listOf(
    "A" to 0, "B" to 1, "X" to 2, "Y" to 3,
    "L1" to 4, "R1" to 5, "L2" to 6, "R2" to 7,
    "Back" to 8, "Start" to 9, "L3" to 10, "R3" to 11, "Home" to 12,
    "Yukarı" to 13, "Aşağı" to 14, "Sol" to 15, "Sağ" to 16,
    "Ekstra 1" to 17, "Ekstra 2" to 18,
)
val XBOX_NAMES = XBOX_FULL.map { it.first }

fun bitToXboxName(bit: Int): String = XBOX_FULL.firstOrNull { it.second == bit }?.first ?: "A"

fun prettyXboxName(n: String): String = when {
    n.startsWith("LB") -> "L1"
    n.startsWith("RB") -> "R1"
    n.startsWith("LT") -> "L2"
    n.startsWith("RT") -> "R2"
    n.startsWith("Guide") -> "Home"
    n.startsWith("D-Up") || n == "DU" -> "Yukarı"
    n.startsWith("D-Down") || n == "DD" -> "Aşağı"
    n.startsWith("D-Left") || n == "DL" -> "Sol"
    n.startsWith("D-Right") || n == "DR" -> "Sağ"
    n.startsWith("Extra1") || n.startsWith("Ekstra 1") -> "Ekstra 1"
    n.startsWith("Extra2") || n.startsWith("Ekstra 2") -> "Ekstra 2"
    else -> n
}

fun shortBitName(bit: Int): String = when (bit) {
    0 -> "A"; 1 -> "B"; 2 -> "X"; 3 -> "Y"
    4 -> "L1"; 5 -> "R1"; 6 -> "L2"; 7 -> "R2"
    8 -> "Back"; 9 -> "Start"; 10 -> "L3"; 11 -> "R3"; 12 -> "Home"
    13 -> "▲"; 14 -> "▼"; 15 -> "◀"; 16 -> "▶"
    17 -> "E1"; 18 -> "E2"; 19 -> "Fare"; else -> "?"
}

fun xboxNameToBit(name: String): Int = when {
    name == "A" -> Btn.A
    name == "B" -> Btn.B
    name == "X" -> Btn.X
    name == "Y" -> Btn.Y
    name == "L1" || name.startsWith("LB") -> Btn.LB
    name == "R1" || name.startsWith("RB") -> Btn.RB
    name == "L2" || name.startsWith("LT") -> Btn.LTB
    name == "R2" || name.startsWith("RT") -> Btn.RTB
    name.startsWith("Back") -> Btn.BACK
    name.startsWith("Start") -> Btn.START
    name.startsWith("L3") -> Btn.L3
    name.startsWith("R3") -> Btn.R3
    name.startsWith("Home") || name.startsWith("Guide") -> Btn.GUIDE
    name == "Yukarı" || name.startsWith("D-Up") || name == "DU" || name == "▲" -> Btn.DU
    name == "Aşağı" || name.startsWith("D-Down") || name == "DD" || name == "▼" -> Btn.DD
    name == "Sol" || name.startsWith("D-Left") || name == "DL" || name == "◀" -> Btn.DL
    name == "Sağ" || name.startsWith("D-Right") || name == "DR" || name == "▶" -> Btn.DR
    name.startsWith("Ekstra 1") || name.startsWith("Extra1") -> 17
    name.startsWith("Ekstra 2") || name.startsWith("Extra2") -> 18
    else -> Btn.A
}

// shortcut: Binding'de Guide yok (12 XInput butonu var) -> NONE; E1/E2 klavye+menüye bağlı.
fun phoneBitToBinding(bit: Int): Binding = when (bit) {
    0 -> Binding.GAMEPAD_BUTTON_A
    1 -> Binding.GAMEPAD_BUTTON_B
    2 -> Binding.GAMEPAD_BUTTON_X
    3 -> Binding.GAMEPAD_BUTTON_Y
    4 -> Binding.GAMEPAD_BUTTON_L1
    5 -> Binding.GAMEPAD_BUTTON_R1
    6 -> Binding.GAMEPAD_BUTTON_L2
    7 -> Binding.GAMEPAD_BUTTON_R2
    8 -> Binding.GAMEPAD_BUTTON_SELECT
    9 -> Binding.GAMEPAD_BUTTON_START
    10 -> Binding.GAMEPAD_BUTTON_L3
    11 -> Binding.GAMEPAD_BUTTON_R3
    13 -> Binding.GAMEPAD_DPAD_UP
    14 -> Binding.GAMEPAD_DPAD_DOWN
    15 -> Binding.GAMEPAD_DPAD_LEFT
    16 -> Binding.GAMEPAD_DPAD_RIGHT
    17 -> Binding.SHOW_KEYBOARD
    18 -> Binding.OPEN_RADIAL_MENU
    else -> Binding.NONE
}

data class PadBtnF(
    val label: String, var x: Float, var y: Float, var size: Float,
    val bit: Int, val wide: Boolean = false,
    var out: Int = bit, var vis: Boolean = true,
    var ratio: Float = 1f, var txt: String = "",
)

data class Sticks(
    var lx: Float = STICK_LX, var ly: Float = STICK_LY, var ld: Float = STICK_D,
    var rx: Float = STICK_RX, var ry: Float = STICK_RY, var rd: Float = STICK_D,
    var la: Float = STICK_ALPHA, var ra: Float = STICK_ALPHA,
)

data class ParsedLayout(
    val btns: MutableList<PadBtnF>, val sticks: Sticks,
    val rpad: String, val lpad: String, val rmouse: Boolean = false,
)

fun defaultLayoutF(): MutableList<PadBtnF> = mutableListOf(
    PadBtnF("L1", 0.135f, 0.085f, 0.095f, Btn.LB, wide = true, ratio = 2.6f),
    PadBtnF("L2", 0.135f, 0.225f, 0.095f, Btn.LTB, wide = true, ratio = 2.6f),
    PadBtnF("R1", 0.865f, 0.085f, 0.095f, Btn.RB, wide = true, ratio = 2.6f),
    PadBtnF("R2", 0.865f, 0.225f, 0.095f, Btn.RTB, wide = true, ratio = 2.6f),
    PadBtnF("Back", 0.452f, 0.085f, 0.072f, Btn.BACK),
    PadBtnF("Start", 0.548f, 0.085f, 0.072f, Btn.START),
    PadBtnF("Y", 0.862f, 0.355f, 0.108f, Btn.Y),
    PadBtnF("X", 0.812f, 0.485f, 0.108f, Btn.X),
    PadBtnF("B", 0.912f, 0.485f, 0.108f, Btn.B),
    PadBtnF("A", 0.862f, 0.615f, 0.108f, Btn.A),
    PadBtnF("▲", 0.138f, 0.355f, 0.108f, Btn.DU),
    PadBtnF("◀", 0.088f, 0.485f, 0.108f, Btn.DL),
    PadBtnF("▶", 0.188f, 0.485f, 0.108f, Btn.DR),
    PadBtnF("▼", 0.138f, 0.615f, 0.108f, Btn.DD),
    PadBtnF("L3", 0.30f, 0.68f, 0.09f, Btn.L3, vis = true),
    PadBtnF("R3", 0.70f, 0.68f, 0.09f, Btn.R3, vis = true),
    PadBtnF("Home", 0.5f, 0.21f, 0.072f, Btn.GUIDE, vis = false),
    PadBtnF("E1", 0.5f, 0.30f, 0.08f, 17, vis = false),
    PadBtnF("E2", 0.5f, 0.42f, 0.08f, 18, vis = false),
    PadBtnF("Fare", 0.5f, 0.065f, 0.08f, Btn.MOUSE, wide = true, ratio = 2.6f),
)

val LEGACY_LABEL = mapOf(
    "L1" to "LB", "R1" to "RB", "L2" to "LT", "R2" to "RT",
    "↑" to "DU", "↓" to "DD", "←" to "DL", "→" to "DR",
    "▲" to "DU", "▼" to "DD", "◀" to "DL", "▶" to "DR",
    "Home" to "Guide", "E1" to "Ex1", "E2" to "Ex2",
)

const val STICK_LX = 0.163f; const val STICK_LY = 0.83f
const val STICK_RX = 0.837f; const val STICK_RY = 0.83f
const val STICK_D = 0.30f
const val STICK_ALPHA = 0.92f

fun parseLayoutJson(js: String): ParsedLayout {
    val j = JSONObject(js)
    var out = defaultLayoutF()
    var st = Sticks()
    var rp = "stick"
    var lp = "stick"
    var rm = false
    when (j.optInt("v", 0)) {
        4, 5, 6 -> {
            val o = j.getJSONObject("btns")
            out = defaultLayoutF().map { b ->
                val key = if (o.has(b.label)) b.label else LEGACY_LABEL[b.label]
                    ?: LEGACY_LABEL.entries.firstOrNull { it.value == b.label }?.key
                if (key != null && o.has(key)) {
                    val q = o.getJSONObject(key)
                    b.copy(
                        x = q.optDouble("x", b.x.toDouble()).toFloat().coerceIn(0f, 1f),
                        y = q.optDouble("y", b.y.toDouble()).toFloat().coerceIn(0f, 1f),
                        size = q.optDouble("s", b.size.toDouble()).toFloat().coerceIn(0.03f, 0.3f),
                        out = q.optInt("out", b.bit).coerceIn(0, 19),
                        vis = q.optBoolean("vis", true),
                        txt = q.optString("t", ""),
                        ratio = q.optDouble("ar", (if (b.wide) 2.6 else 1.0)).toFloat().coerceIn(0.5f, 5f),
                    )
                } else b
            }.toMutableList()
            j.optJSONObject("sticks")?.let { s ->
                st = Sticks(
                    lx = s.optDouble("lx", STICK_LX.toDouble()).toFloat().coerceIn(0f, 1f),
                    ly = s.optDouble("ly", STICK_LY.toDouble()).toFloat().coerceIn(0f, 1f),
                    ld = s.optDouble("ld", STICK_D.toDouble()).toFloat().coerceIn(0.1f, 0.5f),
                    rx = s.optDouble("rx", STICK_RX.toDouble()).toFloat().coerceIn(0f, 1f),
                    ry = s.optDouble("ry", STICK_RY.toDouble()).toFloat().coerceIn(0f, 1f),
                    rd = s.optDouble("rd", STICK_D.toDouble()).toFloat().coerceIn(0.1f, 0.5f),
                    la = s.optDouble("la", STICK_ALPHA.toDouble()).toFloat().coerceIn(0.1f, 1f),
                    ra = s.optDouble("ra", STICK_ALPHA.toDouble()).toFloat().coerceIn(0.1f, 1f),
                )
            }
            rp = if (j.optString("rpad", "stick") == "touch") "touch" else "stick"
            lp = if (j.optString("lpad", "stick") == "touch") "touch" else "stick"
            rm = j.optBoolean("rmouse", false)
        }
        3 -> {
            val o = j.getJSONObject("btns")
            out = defaultLayoutF().map { b ->
                if (o.has(b.label)) {
                    val q = o.getJSONObject(b.label)
                    b.copy(
                        x = q.optDouble("x", b.x.toDouble()).toFloat().coerceIn(0f, 1f),
                        y = q.optDouble("y", b.y.toDouble()).toFloat().coerceIn(0f, 1f),
                        size = q.optDouble("s", b.size.toDouble()).toFloat().coerceIn(0.03f, 0.3f),
                    )
                } else b
            }.toMutableList()
        }
        2 -> {
            val o = j.getJSONObject("btns")
            out = defaultLayoutF().map { b ->
                if (o.has(b.label)) {
                    val q = o.getJSONObject(b.label)
                    val sPx = q.optDouble("s", 108.0).toFloat()
                    val wPx = sPx * (if (b.wide) 2.6f else 1f)
                    val cx = (q.optDouble("x", b.x * 1920.0).toFloat() + wPx / 2f) / 1920f
                    val cy = (q.optDouble("y", b.y * 1080.0).toFloat() + sPx / 2f) / 1080f
                    b.copy(
                        x = cx.coerceIn(0f, 1f), y = cy.coerceIn(0f, 1f),
                        size = (sPx / 1080f).coerceIn(0.03f, 0.3f),
                    )
                } else b
            }.toMutableList()
        }
        else -> throw IllegalArgumentException("tanınmayan düzen dosyası")
    }
    return ParsedLayout(out, st, rp, lp, rm)
}

fun buildLayoutJson(layout: List<PadBtnF>, sticks: Sticks, rpad: String, lpad: String, sizeMul: Float, rmouse: Boolean = false): JSONObject {
    val btns = JSONObject()
    layout.forEach {
        btns.put(
            it.label, JSONObject()
                .put("x", it.x.toDouble()).put("y", it.y.toDouble())
                .put("s", (it.size * sizeMul).toDouble())
                .put("out", it.out).put("vis", it.vis).put("t", it.txt)
                .put("ar", it.ratio.toDouble()),
        )
    }
    val st = JSONObject()
        .put("lx", sticks.lx.toDouble()).put("ly", sticks.ly.toDouble()).put("ld", sticks.ld.toDouble())
        .put("rx", sticks.rx.toDouble()).put("ry", sticks.ry.toDouble()).put("rd", sticks.rd.toDouble())
        .put("la", sticks.la.toDouble()).put("ra", sticks.ra.toDouble())
    return JSONObject().put("v", 6).put("btns", btns).put("sticks", st)
        .put("rpad", rpad).put("lpad", lpad).put("rmouse", rmouse)
}
