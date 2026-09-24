package br.com.rodsil.lamplight

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import br.com.rodsil.lamplight.ui.mixer.MixerScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Mixer)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider = entryProvider { entry<Mixer> { MixerScreen(modifier = Modifier.safeDrawingPadding().padding(16.dp)) } },
  )
}
