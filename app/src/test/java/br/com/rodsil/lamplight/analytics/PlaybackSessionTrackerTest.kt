package br.com.rodsil.lamplight.analytics

import br.com.rodsil.lamplight.audio.MixerState
import br.com.rodsil.lamplight.audio.StopReason
import br.com.rodsil.lamplight.audio.enteringScene
import br.com.rodsil.lamplight.audio.withLayerEnabled
import br.com.rodsil.lamplight.audio.withLayerVolume
import br.com.rodsil.lamplight.audio.withPlaying
import br.com.rodsil.lamplight.scene.Scene
import org.junit.Assert.assertEquals
import org.junit.Test

private val CABIN = Scene(id = "cabin", title = "Cabin", base = "fire", layers = listOf("wind"))
private val TENT = Scene(id = "tent", title = "Tent", base = "forest", layers = listOf("crickets"))

class PlaybackSessionTrackerTest {
  private var clock = 0L
  private val tracker = PlaybackSessionTracker(now = { clock })
  private val idle = MixerState()
  private val cabin = idle.enteringScene(CABIN, emptyList())

  class Scenes {
    private val subject = PlaybackSessionTrackerTest()

    @Test
    fun `logs the scene entered`() = with(subject) {
      assertEquals(sceneEntered("cabin"), tracker.onChange(idle, cabin).first())
    }

    @Test
    fun `logs a switch to another scene without layer noise`() = with(subject) {
      tracker.onChange(idle, cabin)
      val tent = cabin.enteringScene(TENT, emptyList())

      assertEquals(listOf(sceneEntered("tent")), tracker.onChange(cabin, tent))
    }
  }

  class Layers {
    private val subject = PlaybackSessionTrackerTest()

    @Test
    fun `logs a layer turned on or off`() = with(subject) {
      val withWind = cabin.withLayerEnabled("wind", true)

      assertEquals(listOf(layerToggled("wind", true)), tracker.onChange(cabin, withWind))
      assertEquals(listOf(layerToggled("wind", false)), tracker.onChange(withWind, cabin))
    }

    @Test
    fun `ignores volume changes`() = with(subject) {
      assertEquals(emptyList<AnalyticsEvent>(), tracker.onChange(cabin, cabin.withLayerVolume("fire", 0.2f)))
    }
  }

  class Sessions {
    private val subject = PlaybackSessionTrackerTest()

    @Test
    fun `logs how long it played and what stopped it`() = with(subject) {
      tracker.onChange(idle, cabin)
      clock = 90_000

      val events = tracker.onChange(cabin, cabin.withPlaying(false, StopReason.TIMER))

      assertEquals(listOf(sessionEnded(StopReason.TIMER, lengthSeconds = 90)), events)
    }

    @Test
    fun `starts a new session on resume`() = with(subject) {
      tracker.onChange(idle, cabin)
      val paused = cabin.withPlaying(false, StopReason.INTERRUPTION)
      tracker.onChange(cabin, paused)
      clock = 100_000
      tracker.onChange(paused, cabin)
      clock = 130_000

      assertEquals(listOf(sessionEnded(StopReason.USER, lengthSeconds = 30)), tracker.onChange(cabin, cabin.withPlaying(false)))
    }
  }
}
