package app.gamenative.phonepad

import android.view.KeyEvent
import app.gamenative.PluviaApp

// PhoneGamepad NetClient'ın ekran-yüzeyinin yereli: tuş/stick -> InputControlsView köprüsü.
// Ağ yok; emülatör (XServer/WinHandler) tarafına dokunmaz.
class PhonePadInput {
    @Volatile var lx = 0f
    @Volatile var ly = 0f
    @Volatile var rx = 0f
    @Volatile var ry = 0f
    @Volatile var lt = 0f
    @Volatile var rt = 0f

    @Volatile var volUpBit: Int = Btn.LB
    @Volatile var volDownBit: Int = Btn.LTB
    private var volUpDown = false
    private var volDownDown = false

    // Fare modu birikimi: küçük sürüklemeler kaybolmasın diye piksel biriktirir.
    private var mouseAccX = 0f
    private var mouseAccY = 0f

    fun mouseMove(dxPx: Float, dyPx: Float) {
        mouseAccX += dxPx; mouseAccY += dyPx
        val dx = mouseAccX.toInt(); val dy = mouseAccY.toInt()
        if (dx == 0 && dy == 0) return
        mouseAccX -= dx; mouseAccY -= dy
        try { PluviaApp.inputControlsView?.phonePadMouseMove(dx, dy) } catch (_: Exception) {}
    }

    fun mouseClick(right: Boolean = false) {
        try { PluviaApp.inputControlsView?.phonePadMouseClick(if (right) 1 else 0) } catch (_: Exception) {}
    }

    fun setButton(bit: Int, pressed: Boolean) {
        PluviaApp.inputControlsView?.phonePadButton(phoneBitToBinding(bit), pressed)
    }

    // Birleşik D-Pad: basılı yön kümesinin farkını gönderir (çaprazda iki bit).
    private val dpadHeld = mutableSetOf<Int>()

    fun setDpad(dirs: Set<Int>) {
        if (dirs == dpadHeld) return
        (dpadHeld - dirs).forEach { setButton(it, false) }
        (dirs - dpadHeld).forEach { setButton(it, true) }
        dpadHeld.clear()
        dpadHeld.addAll(dirs)
    }

    fun pushNow() {
        PluviaApp.inputControlsView?.setPhonePadAnalog(lx, ly, rx, ry, lt, rt)
    }

    fun releaseAll() {
        lx = 0f; ly = 0f; rx = 0f; ry = 0f; lt = 0f; rt = 0f
        volUpDown = false; volDownDown = false
        dpadHeld.clear()
        try { PluviaApp.inputControlsView?.phonePadReleaseAll() } catch (_: Exception) {}
    }

    private fun volDown(bit: Int, flag: Boolean): Boolean {
        if (flag) return true
        setButton(bit, true)
        if (bit == Btn.RTB) { rt = 1f; pushNow() }
        if (bit == Btn.LTB) { lt = 1f; pushNow() }
        return true
    }

    private fun volUp(bit: Int) {
        setButton(bit, false)
        if (bit == Btn.RTB) { rt = 0f; pushNow() }
        if (bit == Btn.LTB) { lt = 0f; pushNow() }
    }

    // XServerScreen onKeyEvent'ten çağrılır; true = ses yerine pad tuşu oldu.
    fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP && event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return false
        return when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount > 0) return true
                if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    if (volUpDown) return true
                    volUpDown = true
                    volDown(volUpBit, false)
                } else {
                    if (volDownDown) return true
                    volDownDown = true
                    volDown(volDownBit, false)
                }
                true
            }
            KeyEvent.ACTION_UP -> {
                if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    volUpDown = false
                    volUp(volUpBit)
                } else {
                    volDownDown = false
                    volUp(volDownBit)
                }
                true
            }
            else -> false
        }
    }
}
