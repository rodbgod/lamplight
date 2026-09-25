package br.com.rodsil.lamplight.ui.mixer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rodsil.lamplight.audio.MixerState
import br.com.rodsil.lamplight.audio.SoundMixer
import br.com.rodsil.lamplight.mix.MixRepository
import br.com.rodsil.lamplight.mix.SaveMixResult
import br.com.rodsil.lamplight.scene.SceneManifest
import br.com.rodsil.lamplight.timer.SleepTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val MS_PER_MINUTE = 60_000L

@HiltViewModel
class MixerViewModel @Inject constructor(
  private val mixer: SoundMixer,
  private val timer: SleepTimer,
  private val mixRepository: MixRepository,
  manifest: SceneManifest,
) : ViewModel() {
  val sceneTitles: Map<String, String> = manifest.scenes.associate { it.id to it.title }
  val soundTitles: Map<String, String> = manifest.sounds.associate { it.id to it.title }
  val state: StateFlow<MixerState> = mixer.state
  val timerRemainingMs: StateFlow<Long?> = timer.remainingMs

  private val mutableSaveResult = MutableStateFlow<SaveMixResult?>(null)
  val saveResult: StateFlow<SaveMixResult?> = mutableSaveResult.asStateFlow()

  fun setPlaying(playing: Boolean) = mixer.setPlaying(playing)

  fun setMasterVolume(volume: Float) = mixer.setMasterVolume(volume)

  fun setLayerEnabled(soundId: String, enabled: Boolean) = mixer.setLayerEnabled(soundId, enabled)

  fun setLayerVolume(soundId: String, volume: Float) = mixer.setLayerVolume(soundId, volume)

  fun startTimer(minutes: Int) = timer.start(minutes * MS_PER_MINUTE)

  fun cancelTimer() = timer.cancel()

  fun saveMix(name: String) {
    val mix = mixer.state.value
    val sceneId = mix.sceneId ?: return
    viewModelScope.launch { mutableSaveResult.value = mixRepository.save(name, sceneId, mix.masterVolume, mix.layers) }
  }
}
