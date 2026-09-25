package br.com.rodsil.lamplight.timer

import kotlin.math.ceil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

const val FADE_OUT_MS = 60_000L
private const val COUNTDOWN_TICK_MS = 1_000L
private const val FADE_TICK_MS = 250L
private const val MS_PER_MINUTE = 60_000.0

/** 1 until the last minute, then a straight line down to silence, never a hard cut. */
fun fadeFactor(remainingMs: Long): Float = (remainingMs.toFloat() / FADE_OUT_MS).coerceIn(0f, 1f)

fun minutesLeft(remainingMs: Long): Int = ceil(remainingMs / MS_PER_MINUTE).toInt()

/**
 * Counts down on a monotonic clock ([now], elapsed time since boot) so changing the wall clock or
 * the time zone at night cannot shorten or stretch the timer. Fades the mix out over the final
 * minute through [setFade], then calls [onFinish].
 */
class SleepTimer(
  private val scope: CoroutineScope,
  private val now: () -> Long,
  private val setFade: (Float) -> Unit,
  private val onFinish: () -> Unit,
) {
  private var countdown: Job? = null
  private val mutableEndsAt = MutableStateFlow<Long?>(null)
  private val mutableRemainingMs = MutableStateFlow<Long?>(null)

  /** When the timer ends, on the [now] clock. Null means "until I stop it". */
  val endsAt: StateFlow<Long?> = mutableEndsAt.asStateFlow()
  val remainingMs: StateFlow<Long?> = mutableRemainingMs.asStateFlow()

  fun start(durationMs: Long) = runUntil(now() + durationMs)

  fun resume(endsAt: Long) = runUntil(endsAt)

  fun cancel() {
    countdown?.cancel()
    countdown = null
    clear()
  }

  private fun runUntil(endsAt: Long) {
    cancel()
    mutableEndsAt.value = endsAt
    countdown =
      scope.launch {
        var remaining = endsAt - now()
        while (remaining > 0) {
          mutableRemainingMs.value = remaining
          setFade(fadeFactor(remaining))
          delay(nextTickMs(remaining))
          remaining = endsAt - now()
        }
        onFinish()
        clear()
      }
  }

  private fun clear() {
    mutableEndsAt.value = null
    mutableRemainingMs.value = null
    setFade(1f)
  }
}

private fun nextTickMs(remainingMs: Long): Long =
  if (remainingMs > FADE_OUT_MS) minOf(COUNTDOWN_TICK_MS, remainingMs - FADE_OUT_MS) else minOf(FADE_TICK_MS, remainingMs)

val SLEEP_TIMER_PRESETS_MINUTES = listOf(15, 30, 45, 60, 90)
const val CUSTOM_TIMER_MAX_MINUTES = 480

/** "42:10" under an hour, "1:05:03" above. */
fun formatRemaining(remainingMs: Long): String {
  val totalSeconds = (remainingMs + 999) / 1000
  val hours = totalSeconds / 3600
  val minutes = totalSeconds % 3600 / 60
  val seconds = totalSeconds % 60
  return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

/** Minutes typed by the user, or null unless they are a whole number from 1 to [CUSTOM_TIMER_MAX_MINUTES]. */
fun parseCustomMinutes(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 1..CUSTOM_TIMER_MAX_MINUTES }
