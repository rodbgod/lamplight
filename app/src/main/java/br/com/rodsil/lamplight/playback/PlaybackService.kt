package br.com.rodsil.lamplight.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import br.com.rodsil.lamplight.MainActivity
import br.com.rodsil.lamplight.R
import br.com.rodsil.lamplight.audio.SoundMixer
import br.com.rodsil.lamplight.scene.SceneManifest
import br.com.rodsil.lamplight.timer.SleepTimer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 * Keeps the mix alive with the screen off and after the app is swiped away. Media3 promotes this
 * service to the foreground, with the media notification, whenever the mix is playing.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
  @Inject lateinit var mixer: SoundMixer
  @Inject lateinit var manifest: SceneManifest
  @Inject lateinit var timer: SleepTimer

  private val scope = MainScope()
  private var session: MediaSession? = null

  override fun onCreate() {
    super.onCreate()
    val openApp = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
    val player = MixPlayer(mixer, timer, manifest, ::notificationSubtitle, scope)
    session = MediaSession.Builder(this, player).setSessionActivity(openApp).build()
  }

  private fun notificationSubtitle(minutesLeft: Int?): String =
    if (minutesLeft == null) getString(R.string.app_name) else resources.getQuantityString(R.plurals.minutes_left, minutesLeft, minutesLeft)

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

  override fun onDestroy() {
    session?.run {
      player.release()
      release()
    }
    session = null
    scope.cancel()
    super.onDestroy()
  }
}
