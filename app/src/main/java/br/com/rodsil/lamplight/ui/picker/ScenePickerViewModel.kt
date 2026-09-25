package br.com.rodsil.lamplight.ui.picker

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rodsil.lamplight.audio.SoundMixer
import br.com.rodsil.lamplight.mix.MixRepository
import br.com.rodsil.lamplight.mix.SavedMix
import br.com.rodsil.lamplight.scene.Scene
import br.com.rodsil.lamplight.scene.SceneManifest
import br.com.rodsil.lamplight.session.LastSession
import br.com.rodsil.lamplight.session.SessionStore
import br.com.rodsil.lamplight.session.resumableTimerEnd
import br.com.rodsil.lamplight.timer.SleepTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class ScenePickerViewModel @Inject constructor(
  private val mixer: SoundMixer,
  private val timer: SleepTimer,
  private val sessionStore: SessionStore,
  private val mixRepository: MixRepository,
  manifest: SceneManifest,
) : ViewModel() {
  val scenes: List<Scene> = manifest.scenes
  val sceneTitles: Map<String, String> = manifest.scenes.associate { it.id to it.title }
  val mixes: StateFlow<List<SavedMix>> =
    mixRepository.mixes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  private val lastSession: StateFlow<LastSession?> = sessionStore.lastSession.stateIn(viewModelScope, SharingStarted.Eagerly, null)

  /** The scene "Continue" goes back to: the one in the mixer now, or else the last one saved. */
  val continueSceneId: StateFlow<String?> =
    combine(mixer.state, lastSession) { mix, session -> mix.sceneId ?: session?.sceneId }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

  fun enterScene(sceneId: String) = mixer.enterScene(sceneId)

  fun continueListening() {
    if (mixer.state.value.sceneId != null) return mixer.setPlaying(true)
    val session = lastSession.value ?: return
    mixer.applyMix(session.sceneId, session.layers, session.masterVolume)
    session.resumableTimerEnd(sessionStore.bootCount, SystemClock.elapsedRealtime())?.let(timer::resume)
  }

  fun playMix(mix: SavedMix) = mixer.applyMix(mix.sceneId, mix.layers, mix.masterVolume)

  fun deleteMix(mix: SavedMix) {
    viewModelScope.launch { mixRepository.delete(mix) }
  }
}
