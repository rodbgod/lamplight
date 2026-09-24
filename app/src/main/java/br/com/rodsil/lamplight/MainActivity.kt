package br.com.rodsil.lamplight

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import br.com.rodsil.lamplight.playback.PlaybackService
import br.com.rodsil.lamplight.theme.LamplightTheme
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  private var controller: ListenableFuture<MediaController>? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      LamplightTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }

  // Binding creates the playback service while the app is visible, so it is already running and can
  // promote itself to the foreground the moment the mix starts playing.
  override fun onStart() {
    super.onStart()
    controller = MediaController.Builder(this, SessionToken(this, ComponentName(this, PlaybackService::class.java))).buildAsync()
  }

  override fun onStop() {
    controller?.let(MediaController::releaseFuture)
    controller = null
    super.onStop()
  }
}
