package br.com.rodsil.lamplight.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
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

private val AMBIENCE_ATTRIBUTES = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build()

/**
 * Plays a scene as a stack of looping layers, one ExoPlayer per audible layer. Players exist only
 * while their layer is audible, so a paused mix holds no decoder. Main thread only.
 *
 * The mixer owns audio focus for the whole mix, because several players each requesting focus
 * would fight each other. Ducking is left to the system, which ducks media on its own since API 26.
 */
@Singleton
class SoundMixer @Inject constructor(@param:ApplicationContext private val context: Context, private val manifest: SceneManifest) {
  private val players = mutableMapOf<String, ExoPlayer>()
  private val synthWavs = mutableMapOf<Synth, ByteArray>()
  private val mutableState = MutableStateFlow(MixerState())
  val state: StateFlow<MixerState> = mutableState.asStateFlow()

  private val audioManager = context.getSystemService(AudioManager::class.java)
  private val focusRequest =
    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
      .setAudioAttributes(
        android.media.AudioAttributes.Builder()
          .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
          .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
          .build()
      )
      .setOnAudioFocusChangeListener(::onAudioFocusChange)
      .build()
  private var hasFocus = false
  private var focusSuspended = false
  private var resumeOnFocusGain = false
  private val headphonesUnplugged =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) = setPlaying(false)
    }

  fun enterScene(sceneId: String) = userUpdate { it.enteringScene(manifest.scene(sceneId), manifest.globalLayers) }

  fun setLayerEnabled(soundId: String, enabled: Boolean) = update { it.withLayerEnabled(soundId, enabled) }

  fun setLayerVolume(soundId: String, volume: Float) = update { it.withLayerVolume(soundId, volume) }

  fun setMasterVolume(volume: Float) = update { it.withMasterVolume(volume) }

  fun setPlaying(playing: Boolean) = userUpdate { it.withPlaying(playing) }

  fun applyMix(sceneId: String, layers: Map<String, LayerState>, masterVolume: Float) =
    userUpdate { it.applyingMix(manifest.scene(sceneId), manifest.globalLayers, layers, masterVolume) }

  fun setFade(fade: Float) = update { it.withFade(fade) }

  /** A user's play or pause overrides any pending resume after a phone call. */
  private fun userUpdate(transform: (MixerState) -> MixerState) {
    resumeOnFocusGain = false
    update(transform)
  }

  private fun update(transform: (MixerState) -> MixerState) {
    val state = mutableState.updateAndGet(transform)
    if (state.isPlaying && !acquireFocus()) {
      sync(mutableState.updateAndGet { it.withPlaying(false) })
      return
    }
    if (!state.isPlaying && !resumeOnFocusGain) releaseFocus()
    sync(state)
  }

  private fun onAudioFocusChange(focusChange: Int) {
    FOCUS_REACTIONS[focusChange]?.invoke(this)
  }

  private fun pauseUntilFocusReturns() {
    focusSuspended = true
    resumeOnFocusGain = state.value.isPlaying
    update { it.withPlaying(false) }
  }

  private fun pauseForGood() = setPlaying(false)

  private fun resumeIfWaiting() {
    focusSuspended = false
    if (!resumeOnFocusGain) return
    resumeOnFocusGain = false
    update { it.withPlaying(true) }
  }

  /** Asks again while suspended, so pressing play during a call is refused instead of playing over it. */
  private fun acquireFocus(): Boolean {
    if (hasFocus && !focusSuspended) return true
    if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return false
    focusSuspended = false
    if (!hasFocus) {
      ContextCompat.registerReceiver(
        context,
        headphonesUnplugged,
        IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
        ContextCompat.RECEIVER_NOT_EXPORTED,
      )
    }
    hasFocus = true
    return true
  }

  private fun releaseFocus() {
    if (!hasFocus) return
    hasFocus = false
    focusSuspended = false
    audioManager.abandonAudioFocusRequest(focusRequest)
    context.unregisterReceiver(headphonesUnplugged)
  }

  private fun sync(state: MixerState) {
    val audible = state.audibleLayers
    (players.keys - audible).forEach { players.remove(it)?.release() }
    audible.forEach { soundId -> players.getOrPut(soundId) { startPlayer(manifest.sound(soundId)) }.volume = state.effectiveVolume(soundId) }
  }

  @OptIn(UnstableApi::class)
  private fun startPlayer(sound: Sound): ExoPlayer =
    ExoPlayer.Builder(context).setAudioAttributes(AMBIENCE_ATTRIBUTES, false).setWakeMode(C.WAKE_MODE_LOCAL).build().apply {
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

  private companion object {
    // A call is a transient loss: pause and come back after it. Another app taking over for good is a
    // permanent loss: stay paused. Transient duck losses are absent on purpose, the system ducks for us.
    val FOCUS_REACTIONS: Map<Int, SoundMixer.() -> Unit> =
      mapOf(
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT to SoundMixer::pauseUntilFocusReturns,
        AudioManager.AUDIOFOCUS_LOSS to SoundMixer::pauseForGood,
        AudioManager.AUDIOFOCUS_GAIN to SoundMixer::resumeIfWaiting,
      )
  }
}

// ponytail: rendered on the main thread the first time (~30 ms); move to a background dispatcher if it shows in traces.
private fun renderSynth(synth: Synth): ByteArray =
  when (synth) {
    Synth.BROWN_NOISE -> wavBytes(brownNoiseLoop(), BROWN_NOISE_SAMPLE_RATE)
  }
