package br.com.rodsil.lamplight.timer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
private class TimerHarness {
  val scope = TestScope()
  var fade = 1f
  var finished = false
  val timer = SleepTimer(scope, now = { scope.testScheduler.currentTime }, setFade = { fade = it }, onFinish = { finished = true })

  fun advanceBy(ms: Long) {
    scope.advanceTimeBy(ms)
    scope.runCurrent()
  }
}

class SleepTimerTest {
  class Countdown {
    @Test
    fun `keeps full volume until the final minute`() =
      with(TimerHarness()) {
        timer.start(90_000)
        advanceBy(29_000)

        assertEquals(1f, fade)
        assertEquals(61_000L, timer.remainingMs.value)
      }

    @Test
    fun `fades linearly over the final minute`() =
      with(TimerHarness()) {
        timer.start(90_000)
        advanceBy(60_000)

        assertEquals(0.5f, fade, 0.01f)
        assertFalse(finished)
      }

    @Test
    fun `pauses the mix at the end and restores volume for next time`() =
      with(TimerHarness()) {
        timer.start(90_000)
        advanceBy(90_000)

        assertTrue(finished)
        assertEquals(1f, fade)
        assertNull(timer.remainingMs.value)
        assertNull(timer.endsAt.value)
      }
  }

  class Cancelling {
    @Test
    fun `mid fade restores full volume and never pauses`() =
      with(TimerHarness()) {
        timer.start(60_000)
        advanceBy(30_000)
        timer.cancel()
        advanceBy(60_000)

        assertEquals(1f, fade)
        assertFalse(finished)
      }

    @Test
    fun `starting a new timer replaces the running one`() =
      with(TimerHarness()) {
        timer.start(60_000)
        timer.start(120_000)
        advanceBy(60_000)

        assertFalse(finished)
        assertEquals(120_000L, timer.endsAt.value)
      }
  }

  class Resuming {
    @Test
    fun `continues toward the saved end instead of restarting the duration`() =
      with(TimerHarness()) {
        advanceBy(10_000)
        timer.resume(endsAt = 40_000)
        advanceBy(30_000)

        assertTrue(finished)
      }
  }

  class Formatting {
    @Test fun `shows minutes and seconds under an hour`() = assertEquals("42:10", formatRemaining(2_530_000))

    @Test fun `shows hours above an hour`() = assertEquals("1:05:03", formatRemaining(3_903_000))

    @Test fun `rounds partial seconds up so it never shows zero while running`() = assertEquals("0:01", formatRemaining(1))

    @Test fun `counts a partial minute as a whole minute left`() = assertEquals(2, minutesLeft(60_001))
  }

  class CustomMinutes {
    @Test fun `accepts whole minutes in range`() = assertEquals(25, parseCustomMinutes(" 25 "))

    @Test
    fun `rejects zero, too long, and non numbers`() {
      assertNull(parseCustomMinutes("0"))
      assertNull(parseCustomMinutes("${CUSTOM_TIMER_MAX_MINUTES + 1}"))
      assertNull(parseCustomMinutes("abc"))
    }
  }
}
