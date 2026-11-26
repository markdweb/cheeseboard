package com.colorgrid.keyboard

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class GifItem(val id: String, val previewUrl: String, val fullUrl: String)

/**
 * GIF search over the internet (the only part of Cheeseboard that uses it, and only when you open the GIF search).
 * Google shut down the Tenor API on 30 June 2026, so this talks to KLIPY (free key) or GIPHY (key you provide).
 */
object GifClient {
    private val pool = Executors.newFixedThreadPool(4)
    private val main = Handler(Looper.getMainLooper())
    private val bytesCache = object : LruCache<String, ByteArray>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

    private class HttpError(msg: String) : Exception(msg)

    fun configured(store: Store) = store.gifApiKey.isNotBlank()

    fun providerName(store: Store) = if (store.gifProvider == "giphy") "GIPHY" else "KLIPY"

    /** [cb] runs on the main thread with the results, or an error message. An empty query shows trending GIFs. */
    fun search(store: Store, query: String, cb: (List<GifItem>, String?) -> Unit) {
        val provider = store.gifProvider
        val key = store.gifApiKey.trim()
        val clientId = store.gifClientId
        pool.execute {
            var items: List<GifItem> = emptyList()
            var err: String? = null
            try {
                items = fetch(provider, key, clientId, query.trim())
                if (items.isEmpty()) err = "No GIFs found"
            } catch (e: HttpError) {
                err = e.message
            } catch (e: Exception) {
                err = "Couldn't reach the GIF service. Check your internet connection."
            }
            main.post { cb(items, err) }
        }
    }

    private fun fetch(provider: String, key: String, clientId: String, query: String): List<GifItem> {
        val q = URLEncoder.encode(query, "UTF-8")
        return if (provider == "giphy") {
            val url = if (query.isEmpty()) {
                "https://api.giphy.com/v1/gifs/trending?api_key=$key&limit=24&rating=pg-13"
            } else {
                "https://api.giphy.com/v1/gifs/search?api_key=$key&q=$q&limit=24&rating=pg-13&lang=en"
            }
            parseGiphy(httpGet(url))
        } else {
            val k = URLEncoder.encode(key, "UTF-8")
            val url = if (query.isEmpty()) {
                "https://api.klipy.com/api/v1/$k/gifs/trending?per_page=24&customer_id=$clientId"
            } else {
                "https://api.klipy.com/api/v1/$k/gifs/search?q=$q&per_page=24&page=1&customer_id=$clientId"
            }
            parseKlipy(httpGet(url))
        }
    }

    private fun httpGet(address: String): String {
        val conn = URL(address).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8000
            conn.readTimeout = 10000
            conn.setRequestProperty("User-Agent", "Cheeseboard/1.7")
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            if (code != 200) {
                throw HttpError("GIF search failed ($code). Check the API key in the Cheeseboard app > Settings > GIF.")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun pickKlipy(files: JSONObject, order: Array<String>): String? {
        for (size in order) {
            val u = files.optJSONObject(size)?.optJSONObject("gif")?.optString("url")
            if (!u.isNullOrBlank()) return u
        }
        return null
    }

    private fun parseKlipy(body: String): List<GifItem> {
        val root = JSONObject(body)
        val data = root.optJSONObject("data")
        val arr = data?.optJSONArray("data") ?: return emptyList()
        val out = ArrayList<GifItem>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val files = o.optJSONObject("file") ?: o.optJSONObject("files") ?: continue // ads have no files
            val preview = pickKlipy(files, arrayOf("sm", "xs", "md", "hd")) ?: continue
            val full = pickKlipy(files, arrayOf("md", "sm", "hd", "xs")) ?: preview
            out.add(GifItem(o.optString("slug", i.toString()), preview, full))
        }
        return out
    }

    private fun parseGiphy(body: String): List<GifItem> {
        val arr = JSONObject(body).optJSONArray("data") ?: return emptyList()
        val out = ArrayList<GifItem>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val images = o.optJSONObject("images") ?: continue
            val preview = images.optJSONObject("fixed_height_small")?.optString("url")?.takeIf { it.isNotBlank() }
                ?: images.optJSONObject("fixed_height")?.optString("url")?.takeIf { it.isNotBlank() } ?: continue
            val full = images.optJSONObject("fixed_height")?.optString("url")?.takeIf { it.isNotBlank() }
                ?: images.optJSONObject("original")?.optString("url")?.takeIf { it.isNotBlank() } ?: preview
            out.add(GifItem(o.optString("id", i.toString()), preview, full))
        }
        return out
    }

