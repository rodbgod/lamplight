package br.com.rodsil.lamplight.mix

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import br.com.rodsil.lamplight.audio.LayerState
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

@Entity(tableName = "mixes")
data class SavedMix(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val sceneId: String,
  val masterVolume: Float,
  val layers: Map<String, LayerState>,
  val createdAt: Long,
)

@Dao
interface MixDao {
  @Query("SELECT * FROM mixes ORDER BY createdAt DESC") fun all(): Flow<List<SavedMix>>

  @Query("SELECT COUNT(*) FROM mixes") suspend fun count(): Int

  @Insert suspend fun insert(mix: SavedMix)

  @Delete suspend fun delete(mix: SavedMix)
}

class LayerConverters {
  @TypeConverter fun layersToJson(layers: Map<String, LayerState>): String = Json.encodeToString(layers)

  @TypeConverter fun layersFromJson(json: String): Map<String, LayerState> = Json.decodeFromString(json)
}

@Database(entities = [SavedMix::class], version = 1)
@TypeConverters(LayerConverters::class)
abstract class MixDatabase : RoomDatabase() {
  abstract fun mixDao(): MixDao
}
