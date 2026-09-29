package br.com.rodsil.lamplight.timer

import android.os.SystemClock
import br.com.rodsil.lamplight.audio.SoundMixer
import br.com.rodsil.lamplight.audio.StopReason
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.MainScope

@Module
@InstallIn(SingletonComponent::class)
object TimerModule {
  // Lives as long as the process, which the foreground service keeps alive while the mix plays.
  @Provides
  @Singleton
  fun sleepTimer(mixer: SoundMixer): SleepTimer =
    SleepTimer(scope = MainScope(), now = SystemClock::elapsedRealtime, setFade = mixer::setFade, onFinish = { mixer.setPlaying(false, StopReason.TIMER) })
}
