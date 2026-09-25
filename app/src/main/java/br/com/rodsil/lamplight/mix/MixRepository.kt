package br.com.rodsil.lamplight.mix

import br.com.rodsil.lamplight.audio.LayerState
import kotlinx.coroutines.flow.Flow

const val FREE_MIX_LIMIT = 3

enum class SaveMixResult {
  SAVED,
  LIMIT_REACHED,
}

// ponytail: the limit applies to everyone until Pro and the rewarded extra slot land in M5.
class MixRepository(private val dao: MixDao, private val now: () -> Long) {
  val mixes: Flow<List<SavedMix>> = dao.all()

  suspend fun save(name: String, sceneId: String, masterVolume: Float, layers: Map<String, LayerState>): SaveMixResult {
    if (dao.count() >= FREE_MIX_LIMIT) return SaveMixResult.LIMIT_REACHED
    dao.insert(SavedMix(name = name.trim(), sceneId = sceneId, masterVolume = masterVolume, layers = layers, createdAt = now()))
    return SaveMixResult.SAVED
  }

  suspend fun delete(mix: SavedMix) = dao.delete(mix)
}
