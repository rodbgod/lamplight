package br.com.rodsil.lamplight.audio

import br.com.rodsil.lamplight.scene.Scene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val CABIN = Scene(id = "cabin", title = "Cabin", base = "fire", layers = listOf("wind", "rain"))
private val TENT = Scene(id = "tent", title = "Tent", base = "forest", layers = listOf("crickets"))
private val GLOBAL_LAYERS = listOf("brown_noise")

class MixerStateTest {
  class EnteringAScene {
    @Test
    fun `plays only the base ambience`() {
      val state = MixerState().enteringScene(CABIN, GLOBAL_LAYERS)

      assertTrue(state.isPlaying)
      assertEquals(setOf("fire"), state.audibleLayers)
    }

    @Test
    fun `offers scene layers then global layers, in manifest order`() {
      val state = MixerState().enteringScene(CABIN, GLOBAL_LAYERS)

      assertEquals(listOf("fire", "wind", "rain", "brown_noise"), state.layers.keys.toList())
    }

    @Test
    fun `replaces the previous scene's layers but keeps the master volume`() {
      val state = MixerState().withMasterVolume(0.3f).enteringScene(CABIN, GLOBAL_LAYERS).enteringScene(TENT, GLOBAL_LAYERS)

      assertEquals(listOf("forest", "crickets", "brown_noise"), state.layers.keys.toList())
      assertEquals(0.3f, state.masterVolume)
    }

    @Test
    fun `re-entering the current scene resumes it without resetting layers`() {
      val customized = MixerState().enteringScene(CABIN, GLOBAL_LAYERS).withLayerEnabled("rain", true).withPlaying(false)

      val state = customized.enteringScene(CABIN, GLOBAL_LAYERS)

      assertTrue(state.isPlaying)
      assertEquals(setOf("fire", "rain"), state.audibleLayers)
    }
  }

  class Layers {
    private val cabin = MixerState().enteringScene(CABIN, GLOBAL_LAYERS)

    @Test
    fun `enabling a layer makes it audible`() {
      assertEquals(setOf("fire", "brown_noise"), cabin.withLayerEnabled("brown_noise", true).audibleLayers)
    }

    @Test
    fun `effective volume is layer volume times master volume`() {
      val state = cabin.withLayerVolume("fire", 0.5f).withMasterVolume(0.5f)

      assertEquals(0.25f, state.effectiveVolume("fire"))
    }

    @Test
    fun `volumes are clamped between silent and full`() {
      val state = cabin.withLayerVolume("fire", 2f).withMasterVolume(-1f)

      assertEquals(1f, state.layers.getValue("fire").volume)
      assertEquals(0f, state.masterVolume)
    }

    @Test
    fun `ignores layers the scene does not offer`() {
      assertEquals(cabin, cabin.withLayerEnabled("crickets", true))
    }
  }

  class ApplyingAMix {
    @Test
    fun `restores saved layers and master volume`() {
      val saved = mapOf("fire" to LayerState(enabled = true, volume = 0.2f), "rain" to LayerState(enabled = true, volume = 0.9f))

      val state = MixerState().applyingMix(CABIN, GLOBAL_LAYERS, saved, masterVolume = 0.4f)

      assertEquals(setOf("fire", "rain"), state.audibleLayers)
      assertEquals(0.9f, state.layers.getValue("rain").volume)
      assertEquals(0.4f, state.masterVolume)
    }

    @Test
    fun `ignores layers the scene no longer offers and defaults new ones`() {
      val saved = mapOf("fire" to LayerState(enabled = true, volume = 1f), "retired_sound" to LayerState(enabled = true, volume = 1f))

      val state = MixerState().applyingMix(CABIN, GLOBAL_LAYERS, saved, masterVolume = 1f)

      assertEquals(listOf("fire", "wind", "rain", "brown_noise"), state.layers.keys.toList())
      assertEquals(LayerState(enabled = false, volume = LAYER_DEFAULT_VOLUME), state.layers.getValue("wind"))
    }
  }

  class Fading {
    @Test
    fun `scales every layer without touching the master volume`() {
      val state = MixerState().enteringScene(CABIN, GLOBAL_LAYERS).withMasterVolume(0.8f).withFade(0.5f)

      assertEquals(0.4f, state.effectiveVolume("fire"), 0.0001f)
      assertEquals(0.8f, state.masterVolume)
    }
  }

  class Playback {
    @Test
    fun `pausing silences every layer and keeps their settings`() {
      val paused = MixerState().enteringScene(CABIN, GLOBAL_LAYERS).withLayerEnabled("wind", true).withPlaying(false)

      assertEquals(emptySet<String>(), paused.audibleLayers)
      assertTrue(paused.layers.getValue("wind").enabled)
    }

    @Test
    fun `remembers why it stopped and forgets it when playing again`() {
      val stopped = MixerState().enteringScene(CABIN, GLOBAL_LAYERS).withPlaying(false, StopReason.TIMER)

      assertEquals(StopReason.TIMER, stopped.stopReason)
      assertEquals(null, stopped.withPlaying(true).stopReason)
    }

    @Test
    fun `cannot play before a scene is chosen`() {
      assertFalse(MixerState().withPlaying(true).isPlaying)
    }
  }
}
