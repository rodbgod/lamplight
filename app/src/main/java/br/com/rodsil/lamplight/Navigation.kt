package br.com.rodsil.lamplight

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import br.com.rodsil.lamplight.ui.help.PlaybackHelpScreen
import br.com.rodsil.lamplight.ui.mixer.MixerScreen
import br.com.rodsil.lamplight.ui.picker.ScenePickerScreen

private val SCREEN_PADDING = Modifier.safeDrawingPadding().padding(16.dp)

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(ScenePicker)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<ScenePicker> {
          ScenePickerScreen(
            onOpenScene = { backStack.add(ScenePlayer) },
            onHelpClick = { backStack.add(PlaybackHelp) },
            modifier = SCREEN_PADDING,
          )
        }
        entry<ScenePlayer> { MixerScreen(onBack = { backStack.removeLastOrNull() }, modifier = SCREEN_PADDING) }
        entry<PlaybackHelp> { PlaybackHelpScreen(modifier = SCREEN_PADDING) }
      },
  )
}
