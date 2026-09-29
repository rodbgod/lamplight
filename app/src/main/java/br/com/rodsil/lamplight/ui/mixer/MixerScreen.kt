package br.com.rodsil.lamplight.ui.mixer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.rodsil.lamplight.R
import br.com.rodsil.lamplight.audio.LayerState
import br.com.rodsil.lamplight.audio.MixerState
import br.com.rodsil.lamplight.mix.FREE_MIX_LIMIT
import br.com.rodsil.lamplight.mix.SaveMixResult
import br.com.rodsil.lamplight.timer.CUSTOM_TIMER_MAX_MINUTES
import br.com.rodsil.lamplight.timer.SLEEP_TIMER_PRESETS_MINUTES
import br.com.rodsil.lamplight.timer.formatRemaining
import br.com.rodsil.lamplight.timer.parseCustomMinutes

private val SAVE_RESULT_MESSAGES = mapOf(SaveMixResult.SAVED to R.string.mix_saved, SaveMixResult.LIMIT_REACHED to R.string.mix_limit_reached)

/** Functional scene controls. The illustrated scene screen (M3) replaces this layout, not the view model. */
@Composable
fun MixerScreen(onBack: () -> Unit, onProClick: () -> Unit, modifier: Modifier = Modifier, viewModel: MixerViewModel = hiltViewModel()) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val timerRemainingMs by viewModel.timerRemainingMs.collectAsStateWithLifecycle()
  val saveResult by viewModel.saveResult.collectAsStateWithLifecycle()
  val sceneId = state.sceneId

  // The back stack outlives the process, the mixer does not: after a restart there is no scene to show.
  if (sceneId == null) {
    TextButton(onClick = onBack, modifier = modifier) { Text(stringResource(R.string.pick_a_scene)) }
    return
  }

  MixerContent(
    sceneTitle = viewModel.sceneTitles.getValue(sceneId),
    state = state,
    soundTitles = viewModel.soundTitles,
    timerRemainingMs = timerRemainingMs,
    saveResult = saveResult,
    onPlayingChange = viewModel::setPlaying,
    onMasterVolumeChange = viewModel::setMasterVolume,
    onLayerEnabledChange = viewModel::setLayerEnabled,
    onLayerVolumeChange = viewModel::setLayerVolume,
    onStartTimer = viewModel::startTimer,
    onCancelTimer = viewModel::cancelTimer,
    onSaveMix = viewModel::saveMix,
    onProClick = onProClick,
    modifier = modifier,
  )
}

@Composable
private fun MixerContent(
  sceneTitle: String,
  state: MixerState,
  soundTitles: Map<String, String>,
  timerRemainingMs: Long?,
  saveResult: SaveMixResult?,
  onPlayingChange: (Boolean) -> Unit,
  onMasterVolumeChange: (Float) -> Unit,
  onLayerEnabledChange: (String, Boolean) -> Unit,
  onLayerVolumeChange: (String, Float) -> Unit,
  onStartTimer: (Int) -> Unit,
  onCancelTimer: () -> Unit,
  onSaveMix: (String) -> Unit,
  onProClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showTimerDialog by rememberSaveable { mutableStateOf(false) }
  var showSaveDialog by rememberSaveable { mutableStateOf(false) }

  Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(sceneTitle, style = MaterialTheme.typography.displaySmall)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      Button(onClick = { onPlayingChange(!state.isPlaying) }) { Text(stringResource(if (state.isPlaying) R.string.pause else R.string.play)) }
      VolumeSlider(state.masterVolume, onMasterVolumeChange, stringResource(R.string.master_volume), Modifier.weight(1f))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      val timerLabel = timerRemainingMs?.let { stringResource(R.string.timer_remaining, formatRemaining(it)) } ?: stringResource(R.string.timer_off)
      Text(timerLabel, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
      OutlinedButton(onClick = { showTimerDialog = true }) { Text(stringResource(R.string.sleep_timer)) }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      OutlinedButton(onClick = { showSaveDialog = true }) { Text(stringResource(R.string.save_mix)) }
      saveResult?.let { Text(stringResource(SAVE_RESULT_MESSAGES.getValue(it), FREE_MIX_LIMIT), style = MaterialTheme.typography.bodyMedium) }
    }
    if (saveResult == SaveMixResult.LIMIT_REACHED) {
      TextButton(onClick = onProClick) { Text(stringResource(R.string.get_pro_for_mixes)) }
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

  if (showTimerDialog) {
    SleepTimerDialog(
      onStart = { minutes ->
        onStartTimer(minutes)
        showTimerDialog = false
      },
      onUntilStopped = {
        onCancelTimer()
        showTimerDialog = false
      },
      onDismiss = { showTimerDialog = false },
    )
  }
  if (showSaveDialog) {
    SaveMixDialog(
      defaultName = sceneTitle,
      onSave = { name ->
        onSaveMix(name)
        showSaveDialog = false
      },
      onDismiss = { showSaveDialog = false },
    )
  }
}

@Composable
private fun SleepTimerDialog(onStart: (Int) -> Unit, onUntilStopped: () -> Unit, onDismiss: () -> Unit) {
  var customMinutes by rememberSaveable { mutableStateOf("") }
  val parsedMinutes = parseCustomMinutes(customMinutes)
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.sleep_timer)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          SLEEP_TIMER_PRESETS_MINUTES.forEach { minutes ->
            OutlinedButton(onClick = { onStart(minutes) }) { Text(stringResource(R.string.timer_minutes, minutes)) }
          }
        }
        OutlinedTextField(
          value = customMinutes,
          onValueChange = { customMinutes = it },
          label = { Text(stringResource(R.string.timer_custom_minutes, CUSTOM_TIMER_MAX_MINUTES)) },
          isError = customMinutes.isNotBlank() && parsedMinutes == null,
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        TextButton(onClick = onUntilStopped) { Text(stringResource(R.string.timer_until_stopped)) }
      }
    },
    confirmButton = {
      TextButton(onClick = { parsedMinutes?.let(onStart) }, enabled = parsedMinutes != null) { Text(stringResource(R.string.timer_start)) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
  )
}

@Composable
private fun SaveMixDialog(defaultName: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
  var name by rememberSaveable { mutableStateOf(defaultName) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.save_mix)) },
    text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.mix_name)) }, singleLine = true) },
    confirmButton = { TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
  )
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
