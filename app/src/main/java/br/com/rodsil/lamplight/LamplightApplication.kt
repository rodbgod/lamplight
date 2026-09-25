package br.com.rodsil.lamplight

import android.app.Application
import br.com.rodsil.lamplight.session.SessionStore
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.MainScope

@HiltAndroidApp
class LamplightApplication : Application() {
  @Inject lateinit var sessionStore: SessionStore

  override fun onCreate() {
    super.onCreate()
    sessionStore.recordIn(MainScope())
  }
}
