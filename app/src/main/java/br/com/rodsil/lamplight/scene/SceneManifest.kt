package br.com.rodsil.lamplight.scene

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SceneManifest(val sounds: List<Sound>, val globalLayers: List<String>, val scenes: List<Scene>) {
  fun sound(id: String): Sound = sounds.first { it.id == id }

  fun scene(id: String): Scene = scenes.first { it.id == id }
}

/** Exactly one of [asset] (a path inside the APK assets) or [synth] (generated in code) is set. */
@Serializable data class Sound(val id: String, val title: String, val asset: String? = null, val synth: Synth? = null)

enum class Synth {
  BROWN_NOISE
}

@Serializable data class Scene(val id: String, val title: String, val base: String, val layers: List<String>)

fun parseSceneManifest(json: String): SceneManifest = Json.decodeFromString<SceneManifest>(json).also(::validate)

private fun validate(manifest: SceneManifest) {
  val soundIds = manifest.sounds.map { it.id }
  requireUnique("sound", soundIds)
  requireUnique("scene", manifest.scenes.map { it.id })

  manifest.sounds.forEach { sound ->
    require((sound.asset == null) != (sound.synth == null)) { "Sound '${sound.id}' needs exactly one of asset or synth" }
  }
  manifest.globalLayers.forEach { requireKnownSound(it, soundIds, "global layers") }
  manifest.scenes.forEach { scene ->
    val sceneSounds = listOf(scene.base) + scene.layers + manifest.globalLayers
    sceneSounds.forEach { requireKnownSound(it, soundIds, "scene '${scene.id}'") }
    requireUnique("layer in scene '${scene.id}'", sceneSounds)
  }
}

private fun requireUnique(kind: String, ids: List<String>) {
  val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
  require(duplicates.isEmpty()) { "Duplicate $kind ids: $duplicates" }
}

private fun requireKnownSound(id: String, soundIds: List<String>, usedBy: String) {
  require(id in soundIds) { "Unknown sound '$id' in $usedBy" }
}
