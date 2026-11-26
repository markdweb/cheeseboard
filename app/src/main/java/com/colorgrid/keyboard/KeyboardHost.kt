package com.colorgrid.keyboard

/** What the keyboard view needs from the InputMethodService. */
interface KeyboardHost {
    fun typeText(text: String)
    fun backspace()
    fun enter()
    /** Label for the enter key (e.g. "Go", "Search"), or null for the plain return icon. */
    fun enterLabel(): String?
    /** True when the cursor is at a position where the next letter should be capitalized. */
    fun cursorCaps(): Boolean
    fun switchKeyboard()
    fun hideKeyboard()
    /** Opens the settings app. [tab] is "settings" or null. */
    fun openApp(tab: String?)
    fun insertClip(text: String): Boolean
    /** Copies a calculator result, inserts it into the focused field, and records it. */
    fun insertCalculatorResult(text: String): Boolean
    /** Up to [n] characters before the cursor ("" when unavailable). */
    fun textBeforeCursor(n: Int): String
    /** Deletes [deleteCount] characters before the cursor and types [text] in their place. */
    fun replaceBeforeCursor(deleteCount: Int, text: String)
    /** False for password fields, URL/e-mail fields and fields that ask for no suggestions. */
    fun suggestionsAllowed(): Boolean
    /** True while the floating Color Grid overlay is showing. */
    fun isGridOn(): Boolean
    /** Turns the floating Color Grid overlay on/off (asks for "Display over other apps" the first time). */
    fun setGrid(on: Boolean)
    /** Moves the text cursor one character left or right. */
    fun moveCursor(left: Boolean)
    /**
     * Sends a downloaded GIF to the app being typed in. If that app does not accept GIFs the link is typed
     * instead. Returns true when the GIF itself was accepted.
     */
    fun commitGif(file: java.io.File, link: String): Boolean
}
