package br.com.rodsil.lamplight.ui.help

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import br.com.rodsil.lamplight.R

private const val DONT_KILL_MY_APP_URL = "https://dontkillmyapp.com/"

/** Some manufacturers kill foreground services at night. This screen explains how to exempt Lamplight. */
@Composable
fun PlaybackHelpScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  PlaybackHelpContent(
    manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
    onOpenBatterySettings = { openBatterySettings(context) },
    onOpenManufacturerGuide = { openManufacturerGuide(context) },
    modifier = modifier,
  )
}

@Composable
private fun PlaybackHelpContent(
  manufacturer: String,
  onOpenBatterySettings: () -> Unit,
  onOpenManufacturerGuide: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(stringResource(R.string.help_title), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(R.string.help_why), style = MaterialTheme.typography.bodyLarge)
    Text(stringResource(R.string.help_battery), style = MaterialTheme.typography.bodyLarge)
    Button(onClick = onOpenBatterySettings) { Text(stringResource(R.string.help_open_battery_settings)) }
    Text(stringResource(R.string.help_manufacturer, manufacturer), style = MaterialTheme.typography.bodyLarge)
    OutlinedButton(onClick = onOpenManufacturerGuide) { Text(stringResource(R.string.help_open_manufacturer_guide, manufacturer)) }
  }
}

private fun openBatterySettings(context: Context) {
  try {
    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
  } catch (_: ActivityNotFoundException) {
    context.startActivity(Intent(Settings.ACTION_SETTINGS))
  }
}

private fun openManufacturerGuide(context: Context) {
  context.startActivity(Intent(Intent.ACTION_VIEW, "$DONT_KILL_MY_APP_URL${Build.MANUFACTURER.lowercase()}".toUri()))
}
