package br.com.rodsil.lamplight.session

import br.com.rodsil.lamplight.audio.LayerState
import kotlinx.serialization.Serializable

/**
 * What the user last listened to, so the picker can resume it in one tap. The timer end is on the
 * monotonic clock, which restarts at boot, so [bootCount] tells whether it still means anything.
 */
@Serializable
data class LastSession(
  val sceneId: String,
  val layers: Map<String, LayerState>,
  val masterVolume: Float,
  val timerEndsAt: Long? = null,
  val bootCount: Int,
)

/** The timer end to resume, or null when there was none, it already passed, or the phone rebooted since. */
fun LastSession.resumableTimerEnd(currentBootCount: Int, now: Long): Long? = timerEndsAt?.takeIf { bootCount == currentBootCount && it > now }
