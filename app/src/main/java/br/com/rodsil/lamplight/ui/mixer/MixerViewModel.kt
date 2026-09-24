package br.com.rodsil.lamplight.ui.mixer

import androidx.lifecycle.ViewModel
import br.com.rodsil.lamplight.audio.MixerState
import br.com.rodsil.lamplight.audio.SoundMixer
import br.com.rodsil.lamplight.scene.Scene
import br.com.rodsil.lamplight.scene.SceneManifest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class MixerViewModel @Inject constructor(private val mixer: SoundMixer, manifest: SceneManifest) : ViewModel() {
  val scenes: List<Scene> = manifest.scenes
  val soundTitles: Map<String, String> = manifest.sounds.associate { it.id to it.title }
  val state: StateFlow<MixerState> = mixer.state

  fun enterScene(sceneId: String) = mixer.enterScene(sceneId)

  fun setPlaying(playing: Boolean) = mixer.setPlaying(playing)

  fun setMasterVolume(volume: Float) = mixer.setMasterVolume(volume)

  fun setLayerEnabled(soundId: String, enabled: Boolean) = mixer.setLayerEnabled(soundId, enabled)

  fun setLayerVolume(soundId: String, volume: Float) = mixer.setLayerVolume(soundId, volume)
}
