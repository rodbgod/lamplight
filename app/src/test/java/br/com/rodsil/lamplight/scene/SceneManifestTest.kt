package br.com.rodsil.lamplight.scene

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneManifestTest {
  @Test
  fun `shipped manifest is valid and every asset exists`() {
    val manifest = parseSceneManifest(File("src/main/assets/scenes.json").readText())

    assertEquals(listOf("cabin", "rainy_window", "library", "tent"), manifest.scenes.map { it.id })
    manifest.sounds.mapNotNull { it.asset }.forEach { assertTrue("Missing asset $it", File("src/main/assets/$it").exists()) }
  }

  @Test
  fun `rejects a scene that references an unknown sound`() {
    val error = assertThrows(IllegalArgumentException::class.java) { parseSceneManifest(manifest(sceneLayers = """["missing"]""")) }
    assertEquals("Unknown sound 'missing' in scene 'cabin'", error.message)
  }

  @Test
  fun `rejects a sound with both asset and synth`() {
    val sounds = """[{ "id": "fire", "title": "Fire", "asset": "fire.wav", "synth": "BROWN_NOISE" }]"""
    assertThrows(IllegalArgumentException::class.java) { parseSceneManifest(manifest(sounds = sounds)) }
  }

  @Test
  fun `rejects a sound with neither asset nor synth`() {
    val sounds = """[{ "id": "fire", "title": "Fire" }]"""
    assertThrows(IllegalArgumentException::class.java) { parseSceneManifest(manifest(sounds = sounds)) }
  }

  @Test
  fun `rejects a scene layer that repeats a global layer`() {
    val sounds =
      """[{ "id": "fire", "title": "Fire", "asset": "fire.wav" }, { "id": "brown_noise", "title": "Brown noise", "synth": "BROWN_NOISE" }]"""
    val manifest = manifest(sounds = sounds, sceneLayers = """["brown_noise"]""", globalLayers = """["brown_noise"]""")
    assertThrows(IllegalArgumentException::class.java) { parseSceneManifest(manifest) }
  }

  @Test
  fun `rejects unknown keys so typos in the manifest fail loudly`() {
    val sounds = """[{ "id": "fire", "title": "Fire", "aset": "fire.wav" }]"""
    assertThrows(IllegalArgumentException::class.java) { parseSceneManifest(manifest(sounds = sounds)) }
  }
}

private fun manifest(
  sounds: String = """[{ "id": "fire", "title": "Fire", "asset": "fire.wav" }]""",
  sceneLayers: String = "[]",
  globalLayers: String = "[]",
) = """{ "sounds": $sounds, "globalLayers": $globalLayers, "scenes": [{ "id": "cabin", "title": "Cabin", "base": "fire", "layers": $sceneLayers }] }"""
