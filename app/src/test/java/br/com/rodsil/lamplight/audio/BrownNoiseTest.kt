package br.com.rodsil.lamplight.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrownNoiseTest {
  private val loop = brownNoiseLoop()

  @Test
  fun `loop seam is no bigger than a step inside the loop`() {
    val largestStep = (1 until loop.size).maxOf { abs(loop[it] - loop[it - 1]) }
    val seam = abs(loop.first() - loop.last())

    assertTrue("seam $seam > largest step $largestStep", seam <= largestStep)
  }

  @Test
  fun `stays below full scale so it never clips`() {
    val peak = loop.maxOf { abs(it.toInt()) }

    assertTrue("peak $peak", peak in 1 until Short.MAX_VALUE)
  }

  @Test
  fun `renders the same noise every time`() {
    assertTrue(loop.contentEquals(brownNoiseLoop()))
  }

  @Test
  fun `wraps samples in a mono 16 bit WAV header`() {
    val samples = shortArrayOf(1, -1, 300)
    val wav = ByteBuffer.wrap(wavBytes(samples, 22050)).order(ByteOrder.LITTLE_ENDIAN)

    assertEquals("RIFF", String(wav.array(), 0, 4))
    assertEquals("WAVE", String(wav.array(), 8, 4))
    assertEquals(1, wav.getShort(22).toInt())
    assertEquals(22050, wav.getInt(24))
    assertEquals(6, wav.getInt(40))
    assertEquals(300, wav.getShort(48).toInt())
  }
}
