package com.colorgrid.keyboard

import org.json.JSONObject

/** One entry of the keyboard's clipboard history. */
data class ClipEntry(val text: String, val pinned: Boolean, val time: Long) {
    fun toJson(): JSONObject = JSONObject()
        .put("text", text)
        .put("pinned", pinned)
        .put("time", time)

    companion object {
        fun fromJson(o: JSONObject): ClipEntry = ClipEntry(
            text = o.getString("text"),
            pinned = o.optBoolean("pinned", false),
            time = o.optLong("time", 0L)
        )
    }
}
