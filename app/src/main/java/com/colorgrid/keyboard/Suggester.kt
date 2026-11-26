package com.colorgrid.keyboard

import android.content.Context
import org.json.JSONObject

/**
 * Small offline word predictor. A built-in English + Filipino word list (assets/words.txt, most common first)
 * is combined with words the user types often. Nothing leaves the device.
 */
class Suggester(private val context: Context, private val store: Store) {

    /** [slots] is always 3 long, in screen order (left, center, right); empty string = nothing to show. */
    class Result(val prefix: String, val slots: List<String>)

    private var learned = HashMap<String, Int>()
    private var bigrams = HashMap<String, Int>()
    private var loaded = false
    private var dirty = 0
    private var epoch = -1

    private fun dict(): Array<String> {
        sharedWords?.let { return it }
        val list = try {
            context.assets.open("words.txt").bufferedReader().useLines { seq ->
                seq.map { it.trim() }.filter { it.isNotEmpty() }.toList()
            }
        } catch (e: Exception) {
            emptyList()
        }
        val arr = list.toTypedArray()
        sharedWords = arr
        return arr
    }

    private fun ensureLoaded() {
        val e = store.learnEpoch
        if (loaded && e == epoch) return
        loaded = true
        epoch = e
        dirty = 0
        learned = HashMap()
        bigrams = HashMap()
        val raw = store.loadLearned() ?: return
        try {
            val o = JSONObject(raw)
            o.optJSONObject("w")?.let { w ->
                for (k in w.keys()) learned[k] = w.optInt(k, 1)
            }
            o.optJSONObject("b")?.let { b ->
                for (k in b.keys()) bigrams[k] = b.optInt(k, 1)
            }
        } catch (e: Exception) {
            learned = HashMap()
            bigrams = HashMap()
        }
    }

    // ---------- text helpers ----------

    private fun isWordChar(c: Char) = c.isLetter() || c == '\'' || c == '\u2019'

    /** The letters being typed right now (before the cursor), or "". */
    fun currentWord(before: String): String {
        var i = before.length
        while (i > 0 && isWordChar(before[i - 1])) i--
        var w = before.substring(i)
        // a word cannot start with an apostrophe
        while (w.isNotEmpty() && (w[0] == '\'' || w[0] == '\u2019')) w = w.substring(1)
        return w
    }

    private fun previousWord(beforeWord: String): String? {
        val rest = beforeWord.trimEnd(' ', '\t', '\n')
        if (rest.length == beforeWord.length) return null // no separator
        if (rest.isEmpty() || !rest.last().isLetter()) return null
        val w = currentWord(rest).lowercase().replace('\u2019', '\'')
        return w.ifEmpty { null }
    }

    private fun applyCase(word: String, prefix: String, capNext: Boolean): String {
        var w = word
        if (w == "i" || w.startsWith("i'")) w = w.replaceFirstChar { it.uppercaseChar() }
        if (prefix.length > 1 && prefix.all { it.isUpperCase() }) return w.uppercase()
        if (prefix.isNotEmpty() && prefix[0].isUpperCase()) return w.replaceFirstChar { it.uppercaseChar() }
        if (prefix.isEmpty() && capNext) return w.replaceFirstChar { it.uppercaseChar() }
        return w
    }

    // ---------- predictions ----------

    fun suggest(before: String, capNext: Boolean): Result {
        ensureLoaded()
        val prefix = currentWord(before)
        if (prefix.isEmpty()) return Result("", nextWords(before, capNext))

        val lower = prefix.lowercase().replace('\u2019', '\'')
        val found = ArrayList<String>(3)

        // 1) words the user types often
        learned.entries
            .filter { it.value >= 2 && it.key.length > lower.length && it.key.startsWith(lower) }
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.length })
            .take(3)
            .forEach { found.add(it.key) }

        // 2) built-in list (already ordered by how common the words are)
        if (found.size < 3) {
            for (w in dict()) {
                if (w.length > lower.length && w.startsWith(lower) && w !in found) {
                    found.add(w)
                    if (found.size >= 3) break
                }
            }
        }
        val cased = found.map { applyCase(it, prefix, false) }
        val center = cased.getOrNull(0) ?: prefix
        val left = if (cased.isNotEmpty()) prefix else ""
        val right = cased.getOrNull(1) ?: ""
        return Result(prefix, listOf(left, center, right))
    }

    private fun nextWords(before: String, capNext: Boolean): List<String> {
        val prev = if (before.isNotEmpty() && before.last().isWhitespace()) previousWord(before) else null
        val found = ArrayList<String>(3)
        if (prev != null) {
            val head = "$prev "
            bigrams.entries
                .filter { it.value >= 2 && it.key.startsWith(head) }
                .sortedByDescending { it.value }
                .take(3)
                .forEach { found.add(it.key.substring(head.length)) }
        }
        val defaults = if (capNext) listOf("I", "Hindi", "The") else listOf("the", "ang", "and")
        for (d in defaults) if (found.size < 3 && d.lowercase() !in found.map { it.lowercase() }) found.add(d)
        val cased = found.map { applyCase(it, "", capNext) }
        return listOf(cased[1], cased[0], cased[2])
    }

    // ---------- learning ----------

    /** Call with the text before the cursor right before a separator (space, punctuation, enter) is typed. */
    fun learn(before: String) {
        if (!store.learnWords) return
        ensureLoaded()
        val raw = currentWord(before)
        val w = raw.lowercase().replace('\u2019', '\'').trim('\'')
        if (w.length < 2 || w.length > 24) return
        learned[w] = (learned[w] ?: 0) + 1
        val prev = previousWord(before.substring(0, before.length - raw.length))
        if (prev != null) {
            val k = "$prev $w"
            bigrams[k] = (bigrams[k] ?: 0) + 1
        }
        prune(learned)
        prune(bigrams)
        dirty++
        if (dirty >= 15) flush()
    }

    private fun prune(map: HashMap<String, Int>) {
        if (map.size <= 4000) return
        val it = map.entries.iterator()
        while (it.hasNext() && map.size > 3200) {
            if (it.next().value <= 1) it.remove()
        }
    }

    fun flush() {
        if (!loaded || dirty == 0) return
        dirty = 0
        if (epoch != store.learnEpoch) return // cleared from Settings meanwhile
        try {
            val w = JSONObject()
            learned.forEach { (k, v) -> w.put(k, v) }
            val b = JSONObject()
            bigrams.forEach { (k, v) -> b.put(k, v) }
            store.saveLearned(JSONObject().put("w", w).put("b", b).toString())
        } catch (e: Exception) {
            // ignore; learning is best-effort
        }
    }

    companion object {
        @Volatile
        private var sharedWords: Array<String>? = null
    }
}
