package br.com.rodsil.lamplight.audio

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import br.com.rodsil.lamplight.scene.SceneManifest
import br.com.rodsil.lamplight.scene.Sound
import br.com.rodsil.lamplight.scene.Synth
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

// Audio focus is off per player: several players each requesting focus would fight each other.
// The foreground service (M2) owns focus for the whole mix.
private val AMBIENCE_ATTRIBUTES = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build()

/**
 * Plays a scene as a stack of looping layers, one ExoPlayer per audible layer. Players exist only
 * while their layer is audible, so a paused mix or a disabled layer holds no decoder. Main thread only.
 */
@Singleton
class SoundMixer @Inject constructor(@ApplicationContext private val context: Context, private val manifest: SceneManifest) {
  private val players = mutableMapOf<String, ExoPlayer>()
  private val synthWavs = mutableMapOf<Synth, ByteArray>()
  private val mutableState = MutableStateFlow(MixerState())
  val state: StateFlow<MixerState> = mutableState.asStateFlow()

  fun enterScene(sceneId: String) = update { it.enteringScene(manifest.scene(sceneId), manifest.globalLayers) }

  fun setLayerEnabled(soundId: String, enabled: Boolean) = update { it.withLayerEnabled(soundId, enabled) }

  fun setLayerVolume(soundId: String, volume: Float) = update { it.withLayerVolume(soundId, volume) }

  fun setMasterVolume(volume: Float) = update { it.withMasterVolume(volume) }

  fun setPlaying(playing: Boolean) = update { it.withPlaying(playing) }

  private fun update(transform: (MixerState) -> MixerState) = sync(mutableState.updateAndGet(transform))

  private fun sync(state: MixerState) {
    val audible = state.audibleLayers
    (players.keys - audible).forEach { players.remove(it)?.release() }
    audible.forEach { soundId -> players.getOrPut(soundId) { startPlayer(manifest.sound(soundId)) }.volume = state.effectiveVolume(soundId) }
  }

  @OptIn(UnstableApi::class)
  private fun startPlayer(sound: Sound): ExoPlayer =
    ExoPlayer.Builder(context).setAudioAttributes(AMBIENCE_ATTRIBUTES, false).build().apply {
      repeatMode = Player.REPEAT_MODE_ONE
      val synth = sound.synth
      if (synth == null) {
        setMediaItem(MediaItem.fromUri("asset:///${sound.asset}"))
      } else {
        val wav = synthWavs.getOrPut(synth) { renderSynth(synth) }
        val source = ProgressiveMediaSource.Factory(DataSource.Factory { ByteArrayDataSource(wav) })
        setMediaSource(source.createMediaSource(MediaItem.fromUri("synth:///${sound.id}")))
      }
      prepare()
      play()
    }
}

// ponytail: rendered on the main thread the first time (~30 ms); move to a background dispatcher if it shows in traces.
private fun renderSynth(synth: Synth): ByteArray =
  when (synth) {
    Synth.BROWN_NOISE -> wavBytes(brownNoiseLoop(), BROWN_NOISE_SAMPLE_RATE)
  }
