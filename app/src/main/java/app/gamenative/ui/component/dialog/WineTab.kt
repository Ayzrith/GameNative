package app.gamenative.ui.component.dialog

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.gamenative.R
import app.gamenative.ui.component.settings.SettingsListDropdown
import app.gamenative.ui.component.settings.SettingsListDropdownSearchable
import app.gamenative.ui.theme.settingsTileColors
import app.gamenative.ui.theme.settingsTileColorsAlt
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsSwitch
import com.winlator.core.StringUtils

@Composable
fun WineTabContent(state: ContainerConfigState) {
    val config = state.config.value
    val gpuCardsValues = state.gpuCards.values.toList()
    SettingsGroup() {
        SettingsListDropdownSearchable(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.renderer)) },
            subtitle = { Text("Oyunun kendini tanıttığı ekran kartı adı. Değişiklik oyunu yanıltır, dokunmayın.") },
            value = state.gpuNameIndex.value,
            items = state.gpuCards.values.map { it.name },
            onItemSelected = {
                state.gpuNameIndex.value = it
                val cfg = com.winlator.core.KeyValueSet(config.graphicsDriverConfig)
                cfg.put("gpuName", gpuCardsValues[it].deviceId)
                state.config.value = config.copy(
                    videoPciDeviceID = gpuCardsValues[it].deviceId,
                    graphicsDriverConfig = cfg.toString()
                )
            },
        )
        SettingsListDropdownSearchable(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.gpu_name)) },
            subtitle = { Text("Sürücünün kullanacağı ekran kartı modeli.") },
            value = state.gpuNameIndex.value,
            items = state.gpuCards.values.map { it.name },
            onItemSelected = {
                state.gpuNameIndex.value = it
                state.config.value = config.copy(videoPciDeviceID = gpuCardsValues[it].deviceId)
            },
        )
        SettingsListDropdown(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.offscreen_rendering_mode)) },
            subtitle = { Text("Ekran dışı çizim yöntemi. Bozulma varsa fbo dene.") },
            value = state.renderingModeIndex.value,
            items = state.renderingModes,
            onItemSelected = {
                state.renderingModeIndex.value = it
                state.config.value = config.copy(offScreenRenderingMode = state.renderingModes[it].lowercase())
            },
        )
        SettingsListDropdown(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.video_memory_size)) },
            subtitle = { Text("Oyuna gösterilen sahte ekran kartı belleği. Yüksek değer her zaman hızlı demek değil.") },
            value = state.videoMemIndex.value,
            items = state.videoMemSizes,
            onItemSelected = {
                state.videoMemIndex.value = it
                state.config.value = config.copy(videoMemorySize = StringUtils.parseNumber(state.videoMemSizes[it]))
            },
        )
        SettingsSwitch(
            colors = settingsTileColorsAlt(),
            title = { Text(text = stringResource(R.string.enable_csmt)) },
            subtitle = { Text("Çok iş parçacıklı komut akışı. Çoğu oyunda açık kalsın, grafik bozulursa kapat.") },
            state = config.csmt,
            onCheckedChange = { state.config.value = config.copy(csmt = it) },
        )
        SettingsSwitch(
            colors = settingsTileColorsAlt(),
            title = { Text(text = stringResource(R.string.enable_strict_shader_math)) },
            subtitle = { Text("Katı gölgelendirici matematiği. Yavaşlatır, sadece grafik hatası varsa aç.") },
            state = config.strictShaderMath,
            onCheckedChange = { state.config.value = config.copy(strictShaderMath = it) },
        )
        SettingsListDropdown(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.mouse_warp_override)) },
            subtitle = { Text("Oyun faresi ekran kenarına dayanınca imleç davransın mı. Nişan alırken imleç kaçıyorsa değiştir.") },
            value = state.mouseWarpIndex.value,
            items = state.mouseWarps,
            onItemSelected = {
                state.mouseWarpIndex.value = it
                state.config.value = config.copy(mouseWarpOverride = state.mouseWarps[it].lowercase())
            },
        )
    }
}
