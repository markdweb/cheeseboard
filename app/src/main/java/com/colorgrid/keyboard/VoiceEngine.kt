package com.colorgrid.keyboard

import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.pow
import kotlin.random.Random

/** Everything that decides how a key sounds. */
data class SoundConfig(
    val voice: String,
    val pitch: Float,
    val tone: Int,
    val travel: Float,
    val volume: Int,
    val groove: Int,
    val keySpecific: Boolean
) {
    fun key(category: String, variant: Int) =
        "$voice|$category|${(pitch * 10).toInt()}|$tone|${(travel * 100).toInt()}|$variant"
}

object VoiceEngine {
    class Voice(val id: String, val title: String, val type: String, val color: Int)

    private fun c(s: String) = Color.parseColor(s)

    /** The 24 built-in voices (silent included). */
    val voices: List<Voice> = listOf(
        Voice("boop", "Boop", "MELODIC", c("#FF7A45")),
        Voice("carbon", "Carbon", "SOFT-TOUCH", c("#C99A5B")),
        Voice("machina", "Machina", "THOCK", c("#F2EBDD")),
        Voice("titanium", "Titanium", "LINEAR", c("#E8EAEE")),
        Voice("crystal", "Crystal", "GLASSY", c("#7FE3FF")),
        Voice("pulse", "Pulse", "ELECTRONIC", c("#FF4DA6")),
        Voice("topre", "Topre", "CUSHIONED", c("#B28CFF")),
        Voice("ceramic", "Ceramic", "TINK", c("#7FA2F5")),
        Voice("clicky", "Clicky", "CLICK JACKET", c("#FFD54D")),
        Voice("deep", "Deep tactile", "TACTILE BUMP", c("#D4693A")),
        Voice("airy", "Airy", "BREATHY", c("#BFE8E8")),
        Voice("bubble", "Bubble", "POP", c("#8EC9FF")),
        Voice("hud", "HUD", "SCI-FI", c("#2EF2D0")),
        Voice("neon", "Neon", "SYNTH", c("#FF2E97")),
        Voice("droplet", "Droplet", "WATER", c("#4FB3FF")),
        Voice("magma", "Magma", "RUMBLE", c("#FF6A00")),
        Voice("marimba", "Marimba", "MALLET", c("#F2C14E")),
        Voice("chip", "Chip", "8-BIT", c("#5CF2FF")),
        Voice("hifi", "Hi-Fi", "CLEAN", c("#F0A12F")),
        Voice("shadow", "Shadow", "MUTED", c("#8A8AA0")),
        Voice("linear", "Linear", "SMOOTH", c("#A8D5A2")),
        Voice("thock", "Thock", "DEEP THOCK", c("#D9A066")),
        Voice("silk", "Silk", "SOFT", c("#F2C6D9")),
        Voice("silent", "Silent", "OFF", c("#6B6B75"))
    )

    /** The recorded Mechvibes samples that were already in the app. */
    val sampleVoices: List<Voice> = KeyboardSounds.profiles.filter { it.id != "silent" }.map {
        Voice("sample:" + it.id, it.title, "SAMPLE", c("#9AA0B0"))
    }

    fun allVoices(): List<Voice> = voices + sampleVoices

    fun voiceTitle(id: String): String = allVoices().firstOrNull { it.id == id }?.title ?: id

    // ---------- playback ----------

    private var pool: SoundPool? = null
    private val ids = HashMap<String, Int>()
    private val loaded = HashSet<Int>()
    private val pending = HashMap<Int, FloatArray>()
    private val executor = Executors.newSingleThreadExecutor()
    private val lock = Any()
    private var step = 0
    private val rnd = Random(System.nanoTime())

    private val subtle = intArrayOf(0, 2, 4, 2)
    private val musical = intArrayOf(0, 2, 4, 7, 9, 7, 4, 2)

