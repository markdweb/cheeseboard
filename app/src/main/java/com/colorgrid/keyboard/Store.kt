package com.colorgrid.keyboard

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import org.json.JSONArray

/** All persistent data (settings, clipboard history, learned words), shared by the keyboard and the app UI. */
class Store(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("colorgrid_prefs", Context.MODE_PRIVATE)

    // ---------- settings ----------

    /** 0 = follow system, 1 = light, 2 = dark */
    var themeMode: Int
        get() = sp.getInt("theme_mode", 0)
        set(v) { sp.edit().putInt("theme_mode", v).apply() }

    /** Keyboard theme id: glass, classic, rgb, retro, neon, sakura, matcha. */
    var keyboardTheme: String
        get() = sp.getString("kb_theme", null)
            ?: if (sp.getBoolean("glass_theme", true)) "glass" else "classic"
        set(v) { sp.edit().putString("kb_theme", v).apply() }

    /** Theme being tried from the Themes page (not saved as the worn theme). */
    var previewTheme: String?
        get() = sp.getString("preview_theme", null)
        set(v) { sp.edit().apply { if (v == null) remove("preview_theme") else putString("preview_theme", v) }.apply() }

    /** The theme the keyboard shows right now. */
    val activeTheme: String get() = previewTheme ?: keyboardTheme

    /** Press effect style: auto (theme default), glow, ripple, sparks, halo. */
    var pressEffect: String
        get() = sp.getString("press_effect", "auto") ?: "auto"
        set(v) { sp.edit().putString("press_effect", v).apply() }

    /** Bubble with the pressed letter above the finger. */
    var keyPreviews: Boolean
        get() = sp.getBoolean("key_previews", true)
        set(v) { sp.edit().putBoolean("key_previews", v).apply() }

    /** Small corner symbols on letter keys (long-press to type them). */
    var hintSymbols: Boolean
        get() = sp.getBoolean("hint_symbols", true)
        set(v) { sp.edit().putBoolean("hint_symbols", v).apply() }

    /** Recent emoji in a row above the keys. */
    var emojiRow: Boolean
        get() = sp.getBoolean("emoji_row", false)
        set(v) { sp.edit().putBoolean("emoji_row", v).apply() }

    /** Wearing a theme also switches to the key sound that goes with it. */
    var themeSound: Boolean
        get() = sp.getBoolean("theme_sound", true)
        set(v) { sp.edit().putBoolean("theme_sound", v).apply() }

    /** The character on the space bar (each theme has its own). */
    var spacebarMascot: Boolean
        get() = sp.getBoolean("spacebar_mascot", true)
        set(v) { sp.edit().putBoolean("spacebar_mascot", v).apply() }

    /** GIF search provider: klipy (free key) or giphy. Tenor's API was shut down in June 2026. */
    var gifProvider: String
        get() = sp.getString("gif_provider", "klipy") ?: "klipy"
        set(v) { sp.edit().putString("gif_provider", v).apply() }

    var gifApiKey: String
        get() = sp.getString("gif_api_key", "") ?: ""
        set(v) { sp.edit().putString("gif_api_key", v).apply() }

    /** Random id sent with GIF searches (no personal data). */
    val gifClientId: String
        get() {
            var id = sp.getString("gif_client_id", null)
            if (id == null) {
                id = java.util.UUID.randomUUID().toString()
                sp.edit().putString("gif_client_id", id).apply()
            }
            return id
        }

    var numberRow: Boolean
        get() = sp.getBoolean("number_row", false)
        set(v) { sp.edit().putBoolean("number_row", v).apply() }

    var arrowKeys: Boolean
        get() = sp.getBoolean("arrow_keys", false)
        set(v) { sp.edit().putBoolean("arrow_keys", v).apply() }

    /** Key press effects: press effect, RGB backlight wave. */
    var keyEffects: Boolean
        get() = sp.getBoolean("key_effects", true)
        set(v) { sp.edit().putBoolean("key_effects", v).apply() }

    var heightScale: Float
        get() = sp.getFloat("height_scale", 1.0f)
        set(v) { sp.edit().putFloat("height_scale", v).apply() }

    var keyScale: Float
        get() = sp.getFloat("key_scale", 1.0f)
        set(v) { sp.edit().putFloat("key_scale", v).apply() }

    var sound: Boolean
        get() = sp.getBoolean("sound", false)
        set(v) { sp.edit().putBoolean("sound", v).apply() }

    /** Built-in synthesized sound profile; no external audio assets or network access. */
    var soundProfile: String
        get() = when (sp.getString("sound_profile", "cherry_brown_abs")) {
            "deep_thock", "topre" -> "topre_purple"
            "crisp_clicky" -> "cherry_blue_abs"
            "typewriter" -> "cherry_brown_abs"
            "smooth_linear" -> "cherry_red_abs"
            else -> sp.getString("sound_profile", "cherry_brown_abs") ?: "cherry_brown_abs"
        }
        set(v) { sp.edit().putString("sound_profile", v).apply() }

    var soundVolume: Int
        get() = sp.getInt("sound_volume", 65).coerceIn(0, 100)
        set(v) { sp.edit().putInt("sound_volume", v.coerceIn(0, 100)).apply() }

    var vibrate: Boolean
        get() = sp.getBoolean("vibrate", true)
        set(v) { sp.edit().putBoolean("vibrate", v).apply() }

    // ---------- sound lab ----------

    /** True: the voice follows the theme. False: the voice picked in the Sound tab is used. */
    var themeVoice: Boolean
        get() = sp.getBoolean("theme_voice", true)
        set(v) { sp.edit().putBoolean("theme_voice", v).apply() }

    var voiceId: String
        get() = sp.getString("voice_id", "boop") ?: "boop"
        set(v) { sp.edit().putString("voice_id", v).apply() }

    /** Semitones, -12..12 */
    var pitchSt: Float
        get() = sp.getFloat("pitch_st", 0f)
        set(v) { sp.edit().putFloat("pitch_st", v).apply() }

    /** 0 dark ... 100 bright */
    var tone: Int
        get() = sp.getInt("tone", 70).coerceIn(0, 100)
        set(v) { sp.edit().putInt("tone", v.coerceIn(0, 100)).apply() }

    /** Key travel in mm (1.0 - 4.0). Longer travel puts more time between the click and the bottom-out. */
    var keyTravel: Float
        get() = sp.getFloat("key_travel", 2.5f).coerceIn(1f, 4f)
        set(v) { sp.edit().putFloat("key_travel", v.coerceIn(1f, 4f)).apply() }

    /** 0 off, 1 subtle, 2 musical */
    var groove: Int
        get() = sp.getInt("groove", 0)
        set(v) { sp.edit().putInt("groove", v).apply() }

    var keySpecific: Boolean
        get() = sp.getBoolean("key_specific", true)
        set(v) { sp.edit().putBoolean("key_specific", v).apply() }

    /** Vibration strength 0..100 */
    var hapticStrength: Int
        get() = sp.getInt("haptic_strength", 73).coerceIn(0, 100)
        set(v) { sp.edit().putInt("haptic_strength", v.coerceIn(0, 100)).apply() }

    /** theme, crisp, soft, heavy, double */
    var hapticPattern: String
        get() = sp.getString("haptic_pattern", "theme") ?: "theme"
        set(v) { sp.edit().putString("haptic_pattern", v).apply() }

    fun activeVoice(): String = if (themeVoice) KeyboardThemes.voiceFor(activeTheme) else voiceId

    fun soundConfig() = SoundConfig(activeVoice(), pitchSt, tone, keyTravel, soundVolume, groove, keySpecific)

    // ---------- typing ----------

    /** Show word suggestions above the keys while typing. */
    var predictEnabled: Boolean
        get() = sp.getBoolean("predict_enabled", true)
        set(v) { sp.edit().putBoolean("predict_enabled", v).apply() }

    /** Remember the words you type so they show up in suggestions (stored only on this device). */
    var learnWords: Boolean
        get() = sp.getBoolean("learn_words", true)
        set(v) { sp.edit().putBoolean("learn_words", v).apply() }

    fun loadLearned(): String? = sp.getString("learned_words", null)
    fun saveLearned(json: String) { sp.edit().putString("learned_words", json).apply() }
    /** Bumped when the learned words are cleared, so a running keyboard drops its in-memory copy too. */
    val learnEpoch: Int get() = sp.getInt("learn_epoch", 0)
    fun clearLearned() { sp.edit().remove("learned_words").putInt("learn_epoch", learnEpoch + 1).apply() }

    // ---------- clipboard settings ----------

    var historyEnabled: Boolean
        get() = sp.getBoolean("history_enabled", true)
        set(v) { sp.edit().putBoolean("history_enabled", v).apply() }

    /** Also record text copied in other apps (only possible while this keyboard is active). */
    var captureExternal: Boolean
        get() = sp.getBoolean("capture_external", true)
        set(v) { sp.edit().putBoolean("capture_external", v).apply() }

    var historyLimit: Int
        get() = sp.getInt("history_limit", 25)
        set(v) { sp.edit().putInt("history_limit", v).apply() }

    fun registerListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        sp.registerOnSharedPreferenceChangeListener(l)

    fun unregisterListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        sp.unregisterOnSharedPreferenceChangeListener(l)

    /** Changes whenever the keyboard view must be rebuilt. */
    fun signature(ctx: Context): String {
        val c = ctx.resources.configuration
        return listOf(
            themeMode, activeTheme, keyEffects, pressEffect, numberRow, arrowKeys, hintSymbols, emojiRow, spacebarMascot, activeVoice(), pitchSt, tone, keyTravel, sound, heightScale, keyScale, predictEnabled,
            c.orientation, c.uiMode and Configuration.UI_MODE_NIGHT_MASK, c.screenWidthDp
        ).joinToString("|")
    }

    // ---------- recent emoji (newest first) ----------

    fun loadRecentEmoji(): List<String> {
        val raw = sp.getString("emoji_recent", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addRecentEmoji(e: String) {
        val list = loadRecentEmoji().toMutableList()
        list.remove(e)
        list.add(0, e)
        while (list.size > 36) list.removeAt(list.lastIndex)
        sp.edit().putString("emoji_recent", JSONArray(list).toString()).apply()
    }

    // ---------- clipboard history (stored newest first) ----------

    private fun loadHistory(): MutableList<ClipEntry> {
        val raw = sp.getString("history", null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { ClipEntry.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun saveHistory(list: List<ClipEntry>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        sp.edit().putString("history", arr.toString()).apply()
    }

    /** Pinned entries first, then newest first. */
    fun sortedHistory(): List<ClipEntry> = loadHistory().sortedByDescending { it.pinned }

    fun addHistory(text: String) {
        val clean = text.trim()
        if (!historyEnabled || clean.isEmpty()) return
        val list = loadHistory()
        val existing = list.firstOrNull { it.text == clean }
        list.removeAll { it.text == clean }
        list.add(0, ClipEntry(clean, existing?.pinned ?: false, System.currentTimeMillis()))
        var unpinned = list.count { !it.pinned }
        while (unpinned > historyLimit) {
            val idx = list.indexOfLast { !it.pinned }
            if (idx < 0) break
            list.removeAt(idx)
            unpinned--
        }
        saveHistory(list)
    }

    fun togglePin(text: String) {
        saveHistory(loadHistory().map { if (it.text == text) it.copy(pinned = !it.pinned) else it })
    }

    fun removeHistory(text: String) {
        saveHistory(loadHistory().filter { it.text != text })
    }

    fun clearHistory(keepPinned: Boolean) {
        saveHistory(if (keepPinned) loadHistory().filter { it.pinned } else emptyList())
    }
}
