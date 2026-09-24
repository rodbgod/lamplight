package br.com.rodsil.lamplight.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

const val BROWN_NOISE_SAMPLE_RATE = 22050
private const val LOOP_SECONDS = 30
private const val CROSSFADE_SECONDS = 2
private const val STEP = 0.02f
private const val LEAK = 0.998f
private const val PEAK = 0.8f
private const val SEED = 7
private const val WAV_HEADER_BYTES = 44
private const val BYTES_PER_SAMPLE = 2

/**
 * A loop of brown noise that repeats without a seam: the loop's head is an equal power crossfade
 * with the audio that follows its tail, so the last sample flows straight into the first.
 */
fun brownNoiseLoop(random: Random = Random(SEED)): ShortArray {
  val loopLength = LOOP_SECONDS * BROWN_NOISE_SAMPLE_RATE
  val fadeLength = CROSSFADE_SECONDS * BROWN_NOISE_SAMPLE_RATE
  val raw = randomWalk(loopLength + fadeLength, random)
  val loudest = raw.maxOf { abs(it) }

  return ShortArray(loopLength) { index ->
    val sample =
      if (index < fadeLength) {
        val weight = index.toFloat() / fadeLength
        raw[index] * sqrt(weight) + raw[loopLength + index] * sqrt(1 - weight)
      } else {
        raw[index]
      }
    (sample / loudest * PEAK * Short.MAX_VALUE).toInt().coerceIn(-Short.MAX_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
  }
}

private fun randomWalk(length: Int, random: Random): FloatArray {
  var value = 0f
  return FloatArray(length) {
    value = (value + STEP * (random.nextFloat() * 2 - 1)) * LEAK
    value
  }
}

/** Mono 16 bit PCM WAV, so ExoPlayer can play generated audio like any bundled file. */
fun wavBytes(samples: ShortArray, sampleRate: Int): ByteArray {
  val dataBytes = samples.size * BYTES_PER_SAMPLE
  return ByteBuffer.allocate(WAV_HEADER_BYTES + dataBytes)
    .order(ByteOrder.LITTLE_ENDIAN)
    .apply {
      put("RIFF".toByteArray())
      putInt(WAV_HEADER_BYTES - 8 + dataBytes)
      put("WAVEfmt ".toByteArray())
      putInt(16)
      putShort(1)
      putShort(1)
      putInt(sampleRate)
      putInt(sampleRate * BYTES_PER_SAMPLE)
      putShort(BYTES_PER_SAMPLE.toShort())
      putShort(16)
      put("data".toByteArray())
      putInt(dataBytes)
      samples.forEach(::putShort)
    }
    .array()
}
