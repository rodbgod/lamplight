package br.com.rodsil.lamplight.ui.mixer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.rodsil.lamplight.R
import br.com.rodsil.lamplight.audio.LayerState
import br.com.rodsil.lamplight.audio.MixerState
import br.com.rodsil.lamplight.scene.Scene

/** Functional mixer for M1. The illustrated scene screen (M3) replaces this layout, not the view model. */
@Composable
fun MixerScreen(modifier: Modifier = Modifier, viewModel: MixerViewModel = hiltViewModel()) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  MixerContent(
    state = state,
    scenes = viewModel.scenes,
    soundTitles = viewModel.soundTitles,
    onSceneClick = viewModel::enterScene,
    onPlayingChange = viewModel::setPlaying,
    onMasterVolumeChange = viewModel::setMasterVolume,
    onLayerEnabledChange = viewModel::setLayerEnabled,
    onLayerVolumeChange = viewModel::setLayerVolume,
    modifier = modifier,
  )
}

@Composable
private fun MixerContent(
  state: MixerState,
  scenes: List<Scene>,
  soundTitles: Map<String, String>,
  onSceneClick: (String) -> Unit,
  onPlayingChange: (Boolean) -> Unit,
  onMasterVolumeChange: (Float) -> Unit,
  onLayerEnabledChange: (String, Boolean) -> Unit,
  onLayerVolumeChange: (String, Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
    Text(stringResource(R.string.pick_a_scene), style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      scenes.forEach { scene ->
        FilterChip(selected = scene.id == state.sceneId, onClick = { onSceneClick(scene.id) }, label = { Text(scene.title) })
      }
    }
    if (state.sceneId != null) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(onClick = { onPlayingChange(!state.isPlaying) }) {
          Text(stringResource(if (state.isPlaying) R.string.pause else R.string.play))
        }
        VolumeSlider(state.masterVolume, onMasterVolumeChange, stringResource(R.string.master_volume), Modifier.weight(1f))
      }
      state.layers.forEach { (soundId, layer) ->
        LayerControls(
          title = soundTitles.getValue(soundId),
          layer = layer,
          onEnabledChange = { onLayerEnabledChange(soundId, it) },
          onVolumeChange = { onLayerVolumeChange(soundId, it) },
        )
      }
    }
  }
}

@Composable
private fun LayerControls(title: String, layer: LayerState, onEnabledChange: (Boolean) -> Unit, onVolumeChange: (Float) -> Unit) {
  Column {
    Row(
      Modifier.fillMaxWidth()
        .minimumInteractiveComponentSize()
        .toggleable(value = layer.enabled, role = Role.Switch, onValueChange = onEnabledChange),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
      Switch(checked = layer.enabled, onCheckedChange = null)
    }
    VolumeSlider(layer.volume, onVolumeChange, stringResource(R.string.layer_volume, title), enabled = layer.enabled)
  }
}

@Composable
private fun VolumeSlider(volume: Float, onVolumeChange: (Float) -> Unit, label: String, modifier: Modifier = Modifier, enabled: Boolean = true) {
  Slider(value = volume, onValueChange = onVolumeChange, enabled = enabled, modifier = modifier.semantics { contentDescription = label })
}
