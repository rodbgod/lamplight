package br.com.rodsil.lamplight.session

import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.rodsil.lamplight.audio.SoundMixer
import br.com.rodsil.lamplight.timer.SleepTimer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private const val SAVE_DEBOUNCE_MS = 1_000L
private val LAST_SESSION_KEY = stringPreferencesKey("last_session")
private val Context.sessionDataStore by preferencesDataStore(name = "session")

@Singleton
class SessionStore @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val mixer: SoundMixer,
  private val timer: SleepTimer,
) {
  val lastSession: Flow<LastSession?> = context.sessionDataStore.data.map { preferences -> preferences[LAST_SESSION_KEY]?.let(::decode) }

  val bootCount: Int
    get() = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)

  /** Saves every change to the mix and timer, debounced so dragging a slider is one write. */
  @OptIn(FlowPreview::class)
  fun recordIn(scope: CoroutineScope) {
    scope.launch {
      combine(mixer.state, timer.endsAt) { mix, timerEndsAt ->
          mix.sceneId?.let { LastSession(it, mix.layers, mix.masterVolume, timerEndsAt, bootCount) }
        }
        .filterNotNull()
        .distinctUntilChanged()
        .debounce(SAVE_DEBOUNCE_MS)
        .collect { session -> context.sessionDataStore.edit { it[LAST_SESSION_KEY] = Json.encodeToString(session) } }
    }
  }
}

// A session saved by an older app version that no longer parses is not worth crashing over.
private fun decode(json: String): LastSession? = runCatching { Json.decodeFromString<LastSession>(json) }.getOrNull()
