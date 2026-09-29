package br.com.rodsil.lamplight

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object ScenePicker : NavKey

@Serializable data object ScenePlayer : NavKey

@Serializable data object PlaybackHelp : NavKey

@Serializable data object Pro : NavKey
