package br.com.rodsil.lamplight.ui.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.rodsil.lamplight.R
import br.com.rodsil.lamplight.mix.SavedMix
import br.com.rodsil.lamplight.scene.Scene

@Composable
fun ScenePickerScreen(
  onOpenScene: () -> Unit,
  onHelpClick: () -> Unit,
  onProClick: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ScenePickerViewModel = hiltViewModel(),
) {
  val continueSceneId by viewModel.continueSceneId.collectAsStateWithLifecycle()
  val mixes by viewModel.mixes.collectAsStateWithLifecycle()
  ScenePickerContent(
    continueSceneTitle = continueSceneId?.let(viewModel.sceneTitles::getValue),
    scenes = viewModel.scenes,
    mixes = mixes,
    sceneTitles = viewModel.sceneTitles,
    onContinue = {
      viewModel.continueListening()
      onOpenScene()
    },
    onSceneClick = { sceneId ->
      viewModel.enterScene(sceneId)
      onOpenScene()
    },
    onPlayMix = { mix ->
      viewModel.playMix(mix)
      onOpenScene()
    },
    onDeleteMix = viewModel::deleteMix,
    onHelpClick = onHelpClick,
    onProClick = onProClick,
    modifier = modifier,
  )
}

@Composable
private fun ScenePickerContent(
  continueSceneTitle: String?,
  scenes: List<Scene>,
  mixes: List<SavedMix>,
  sceneTitles: Map<String, String>,
  onContinue: () -> Unit,
  onSceneClick: (String) -> Unit,
  onPlayMix: (SavedMix) -> Unit,
  onDeleteMix: (SavedMix) -> Unit,
  onHelpClick: () -> Unit,
  onProClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
    if (continueSceneTitle != null) {
      Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.continue_scene, continueSceneTitle)) }
    }
    Text(stringResource(R.string.scenes), style = MaterialTheme.typography.titleMedium)
    scenes.forEach { scene ->
      OutlinedCard(onClick = { onSceneClick(scene.id) }, modifier = Modifier.fillMaxWidth()) {
        Text(scene.title, Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge)
      }
    }
    if (mixes.isNotEmpty()) {
      Text(stringResource(R.string.your_mixes), style = MaterialTheme.typography.titleMedium)
      mixes.forEach { mix -> MixRow(mix, sceneTitles.getValue(mix.sceneId), onPlay = { onPlayMix(mix) }, onDelete = { onDeleteMix(mix) }) }
    }
    TextButton(onClick = onProClick) { Text(stringResource(R.string.get_pro)) }
    TextButton(onClick = onHelpClick) { Text(stringResource(R.string.playback_help_link)) }
  }
}

@Composable
private fun MixRow(mix: SavedMix, sceneTitle: String, onPlay: () -> Unit, onDelete: () -> Unit) {
  val playDescription = stringResource(R.string.play_mix, mix.name)
  val deleteDescription = stringResource(R.string.delete_mix, mix.name)
  Row(verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
      Text(mix.name, style = MaterialTheme.typography.bodyLarge)
      Text(sceneTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    TextButton(onClick = onPlay, modifier = Modifier.semantics { contentDescription = playDescription }) { Text(stringResource(R.string.play)) }
    TextButton(onClick = onDelete, modifier = Modifier.semantics { contentDescription = deleteDescription }) { Text(stringResource(R.string.delete)) }
  }
}
