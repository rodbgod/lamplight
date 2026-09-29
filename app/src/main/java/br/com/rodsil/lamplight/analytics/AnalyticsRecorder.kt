package br.com.rodsil.lamplight.analytics

import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.rodsil.lamplight.audio.MixerState
import br.com.rodsil.lamplight.audio.SoundMixer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val MS_PER_SECOND = 1_000L
private val PLAYING_SINCE_KEY = longPreferencesKey("playing_since")
private val Context.playbackDataStore by preferencesDataStore(name = "playback")

/**
 * Logs mixer activity. It also leaves a "playing since" mark while the mix plays and removes it on
 * any normal stop. A mark found at the next start means the process died mid play, which is the
 * OEM background kill PRD section 11 asks to watch by manufacturer.
 */
@Singleton
class AnalyticsRecorder @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val mixer: SoundMixer,
  private val analytics: Analytics,
) {
  fun recordIn(scope: CoroutineScope) {
    scope.launch {
      reportUnexpectedStop()
      val tracker = PlaybackSessionTracker(now = SystemClock::elapsedRealtime)
      var previous = MixerState()
      mixer.state.collect { next ->
        tracker.onChange(previous, next).forEach(analytics::log)
        if (!previous.isPlaying && next.isPlaying) markPlaying()
        if (previous.isPlaying && !next.isPlaying) clearMark()
        previous = next
      }
    }
  }

  private suspend fun reportUnexpectedStop() {
    val playingSince = context.playbackDataStore.data.first()[PLAYING_SINCE_KEY] ?: return
    val lengthSeconds = (System.currentTimeMillis() - playingSince) / MS_PER_SECOND
    analytics.log(playbackStoppedUnexpectedly(Build.MANUFACTURER, Build.MODEL, lengthSeconds))
    analytics.log(sessionEnded(ENDED_UNEXPECTEDLY, lengthSeconds))
    clearMark()
  }

  // Wall clock, unlike the tracker: the mark is read by a later process, possibly after a reboot.
  private suspend fun markPlaying() = context.playbackDataStore.edit { it[PLAYING_SINCE_KEY] = System.currentTimeMillis() }

  private suspend fun clearMark() = context.playbackDataStore.edit { it.remove(PLAYING_SINCE_KEY) }
}
