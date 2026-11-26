package com.colorgrid.keyboard

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Makes the key sounds. Every voice is built from a few simple layers (tonal partials, a filtered noise "click",
 * a low "thump", a second bottom-out hit, optional echo) so no sound files are needed and pitch, tone and key
 * travel can be changed freely. Pure Kotlin (no Android classes).
 */
object VoiceSynth {
    const val SR = 22050

    class Spec(
        /** 0 sine, 1 square, 2 narrow pulse, 3 saw, 4 triangle */
        val shape: Int = 0,
        val freq: Float = 0f,
        /** triples: ratio, amplitude, decay seconds */
        val partials: FloatArray = floatArrayOf(),
        val bend: Float = 1f,
        val bendT: Float = 0.03f,
        val clickHz: Float = 0f,
        val clickQ: Float = 1f,
        val clickAmp: Float = 0f,
        val clickMs: Float = 6f,
        val subHz: Float = 0f,
        val subAmp: Float = 0f,
        val subDecay: Float = 0.08f,
        /** strength of the second "bottom-out" hit, 0 = none */
        val bottom: Float = 0f,
        val secondRatio: Float = 0f,
        val secondDelay: Float = 0f,
        val echo: Boolean = false,
        val crush: Int = 0,
        val len: Float = 0.15f
    )

    val specs: Map<String, Spec> = mapOf(
        "boop" to Spec(freq = 520f, partials = floatArrayOf(1f, 1f, 0.09f, 2f, 0.18f, 0.05f), bend = 1.35f, bendT = 0.03f, len = 0.16f),
        "carbon" to Spec(clickHz = 1200f, clickQ = 0.8f, clickAmp = 0.55f, clickMs = 14f, subHz = 150f, subAmp = 0.8f, subDecay = 0.07f, bottom = 0.5f, len = 0.13f),
        "machina" to Spec(freq = 190f, partials = floatArrayOf(1f, 0.5f, 0.09f), clickHz = 2200f, clickQ = 1.2f, clickAmp = 0.6f, clickMs = 8f, subHz = 120f, subAmp = 1f, subDecay = 0.12f, bottom = 0.9f, len = 0.2f),
        "titanium" to Spec(clickHz = 3000f, clickQ = 1f, clickAmp = 0.35f, clickMs = 6f, subHz = 220f, subAmp = 0.7f, subDecay = 0.08f, bottom = 0.3f, len = 0.12f),
        "crystal" to Spec(freq = 1400f, partials = floatArrayOf(1f, 1f, 0.28f, 2.76f, 0.6f, 0.2f, 5.4f, 0.35f, 0.12f), clickHz = 5000f, clickQ = 1f, clickAmp = 0.25f, clickMs = 4f, len = 0.35f),
        "pulse" to Spec(shape = 2, freq = 330f, partials = floatArrayOf(1f, 1f, 0.07f), len = 0.1f),
        "topre" to Spec(clickHz = 900f, clickQ = 0.7f, clickAmp = 0.4f, clickMs = 18f, subHz = 200f, subAmp = 1f, subDecay = 0.09f, bottom = 0.6f, len = 0.16f),
        "ceramic" to Spec(freq = 2200f, partials = floatArrayOf(1f, 1f, 0.06f, 1.68f, 0.5f, 0.05f, 2.9f, 0.3f, 0.04f), clickHz = 6000f, clickQ = 1f, clickAmp = 0.4f, clickMs = 3f, len = 0.12f),
        "clicky" to Spec(clickHz = 3400f, clickQ = 2f, clickAmp = 1f, clickMs = 5f, subHz = 160f, subAmp = 0.5f, subDecay = 0.05f, bottom = 1f, len = 0.14f),
        "deep" to Spec(freq = 150f, partials = floatArrayOf(1f, 0.6f, 0.1f), clickHz = 1500f, clickQ = 0.9f, clickAmp = 0.5f, clickMs = 10f, subHz = 110f, subAmp = 1f, subDecay = 0.14f, bottom = 0.9f, len = 0.22f),
        "airy" to Spec(freq = 900f, partials = floatArrayOf(1f, 0.3f, 0.1f), clickHz = 2500f, clickQ = 0.4f, clickAmp = 0.7f, clickMs = 70f, len = 0.2f),
        "bubble" to Spec(freq = 380f, partials = floatArrayOf(1f, 1f, 0.1f, 3f, 0.2f, 0.03f), bend = 2.4f, bendT = 0.05f, len = 0.14f),
        "hud" to Spec(shape = 4, freq = 880f, partials = floatArrayOf(1f, 0.8f, 0.05f), secondRatio = 1.5f, secondDelay = 0.045f, len = 0.16f),
        "neon" to Spec(shape = 3, freq = 440f, partials = floatArrayOf(1f, 0.8f, 0.08f, 1.005f, 0.6f, 0.1f), len = 0.15f),
        "droplet" to Spec(freq = 1100f, partials = floatArrayOf(1f, 1f, 0.08f), bend = 0.7f, bendT = 0.04f, echo = true, len = 0.35f),
        "magma" to Spec(freq = 95f, partials = floatArrayOf(1f, 0.6f, 0.15f), clickHz = 1800f, clickQ = 0.5f, clickAmp = 0.5f, clickMs = 40f, subHz = 70f, subAmp = 1f, subDecay = 0.2f, len = 0.3f),
        "marimba" to Spec(freq = 660f, partials = floatArrayOf(1f, 1f, 0.2f, 4f, 0.35f, 0.08f, 10f, 0.12f, 0.03f), clickHz = 3000f, clickQ = 1f, clickAmp = 0.15f, clickMs = 3f, len = 0.3f),
        "chip" to Spec(shape = 2, freq = 660f, partials = floatArrayOf(1f, 1f, 0.06f), crush = 10, secondRatio = 1.5f, secondDelay = 0.04f, len = 0.12f),
        "hifi" to Spec(freq = 330f, partials = floatArrayOf(1f, 0.3f, 0.06f), clickHz = 2600f, clickQ = 1.3f, clickAmp = 0.6f, clickMs = 7f, subHz = 220f, subAmp = 0.8f, subDecay = 0.09f, bottom = 0.8f, len = 0.16f),
        "shadow" to Spec(clickHz = 700f, clickQ = 0.6f, clickAmp = 0.3f, clickMs = 10f, subHz = 120f, subAmp = 1f, subDecay = 0.06f, len = 0.1f),
        "linear" to Spec(clickHz = 1800f, clickQ = 0.8f, clickAmp = 0.2f, clickMs = 10f, subHz = 200f, subAmp = 0.7f, subDecay = 0.1f, bottom = 0.4f, len = 0.13f),
        "thock" to Spec(freq = 165f, partials = floatArrayOf(1f, 0.5f, 0.12f), clickHz = 2000f, clickQ = 1f, clickAmp = 0.5f, clickMs = 8f, subHz = 120f, subAmp = 1f, subDecay = 0.16f, bottom = 1f, len = 0.24f),
        "silk" to Spec(freq = 300f, partials = floatArrayOf(1f, 0.6f, 0.12f), clickHz = 1200f, clickQ = 0.4f, clickAmp = 0.25f, clickMs = 30f, len = 0.15f)
    )

