package br.com.rodsil.lamplight.analytics

import br.com.rodsil.lamplight.audio.MixerState
import br.com.rodsil.lamplight.audio.StopReason

private const val MS_PER_SECOND = 1_000L

/**
 * Turns mixer state changes into analytics events. A session is one uninterrupted stretch of
 * playing, measured on [now], a monotonic clock.
 */
class PlaybackSessionTracker(private val now: () -> Long) {
  private var playingSince: Long? = null

  fun onChange(previous: MixerState, next: MixerState): List<AnalyticsEvent> = buildList {
    val sceneId = next.sceneId
    if (sceneId != null && sceneId != previous.sceneId) add(sceneEntered(sceneId))
    if (sceneId == previous.sceneId) addAll(toggledLayers(previous, next))

    if (!previous.isPlaying && next.isPlaying) playingSince = now()
    val startedAt = playingSince
    if (previous.isPlaying && !next.isPlaying && startedAt != null) {
      add(sessionEnded(next.stopReason ?: StopReason.USER, (now() - startedAt) / MS_PER_SECOND))
      playingSince = null
    }
  }
}

private fun toggledLayers(previous: MixerState, next: MixerState): List<AnalyticsEvent> =
  next.layers.mapNotNull { (soundId, layer) ->
    val before = previous.layers[soundId] ?: return@mapNotNull null
    if (before.enabled == layer.enabled) null else layerToggled(soundId, layer.enabled)
  }
