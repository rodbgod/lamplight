package br.com.rodsil.lamplight

import android.app.Application
import br.com.rodsil.lamplight.analytics.AnalyticsRecorder
import br.com.rodsil.lamplight.pro.ProStore
import br.com.rodsil.lamplight.session.SessionStore
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.MainScope

@HiltAndroidApp
class LamplightApplication : Application() {
  @Inject lateinit var sessionStore: SessionStore
  @Inject lateinit var analyticsRecorder: AnalyticsRecorder
  @Inject lateinit var proStore: ProStore

  override fun onCreate() {
    super.onCreate()
    val scope = MainScope()
    sessionStore.recordIn(scope)
    analyticsRecorder.recordIn(scope)
    proStore.connect()
  }
}
