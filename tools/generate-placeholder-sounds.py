"""Generates short, loopable placeholder WAVs for every ambience in the scene manifest.

The final sounds come from task M0a. Until then, the app needs distinct, recognizable
audio per layer to develop and test the mixer. Each placeholder loops seamlessly by
blending its tail into its head.

Usage: python tools/generate-placeholder-sounds.py
"""

import math
import random
import struct
import wave
from pathlib import Path

SAMPLE_RATE = 22050
DURATION_SECONDS = 10
CROSSFADE_SECONDS = 1
OUTPUT_DIR = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "assets" / "sounds"


def white(rng):
    return rng.uniform(-1, 1)


def lowpassed_noise(rng, count, smoothing):
    value = 0.0
    samples = []
    for _ in range(count):
        value += smoothing * (white(rng) - value)
        samples.append(value)
    return normalize(samples, 0.5)


def brownish_noise(rng, count):
    value = 0.0
    samples = []
    for _ in range(count):
        value = (value + 0.02 * white(rng)) * 0.998
        samples.append(value)
    return normalize(samples, 0.5)


def normalize(samples, peak):
    loudest = max(abs(sample) for sample in samples) or 1.0
    return [sample / loudest * peak for sample in samples]


def slow_swell(count, cycles, depth):
    return [1 - depth + depth * (0.5 + 0.5 * math.sin(2 * math.pi * cycles * index / count)) for index in range(count)]


def bursts(count, starts, length, shape):
    envelope = [0.0] * count
    for start in starts:
        for offset in range(length):
            envelope[(start + offset) % count] = max(envelope[(start + offset) % count], shape(offset / length))
    return envelope


def decay(position):
    return math.exp(-6 * position)


def hump(position):
    return math.sin(math.pi * position)


def multiply(samples, envelope):
    return [sample * gain for sample, gain in zip(samples, envelope)]


def mix(*tracks):
    return [sum(values) for values in zip(*tracks)]


def tone(count, frequency, amplitude):
    return [amplitude * math.sin(2 * math.pi * frequency * index / SAMPLE_RATE) for index in range(count)]


def evenly_spaced(count, every_seconds):
    step = int(every_seconds * SAMPLE_RATE)
    return list(range(0, count, step))


def scattered(rng, count, how_many):
    return sorted(rng.randrange(count) for _ in range(how_many))


def fire(rng, count):
    crackles = multiply([white(rng) for _ in range(count)], bursts(count, scattered(rng, count, 60), 400, decay))
    return mix(multiply(brownish_noise(rng, count), slow_swell(count, 3, 0.3)), multiply(crackles, [0.6] * count))


def rain_glass(rng, count):
    drops = multiply([white(rng) for _ in range(count)], bursts(count, scattered(rng, count, 400), 120, decay))
    return mix(multiply(lowpassed_noise(rng, count, 0.6), [0.4] * count), multiply(drops, [0.5] * count))


def light_rain(rng, count):
    return multiply(lowpassed_noise(rng, count, 0.3), [0.6] * count)


def thunder(rng, count):
    return multiply(brownish_noise(rng, count), bursts(count, [int(count * 0.2), int(count * 0.65)], SAMPLE_RATE * 3, hump))


def wind_outside(rng, count):
    return multiply(lowpassed_noise(rng, count, 0.05), slow_swell(count, 2, 0.8))


def wind_trees(rng, count):
    return multiply(lowpassed_noise(rng, count, 0.25), slow_swell(count, 3, 0.7))


def forest_night(rng, count):
    chirps = multiply(tone(count, 1800, 0.2), bursts(count, scattered(rng, count, 5), 3000, hump))
    return mix(multiply(lowpassed_noise(rng, count, 0.08), [0.5] * count), chirps)


def crickets(rng, count):
    return multiply(tone(count, 4400, 0.4), bursts(count, evenly_spaced(count, 0.25), 1200, hump))


def room_tone(rng, count):
    return mix(tone(count, 60, 0.15), multiply(lowpassed_noise(rng, count, 0.1), [0.15] * count))


def page_turn(rng, count):
    rustle = [white(rng) for _ in range(count)]
    return multiply(rustle, bursts(count, [int(count * 0.1), int(count * 0.55)], SAMPLE_RATE // 2, hump))


def clock(rng, count):
    clicks = [white(rng) for _ in range(count)]
    return multiply(clicks, bursts(count, evenly_spaced(count, 1), 300, decay))


def cat_purr(rng, count):
    purring = [0.5 + 0.5 * math.sin(2 * math.pi * 25 * index / SAMPLE_RATE) for index in range(count)]
    breathing = slow_swell(count, 4, 0.6)
    return multiply(lowpassed_noise(rng, count, 0.1), multiply(purring, breathing))


def footsteps(rng, count):
    thumps = [math.sin(2 * math.pi * 90 * index / SAMPLE_RATE) for index in range(count)]
    return multiply(thumps, bursts(count, [int(count * fraction) for fraction in (0.1, 0.18, 0.26, 0.34)], 2000, decay))


def kettle(rng, count):
    bubbling = multiply([white(rng) for _ in range(count)], bursts(count, scattered(rng, count, 200), 200, decay))
    return mix(multiply(lowpassed_noise(rng, count, 0.4), [0.3] * count), multiply(bubbling, [0.3] * count))


PLACEHOLDERS = {
    "fire": fire,
    "rain_glass": rain_glass,
    "light_rain": light_rain,
    "thunder": thunder,
    "wind_outside": wind_outside,
    "wind_trees": wind_trees,
    "forest_night": forest_night,
    "crickets": crickets,
    "room_tone": room_tone,
    "page_turn": page_turn,
    "clock": clock,
    "cat_purr": cat_purr,
    "footsteps": footsteps,
    "kettle": kettle,
}


def make_loopable(samples):
    fade = CROSSFADE_SECONDS * SAMPLE_RATE
    body, tail = samples[:-fade], samples[-fade:]
    for index in range(fade):
        weight = index / fade
        body[index] = body[index] * weight + tail[index] * (1 - weight)
    return body


def write_wav(path, samples):
    with wave.open(str(path), "wb") as file:
        file.setnchannels(1)
        file.setsampwidth(2)
        file.setframerate(SAMPLE_RATE)
        file.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, sample)) * 32767)) for sample in samples))


def main():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    count = (DURATION_SECONDS + CROSSFADE_SECONDS) * SAMPLE_RATE
    for name, generate in PLACEHOLDERS.items():
        rng = random.Random(name)
        write_wav(OUTPUT_DIR / f"{name}.wav", make_loopable(generate(rng, count)))
        print(f"wrote {name}.wav")


if __name__ == "__main__":
    main()
