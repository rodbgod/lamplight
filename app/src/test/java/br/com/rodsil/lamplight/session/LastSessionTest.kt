package br.com.rodsil.lamplight.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LastSessionTest {
  private val session = LastSession(sceneId = "cabin", layers = emptyMap(), masterVolume = 1f, timerEndsAt = 5_000, bootCount = 3)

  @Test fun `resumes a timer that has not ended yet`() = assertEquals(5_000L, session.resumableTimerEnd(currentBootCount = 3, now = 4_000))

  @Test fun `drops a timer that already ended`() = assertNull(session.resumableTimerEnd(currentBootCount = 3, now = 6_000))

  @Test fun `drops a timer from before a reboot, when the clock restarted`() = assertNull(session.resumableTimerEnd(currentBootCount = 4, now = 1_000))

  @Test fun `has nothing to resume when there was no timer`() = assertNull(session.copy(timerEndsAt = null).resumableTimerEnd(3, 0))
}
