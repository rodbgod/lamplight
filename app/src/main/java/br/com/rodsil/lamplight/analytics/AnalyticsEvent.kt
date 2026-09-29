package br.com.rodsil.lamplight.analytics

import br.com.rodsil.lamplight.audio.StopReason

/** One analytics event. Names and parameters follow PRD section 11. */
data class AnalyticsEvent(val name: String, val params: Map<String, Any> = emptyMap())

/** Ended by the mix being cut off without a pause: the process died while playing. */
const val ENDED_UNEXPECTEDLY = "unexpected"

fun sceneEntered(sceneId: String) = AnalyticsEvent("scene_entered", mapOf("scene" to sceneId))

fun layerToggled(soundId: String, enabled: Boolean) = AnalyticsEvent("layer_toggled", mapOf("layer" to soundId, "enabled" to enabled.toString()))

fun mixSaved(sceneId: String) = AnalyticsEvent("mix_saved", mapOf("scene" to sceneId))

fun timerSet(minutes: Int) = AnalyticsEvent("timer_set", mapOf("minutes" to minutes.toLong()))

/** PRD's session_length and session_ended_by in one event, so every length has its cause. */
fun sessionEnded(endedBy: String, lengthSeconds: Long) =
  AnalyticsEvent("session_ended", mapOf("ended_by" to endedBy, "length_seconds" to lengthSeconds))

fun sessionEnded(reason: StopReason, lengthSeconds: Long) = sessionEnded(reason.name.lowercase(), lengthSeconds)

fun playbackStoppedUnexpectedly(manufacturer: String, model: String, lengthSeconds: Long) =
  AnalyticsEvent(
    "playback_stopped_unexpectedly",
    mapOf("manufacturer" to manufacturer, "model" to model, "length_seconds" to lengthSeconds),
  )

fun paywallViewed() = AnalyticsEvent("paywall_viewed")

fun proPurchased(productId: String) = AnalyticsEvent("pro_purchased", mapOf("product" to productId))
