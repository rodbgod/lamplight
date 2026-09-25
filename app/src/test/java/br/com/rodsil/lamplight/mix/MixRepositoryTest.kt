package br.com.rodsil.lamplight.mix

import br.com.rodsil.lamplight.audio.LayerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private val LAYERS = mapOf("fire" to LayerState(enabled = true, volume = 1f))

class MixRepositoryTest {
  private val dao = FakeMixDao()
  private val repository = MixRepository(dao, now = { 1L })

  @Test
  fun `saves up to the free limit`() = runTest {
    repeat(FREE_MIX_LIMIT) { assertEquals(SaveMixResult.SAVED, repository.save("Mix $it", "cabin", 1f, LAYERS)) }
    assertEquals(FREE_MIX_LIMIT, dao.count())
  }

  @Test
  fun `refuses a mix beyond the free limit`() = runTest {
    repeat(FREE_MIX_LIMIT) { repository.save("Mix $it", "cabin", 1f, LAYERS) }

    assertEquals(SaveMixResult.LIMIT_REACHED, repository.save("One more", "cabin", 1f, LAYERS))
    assertEquals(FREE_MIX_LIMIT, dao.count())
  }

  @Test
  fun `deleting a mix frees a slot`() = runTest {
    repeat(FREE_MIX_LIMIT) { repository.save("Mix $it", "cabin", 1f, LAYERS) }
    repository.delete(dao.mixes.value.first())

    assertEquals(SaveMixResult.SAVED, repository.save("Replacement", "cabin", 1f, LAYERS))
  }

  @Test
  fun `trims the name`() = runTest {
    repository.save("  Rainy night  ", "rainy_window", 1f, LAYERS)
    assertEquals("Rainy night", dao.mixes.value.single().name)
  }
}

private class FakeMixDao : MixDao {
  val mixes = MutableStateFlow(emptyList<SavedMix>())
  private var nextId = 1L

  override fun all(): Flow<List<SavedMix>> = mixes

  override suspend fun count(): Int = mixes.value.size

  override suspend fun insert(mix: SavedMix) {
    mixes.value += mix.copy(id = nextId++)
  }

  override suspend fun delete(mix: SavedMix) {
    mixes.value = mixes.value.filterNot { it.id == mix.id }
  }
}