    private fun ensurePool(): SoundPool {
        synchronized(lock) {
            pool?.let { return it }
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val p = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attrs).build()
            p.setOnLoadCompleteListener { sp, sampleId, status ->
                if (status == 0) {
                    synchronized(lock) {
                        loaded.add(sampleId)
                        pending.remove(sampleId)?.let { a -> sp.play(sampleId, a[0], a[0], 1, 0, a[1]) }
                    }
                }
            }
            pool = p
            return p
        }
    }

    /** Builds the sounds for the current settings in the background so the first key press is not late. */
    fun prepare(context: Context, cfg: SoundConfig) {
        if (cfg.voice == "silent" || cfg.voice.startsWith("sample:")) return
        val dir = File(context.applicationContext.cacheDir, "voices")
        executor.execute {
            try {
                dir.mkdirs()
                val p = ensurePool()
                synchronized(lock) {
                    if (ids.size > 120) {
                        ids.values.forEach { p.unload(it) }
                        ids.clear()
                        loaded.clear()
                        pending.clear()
                    }
                }
                for (cat in listOf("letter", "space", "enter", "delete")) {
                    for (variant in 0..2) {
                        val key = cfg.key(cat, variant)
                        if (synchronized(lock) { ids.containsKey(key) }) continue
                        val pcm = VoiceSynth.render(cfg.voice, cat, cfg.pitch, cfg.tone, cfg.travel, variant) ?: continue
                        val f = File(dir, Integer.toHexString(key.hashCode()) + ".wav")
                        f.writeBytes(VoiceSynth.wavBytes(pcm))
                        val id = p.load(f.absolutePath, 1)
                        synchronized(lock) { ids[key] = id }
                    }
                }
            } catch (e: Exception) {
                // sound is optional
            }
        }
    }

    fun play(context: Context, cfg: SoundConfig, category: String) {
        if (cfg.volume <= 0 || cfg.voice == "silent") return
        val cat = if (cfg.keySpecific) category else "letter"
        var rate = 2.0.pow(cfg.pitch / 12.0).toFloat()
        var vol = cfg.volume.coerceIn(0, 100) / 100f
        if (cfg.voice.startsWith("sample:")) {
            KeyboardSounds.play(context, cfg.voice.removePrefix("sample:"), cfg.volume, cat, rate.coerceIn(0.5f, 2f))
            return
        }
        // groove: a quiet chord walk with a soft accent on every fourth press, so the same note does not repeat
        if (cfg.groove > 0 && cat == "letter") {
            val seq = if (cfg.groove == 1) subtle else musical
            val i = step++ % seq.size
            rate *= 2.0.pow(seq[i] / 12.0).toFloat()
            vol *= if (i % 4 == 0) 1f else 0.8f
        }
        val key = cfg.key(cat, rnd.nextInt(3))
        val id = synchronized(lock) { ids[key] }
        if (id == null) {
            prepare(context, cfg)
            return
        }
        val p = ensurePool()
        val r = rate.coerceIn(0.5f, 2f)
        synchronized(lock) {
            if (loaded.contains(id)) p.play(id, vol, vol, 1, 0, r) else pending[id] = floatArrayOf(vol, r)
        }
    }

    /** Test strip / preview button. */
    fun preview(context: Context, cfg: SoundConfig, category: String = "letter") = play(context, cfg, category)
}

/** Vibration patterns. Every theme has its own feel, and the Sound tab can override it. */
object Haptics {
    private class Pattern(val timings: LongArray, val amps: IntArray)

