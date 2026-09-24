package br.com.rodsil.lamplight.playback

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import br.com.rodsil.lamplight.audio.SoundMixer
import br.com.rodsil.lamplight.scene.SceneManifest
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val ARTIST = "Lamplight"

private val COMMANDS =
  Player.Commands.Builder()
    .addAll(Player.COMMAND_PLAY_PAUSE, Player.COMMAND_STOP, Player.COMMAND_GET_CURRENT_MEDIA_ITEM, Player.COMMAND_GET_METADATA)
    .build()

/**
 * Presents the whole mix to the media session as one endless track named after the scene, so the
 * notification, lock screen, Bluetooth and headset buttons control every layer at once.
 */
@OptIn(UnstableApi::class)
class MixPlayer(private val mixer: SoundMixer, private val manifest: SceneManifest, scope: CoroutineScope) :
  SimpleBasePlayer(Looper.getMainLooper()) {

  init {
    scope.launch { mixer.state.collect { invalidateState() } }
  }

  override fun getState(): State {
    val mix = mixer.state.value
    val state = State.Builder().setAvailableCommands(COMMANDS).setPlayWhenReady(mix.isPlaying, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
    val sceneId = mix.sceneId ?: return state.setPlaybackState(STATE_IDLE).build()

    val metadata = MediaMetadata.Builder().setTitle(manifest.scene(sceneId).title).setArtist(ARTIST).build()
    val item = MediaItem.Builder().setMediaId(sceneId).setMediaMetadata(metadata).build()
    val playlist = listOf(MediaItemData.Builder(sceneId).setMediaItem(item).setDurationUs(C.TIME_UNSET).setIsSeekable(false).build())
    return state.setPlaylist(playlist).setPlaybackState(STATE_READY).build()
  }

  override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
    mixer.setPlaying(playWhenReady)
    return Futures.immediateVoidFuture()
  }

  override fun handleStop(): ListenableFuture<*> {
    mixer.setPlaying(false)
    return Futures.immediateVoidFuture()
  }
}
