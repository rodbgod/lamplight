package br.com.rodsil.lamplight.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Sends events to Firebase. Silent when the build has no google-services.json, so local builds work. */
@Singleton
class Analytics @Inject constructor(@ApplicationContext context: Context) {
  private val firebase: FirebaseAnalytics? = FirebaseApp.initializeApp(context)?.let { FirebaseAnalytics.getInstance(context) }

  fun log(event: AnalyticsEvent) {
    firebase?.logEvent(event.name, event.toBundle())
  }
}

// Firebase reports numbers and text differently, so numbers stay numbers.
private fun AnalyticsEvent.toBundle() =
  Bundle().apply {
    params.forEach { (key, value) -> if (value is Long) putLong(key, value) else putString(key, value.toString()) }
  }
