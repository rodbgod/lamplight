package br.com.rodsil.lamplight.scene

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val SCENE_MANIFEST_ASSET = "scenes.json"

@Module
@InstallIn(SingletonComponent::class)
object SceneModule {
  @Provides
  @Singleton
  fun sceneManifest(@ApplicationContext context: Context): SceneManifest =
    parseSceneManifest(context.assets.open(SCENE_MANIFEST_ASSET).bufferedReader().use { it.readText() })
}