    private val patterns = mapOf(
        "squish" to Pattern(longArrayOf(14, 18, 10), intArrayOf(150, 0, 70)),
        "blip" to Pattern(longArrayOf(8), intArrayOf(190)),
        "thock" to Pattern(longArrayOf(22), intArrayOf(230)),
        "tick" to Pattern(longArrayOf(6), intArrayOf(255)),
        "rumble" to Pattern(longArrayOf(20, 14, 20, 14, 22), intArrayOf(120, 0, 170, 0, 230)),
        "mallet" to Pattern(longArrayOf(24, 10, 10), intArrayOf(170, 0, 70)),
        "plip" to Pattern(longArrayOf(6, 8, 6), intArrayOf(150, 0, 90)),
        "pop" to Pattern(longArrayOf(12), intArrayOf(225)),
        "glasstick" to Pattern(longArrayOf(4), intArrayOf(255)),
        "clack" to Pattern(longArrayOf(10, 12, 10), intArrayOf(210, 0, 150)),
        "snap" to Pattern(longArrayOf(9), intArrayOf(255)),
        "clickjacket" to Pattern(longArrayOf(8, 22, 6), intArrayOf(235, 0, 110)),
        "tink" to Pattern(longArrayOf(3), intArrayOf(210)),
        "bit" to Pattern(longArrayOf(5, 6, 5, 6, 5), intArrayOf(200, 0, 200, 0, 200)),
        "keybed" to Pattern(longArrayOf(30), intArrayOf(95)),
        "zap" to Pattern(longArrayOf(5, 5, 14), intArrayOf(255, 0, 150)),
        "double" to Pattern(longArrayOf(8, 50, 8), intArrayOf(200, 0, 200))
    )

    /** Name shown in the app for each pattern. */
    fun title(id: String): String = when (id) {
        "squish" -> "Squish"
        "blip" -> "Blip"
        "thock" -> "Thock"
        "tick" -> "Firm tick"
        "rumble" -> "Rumble"
        "mallet" -> "Mallet"
        "plip" -> "Plip"
        "pop" -> "Pop"
        "glasstick" -> "Glass tick"
        "clack" -> "Clack"
        "snap" -> "Snap"
        "clickjacket" -> "Click jacket"
        "tink" -> "Tink"
        "bit" -> "8-bit"
        "keybed" -> "Keybed"
        "zap" -> "Zap"
        "double" -> "Double"
        else -> id
    }

    fun description(id: String): String = when (id) {
        "squish" -> "soft jelly bounce"
        "blip" -> "one short, clean pulse"
        "thock" -> "deep, weighty knock"
        "tick" -> "sharp and firm"
        "rumble" -> "three low pulses, like lava"
        "mallet" -> "soft hit with a small bounce"
        "plip" -> "light water drop"
        "pop" -> "quick bubble pop"
        "glasstick" -> "tiny, glassy tick"
        "clack" -> "two crisp taps"
        "snap" -> "short and snappy"
        "clickjacket" -> "click, then a soft settle"
        "tink" -> "very light tap"
        "bit" -> "three tiny arcade pulses"
        "keybed" -> "long, soft press"
        "zap" -> "spark with a short tail"
        else -> ""
    }

    /** Pattern id used by a "Crisp / Soft / Heavy / Double" choice, or the theme's own for "theme". */
    fun resolve(choice: String, themeId: String): String = when (choice) {
        "crisp" -> "tick"
        "soft" -> "keybed"
        "heavy" -> "thock"
        "double" -> "double"
        else -> KeyboardThemes.hapticFor(themeId)
    }

    fun play(v: Vibrator?, patternId: String, strength: Int, heavy: Boolean = false) {
        if (v == null || !v.hasVibrator() || strength <= 0) return
        val p = patterns[patternId] ?: patterns["blip"]!!
        val scale = strength / 100f * 1.25f * (if (heavy) 1.25f else 1f)
        try {
            if (v.hasAmplitudeControl()) {
                val amps = IntArray(p.amps.size) { i ->
                    if (p.amps[i] == 0) 0 else (p.amps[i] * scale).toInt().coerceIn(1, 255)
                }
                v.vibrate(VibrationEffect.createWaveform(p.timings, amps, -1))
            } else {
                var on = 0L
                for (i in p.timings.indices) if (p.amps[i] > 0) on += p.timings[i]
                v.vibrate(VibrationEffect.createOneShot((on * scale).toLong().coerceAtLeast(5L), VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) {
            // ignore: haptics are optional
        }
    }
}