    /** Raw 16-bit mono samples of one key sound. */
    fun render(voice: String, category: String, pitchSt: Float, tone: Int, travelMm: Float, variant: Int): ShortArray? {
        val spec = specs[voice] ?: return null
        val sr = SR
        val f = when (category) {
            "space" -> floatArrayOf(0.78f, 1.35f, 1.3f, 0.9f)
            "enter" -> floatArrayOf(0.9f, 1.25f, 1.2f, 1.1f)
            "delete" -> floatArrayOf(1.12f, 0.85f, 0.9f, 1f)
            else -> floatArrayOf(1f, 1f, 1f, 1f)
        }
        val pMul = f[0]
        val dMul = f[1]
        val sMul = f[2]
        val cMul = f[3]
        val pm = 2.0.pow(pitchSt / 12.0).toFloat() * pMul * (1f + (variant - 1) * 0.03f)
        val extra = 0.07f + if (spec.echo) 0.22f else 0f
        val n = (sr * (spec.len * dMul + extra)).toInt()
        val buf = FloatArray(n)
        val rnd = Random(4000 + variant * 77 + voice.hashCode())
        val bottomDelay = ((0.006f + travelMm * 0.004f) * sr).toInt()

        fun addTonal(offset: Int, ratioMul: Float, ampMul: Float) {
            if (spec.freq <= 0f) return
            var i = 0
            while (i + 2 < spec.partials.size) {
                val ratio = spec.partials[i]
                val amp = spec.partials[i + 1] * ampMul
                val dec = spec.partials[i + 2] * dMul
                val f0 = spec.freq * ratio * pm * ratioMul
                var phase = 0.0
                var idx = 0
                while (offset + idx < n) {
                    val t = idx / sr.toFloat()
                    val inst = f0 * (1f + (spec.bend - 1f) * exp(-t / spec.bendT))
                    phase += inst / sr
                    val ph = phase - Math.floor(phase)
                    val w = when (spec.shape) {
                        1 -> if (ph < 0.5) 0.5f else -0.5f
                        2 -> if (ph < 0.125) 0.5f else -0.5f
                        3 -> (2f * ph.toFloat() - 1f) * 0.55f
                        4 -> (1f - 4f * abs(ph.toFloat() - 0.5f)) * 0.9f
                        else -> sin(2.0 * PI * phase).toFloat()
                    }
                    val attack = (t / 0.001f).coerceAtMost(1f)
                    val env = exp(-t / dec) * attack
                    if (env < 0.001f && t > 0.02f) break
                    buf[offset + idx] += w * amp * env * 0.55f
                    idx++
                }
                i += 3
            }
        }

        fun addClick(offset: Int, scale: Float) {
            if (spec.clickAmp <= 0f) return
            val fc = (spec.clickHz * Math.sqrt(pm.toDouble()).toFloat()).coerceIn(200f, 9500f)
            val ff = (2.0 * sin(PI * fc / sr)).toFloat().coerceAtMost(1.2f)
            val q = 1f / spec.clickQ
            val nb = (sr * spec.clickMs * 3f / 1000f).toInt()
            val tau = sr * spec.clickMs / 1000f / 2.2f
            var low = 0f
            var band = 0f
            for (i in 0 until nb) {
                if (offset + i >= n) break
                val x = (rnd.nextFloat() * 2f - 1f) * exp(-i / tau)
                val high = x - low - q * band
                band += ff * high
                low += ff * band
                buf[offset + i] += band * spec.clickAmp * cMul * scale * 1.6f
            }
        }

        fun addSub(offset: Int, scale: Float) {
            if (spec.subAmp <= 0f) return
            val hz = spec.subHz * pm
            val dec = spec.subDecay * dMul
            var phase = 0.0
            var idx = 0
            while (offset + idx < n) {
                val t = idx / sr.toFloat()
                phase += hz.toDouble() * (1.0 + 0.5 * exp(-t / 0.012)) / sr
                val env = exp(-t / dec) * (t / 0.002f).coerceAtMost(1f)
                if (env < 0.001f && t > 0.02f) break
                buf[offset + idx] += sin(2.0 * PI * phase).toFloat() * spec.subAmp * sMul * scale * env * 0.7f
                idx++
            }
        }

        addTonal(0, 1f, 1f)
        addClick(0, 1f)
        addSub(0, 1f)
        if (spec.bottom > 0f) {
            addClick(bottomDelay, spec.bottom * 0.8f)
            addSub(bottomDelay, spec.bottom * 0.8f)
        }
        if (spec.secondRatio > 0f) addTonal((spec.secondDelay * sr).toInt(), spec.secondRatio, 0.85f)
        if (spec.echo) {
            val src = buf.copyOf()
            val taps = floatArrayOf(0.09f, 0.4f, 0.17f, 0.2f)
            var k = 0
            while (k < taps.size) {
                val d = (taps[k] * sr).toInt()
                val g = taps[k + 1]
                for (i in 0 until n - d) buf[i + d] += src[i] * g
                k += 2
            }
        }
        if (spec.crush > 0) {
            val lv = spec.crush.toFloat()
            var held = 0f
            for (i in 0 until n) {
                if (i % 2 == 0) held = (buf[i] * lv).roundToInt() / lv
                buf[i] = held
            }
        }
        // tone: 0 = dark ... 100 = bright (low-pass cutoff from 500 Hz to the top of the range)
        val fc = 500.0 * 2.0.pow(tone.coerceIn(0, 100) / 100.0 * 4.5)
        if (fc < 10500.0) {
            val a = (1.0 - exp(-2.0 * PI * fc / sr)).toFloat()
            repeat(2) {
                var y = 0f
                for (i in 0 until n) {
                    y += a * (buf[i] - y)
                    buf[i] = y
                }
            }
        }
        var peak = 0f
        for (v in buf) if (abs(v) > peak) peak = abs(v)
        if (peak <= 0f) return null
        val gain = 0.88f / peak
        val fade = (sr * 0.004f).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            var v = buf[i] * gain
            if (i > n - fade) v *= (n - i).toFloat() / fade
            out[i] = (v.coerceIn(-1f, 1f) * 32000f).toInt().toShort()
        }
        return out
    }

    fun wavBytes(pcm: ShortArray): ByteArray {
        val data = pcm.size * 2
        val bb = ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray()); bb.putInt(36 + data); bb.put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray()); bb.putInt(16); bb.putShort(1); bb.putShort(1)
        bb.putInt(SR); bb.putInt(SR * 2); bb.putShort(2); bb.putShort(16)
        bb.put("data".toByteArray()); bb.putInt(data)
        for (s in pcm) bb.putShort(s)
        return bb.array()
    }
}
