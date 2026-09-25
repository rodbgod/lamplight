package br.com.rodsil.lamplight.mix

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "lamplight.db"

@Module
@InstallIn(SingletonComponent::class)
object MixModule {
  @Provides
  @Singleton
  fun mixDatabase(@ApplicationContext context: Context): MixDatabase = Room.databaseBuilder(context, MixDatabase::class.java, DATABASE_NAME).build()

  @Provides fun mixDao(database: MixDatabase): MixDao = database.mixDao()

  @Provides fun mixRepository(dao: MixDao): MixRepository = MixRepository(dao, System::currentTimeMillis)
}