    private fun fetchBytes(address: String, max: Int): ByteArray? {
        bytesCache.get(address)?.let { return it }
        val conn = URL(address).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8000
            conn.readTimeout = 12000
            conn.setRequestProperty("User-Agent", "Cheeseboard/1.7")
            if (conn.responseCode != 200) return null
            val data = conn.inputStream.use { it.readBytes() }
            if (data.size > max) return null
            bytesCache.put(address, data)
            return data
        } finally {
            conn.disconnect()
        }
    }

    /** Downloads and decodes a small GIF; [cb] runs on the main thread (null if it failed). */
    fun loadPreview(context: Context, url: String, cb: (Drawable?) -> Unit) {
        val res = context.applicationContext.resources
        pool.execute {
            var d: Drawable? = null
            try {
                val bytes = fetchBytes(url, 4 * 1024 * 1024)
                if (bytes != null) {
                    d = if (Build.VERSION.SDK_INT >= 28) {
                        ImageDecoder.decodeDrawable(ImageDecoder.createSource(ByteBuffer.wrap(bytes)))
                    } else {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { BitmapDrawable(res, it) }
                    }
                }
            } catch (e: Exception) {
                d = null
            }
            main.post { cb(d) }
        }
    }

    /** Saves the GIF into the cache folder so it can be shared with the app you are typing in. */
    fun download(context: Context, url: String, cb: (File?) -> Unit) {
        val dir = File(context.applicationContext.cacheDir, "gifs")
        pool.execute {
            var file: File? = null
            try {
                dir.mkdirs()
                val f = File(dir, Integer.toHexString(url.hashCode()) + ".gif")
                if (!f.exists() || f.length() == 0L) {
                    val bytes = fetchBytes(url, 10 * 1024 * 1024)
                    if (bytes != null) f.writeBytes(bytes)
                }
                if (f.exists() && f.length() > 0L) file = f
                // keep the cache small
                val all = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
                all.drop(15).forEach { it.delete() }
            } catch (e: Exception) {
                file = null
            }
            main.post { cb(file) }
        }
    }
}

/** Emoji search by name and keywords ("happy", "cat", "heart"...), using the list in assets/emoji_search.txt. */
object EmojiSearch {
    private class Entry(val emoji: String, val tts: String, val words: String)

    private var entries: List<Entry>? = null

    private fun load(context: Context): List<Entry> {
        entries?.let { return it }
        val list = ArrayList<Entry>()
        try {
            context.assets.open("emoji_search.txt").bufferedReader().useLines { lines ->
                for (line in lines) {
                    val parts = line.split('\t')
                    if (parts.isNotEmpty() && parts[0].isNotEmpty()) {
                        list.add(Entry(parts[0], parts.getOrElse(1) { "" }, parts.getOrElse(2) { "" }))
                    }
                }
            }
        } catch (e: Exception) {
            // empty list: search just finds nothing
        }
        entries = list
        return list
    }

    fun search(context: Context, query: String, limit: Int = 60): List<String> {
        val tokens = query.lowercase().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptyList()
        val scored = ArrayList<Pair<Int, String>>()
        for ((idx, e) in load(context).withIndex()) {
            val hay = " ${e.tts} ${e.words} "
            var score = 0
            var ok = true
            for (t in tokens) {
                if (!hay.contains(" $t")) { ok = false; break }
                score += if (hay.contains(" $t ")) 3 else 1
                if (e.tts == t || e.tts.startsWith("$t ")) score += 4
            }
            if (ok) scored.add(Pair(score * 100000 - idx, e.emoji))
        }
        scored.sortByDescending { it.first }
        val paint = Paint()
        return scored.map { it.second }.filter { paint.hasGlyph(it) }.take(limit)
    }
}
