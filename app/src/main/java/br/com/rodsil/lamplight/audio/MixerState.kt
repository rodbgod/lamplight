package br.com.rodsil.lamplight.audio

import br.com.rodsil.lamplight.scene.Scene

const val BASE_VOLUME = 1f
const val LAYER_DEFAULT_VOLUME = 0.5f
const val DEFAULT_MASTER_VOLUME = 1f

data class LayerState(val enabled: Boolean, val volume: Float)

/** [layers] keeps manifest order: base first, then scene layers, then global layers. */
data class MixerState(
  val sceneId: String? = null,
  val isPlaying: Boolean = false,
  val masterVolume: Float = DEFAULT_MASTER_VOLUME,
  val layers: Map<String, LayerState> = emptyMap(),
) {
  val audibleLayers: Set<String>
    get() = if (isPlaying) layers.filterValues { it.enabled }.keys else emptySet()

  fun effectiveVolume(soundId: String): Float = (layers[soundId]?.volume ?: 0f) * masterVolume
}

fun MixerState.enteringScene(scene: Scene, globalLayers: List<String>): MixerState {
  if (scene.id == sceneId) return copy(isPlaying = true)

  val layers = buildMap {
    put(scene.base, LayerState(enabled = true, volume = BASE_VOLUME))
    (scene.layers + globalLayers).forEach { put(it, LayerState(enabled = false, volume = LAYER_DEFAULT_VOLUME)) }
  }
  return copy(sceneId = scene.id, isPlaying = true, layers = layers)
}

fun MixerState.withLayerEnabled(soundId: String, enabled: Boolean): MixerState = updatingLayer(soundId) { it.copy(enabled = enabled) }

fun MixerState.withLayerVolume(soundId: String, volume: Float): MixerState =
  updatingLayer(soundId) { it.copy(volume = volume.coerceIn(0f, 1f)) }

fun MixerState.withMasterVolume(volume: Float): MixerState = copy(masterVolume = volume.coerceIn(0f, 1f))

fun MixerState.withPlaying(playing: Boolean): MixerState = copy(isPlaying = playing && sceneId != null)

private fun MixerState.updatingLayer(soundId: String, transform: (LayerState) -> LayerState): MixerState {
  val layer = layers[soundId] ?: return this
  return copy(layers = layers + (soundId to transform(layer)))
}
