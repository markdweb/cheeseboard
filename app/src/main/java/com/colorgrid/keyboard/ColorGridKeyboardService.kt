package com.colorgrid.keyboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.FileProvider
import android.widget.Toast

class ColorGridKeyboardService : InputMethodService(), KeyboardHost {

    private lateinit var store: Store
    private lateinit var clipboard: ClipboardManager
    private var keyboardView: KeyboardView? = null
    private var viewSignature = ""

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener { captureClipboard() }

    /** Settings or the Themes page changed something that needs a new keyboard view: rebuild it right away. */
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        if (keyboardView != null && store.signature(this) != viewSignature) rebuildKeyboard()
    }

    override fun onCreate() {
        super.onCreate()
        store = Store(this)
        clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.addPrimaryClipChangedListener(clipListener)
        store.registerListener(prefListener)
    }

    override fun onDestroy() {
        clipboard.removePrimaryClipChangedListener(clipListener)
        store.unregisterListener(prefListener)
        super.onDestroy()
    }

    private var container: FrameLayout? = null

    /**
     * The keyboard lives inside a fixed container. Changing the theme or a layout setting only swaps the
     * keyboard inside it, so the input window keeps its size and is never left overlapping the app behind it.
     */
    override fun onCreateInputView(): View {
        val c = FrameLayout(this)
        container = c
        attachKeyboard(c)
        return c
    }

    private fun attachKeyboard(c: FrameLayout) {
        keyboardView?.onInputFinished()
        c.removeAllViews()
        val view = KeyboardView(this, store, this)
        keyboardView = view
        viewSignature = store.signature(this)
        c.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun rebuildKeyboard() {
        val c = container
        if (c != null) attachKeyboard(c) else setInputView(onCreateInputView())
        val info = currentInputEditorInfo
        val cls = (info?.inputType ?: 0) and InputType.TYPE_MASK_CLASS
        val numeric = cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE || cls == InputType.TYPE_CLASS_DATETIME
        keyboardView?.onInputStarted(numeric, cursorCaps(), true)
    }

    // The extract (fullscreen) UI would cover the website in landscape; never use it.
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (store.signature(this) != viewSignature) rebuildKeyboard()
        val cls = (info?.inputType ?: 0) and InputType.TYPE_MASK_CLASS
        val numeric = cls == InputType.TYPE_CLASS_NUMBER ||
            cls == InputType.TYPE_CLASS_PHONE ||
            cls == InputType.TYPE_CLASS_DATETIME
        keyboardView?.onInputStarted(numeric, cursorCaps(), restarting)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        keyboardView?.onInputFinished()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        keyboardView?.onSelectionChanged()
    }

    // ---------- KeyboardHost ----------

    override fun typeText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun backspace() {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
    }

    override fun enter() {
        val action = enterAction()
        if (action != 0) {
            currentInputConnection?.performEditorAction(action)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
    }

    override fun enterLabel(): String? = when (enterAction()) {
        EditorInfo.IME_ACTION_GO -> "Go"
        EditorInfo.IME_ACTION_SEARCH -> "Search"
        EditorInfo.IME_ACTION_SEND -> "Send"
        EditorInfo.IME_ACTION_NEXT -> "Next"
        EditorInfo.IME_ACTION_DONE -> "Done"
        EditorInfo.IME_ACTION_PREVIOUS -> "Prev"
        else -> null
    }

    private fun enterAction(): Int {
        val ei = currentInputEditorInfo ?: return 0
        if ((ei.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) return 0
        if ((ei.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0) return 0
        val action = ei.imeOptions and EditorInfo.IME_MASK_ACTION
        return if (action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED) 0 else action
    }

    override fun cursorCaps(): Boolean {
        val ei = currentInputEditorInfo ?: return false
        if ((ei.inputType and InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT) return false
        val mode = currentInputConnection?.getCursorCapsMode(ei.inputType) ?: 0
        return mode != 0
    }

    override fun switchKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }

    override fun hideKeyboard() {
        requestHideSelf(0)
    }

    override fun openApp(tab: String?) {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (tab != null) intent.putExtra("tab", tab)
        startActivity(intent)
    }

    override fun insertCalculatorResult(text: String): Boolean {
        copyToClipboard(text)
        store.addHistory(text)
        val ok = try {
            currentInputConnection?.commitText(text, 1) == true
        } catch (e: Exception) {
            false
        }
        if (!ok) {
            Toast.makeText(this, "Couldn't insert. Result copied - paste it manually.", Toast.LENGTH_LONG).show()
        }
        return ok
    }

    override fun insertClip(text: String): Boolean {
        val ok = try {
            currentInputConnection?.commitText(text, 1) == true
        } catch (e: Exception) {
            false
        }
        if (!ok) {
            copyToClipboard(text)
            Toast.makeText(this, "Couldn't insert. Copied - paste it manually.", Toast.LENGTH_LONG).show()
        }
        return ok
    }

    override fun textBeforeCursor(n: Int): String = try {
        currentInputConnection?.getTextBeforeCursor(n, 0)?.toString() ?: ""
    } catch (e: Exception) {
        ""
    }

    override fun replaceBeforeCursor(deleteCount: Int, text: String) {
        val ic = currentInputConnection ?: return
        try {
            ic.beginBatchEdit()
            if (deleteCount > 0) ic.deleteSurroundingText(deleteCount, 0)
            ic.commitText(text, 1)
            ic.endBatchEdit()
        } catch (e: Exception) {
            // field went away; nothing to do
        }
    }

    /** Mime types the focused field says it accepts (API 33+ field, or the androidx key older apps use). */
    private fun acceptedMimeTypes(): List<String> {
        val ei = currentInputEditorInfo ?: return emptyList()
        val out = ArrayList<String>()
        try {
            if (Build.VERSION.SDK_INT >= 33) ei.contentMimeTypes?.let { out.addAll(it) }
            val extras = ei.extras
            if (extras != null) {
                for (key in listOf(
                    "androidx.core.view.inputmethod.EditorInfoCompat.CONTENT_MIME_TYPES",
                    "android.support.v13.view.inputmethod.EditorInfoCompat.CONTENT_MIME_TYPES"
                )) {
                    extras.getStringArray(key)?.let { out.addAll(it) }
                }
            }
        } catch (e: Exception) {
            // treat as "no image support"
        }
        return out
    }

    override fun commitGif(file: java.io.File, link: String): Boolean {
        val ic: InputConnection? = currentInputConnection
        var ok = false
        if (ic != null && Build.VERSION.SDK_INT >= 25) {
            val accepts = acceptedMimeTypes().any { ClipDescription.compareMimeTypes("image/gif", it) }
            if (accepts) {
                try {
                    val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                    val info = InputContentInfo(uri, ClipDescription("GIF", arrayOf("image/gif")), Uri.parse(link))
                    ok = ic.commitContent(info, InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, null)
                } catch (e: Exception) {
                    ok = false
                }
            }
        }
        if (!ok) {
            ic?.commitText(link, 1)
            Toast.makeText(this, "This app doesn't accept GIFs, so the link was pasted instead.", Toast.LENGTH_SHORT).show()
        }
        return ok
    }

    override fun moveCursor(left: Boolean) {
        sendDownUpKeyEvents(if (left) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT)
    }

    override fun isGridOn(): Boolean = GridOverlayService.running

    override fun setGrid(on: Boolean) {
        if (!on) {
            stopService(Intent(this, GridOverlayService::class.java))
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Allow \"Display over other apps\" for this keyboard, then tap Grid again",
                Toast.LENGTH_LONG
            ).show()
            try {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                // no settings screen available; the toast above explains what to do
            }
            return
        }
        val intent = Intent(this, GridOverlayService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't start Color Grid. Open the app once and try again.", Toast.LENGTH_LONG).show()
        }
    }

    override fun suggestionsAllowed(): Boolean {
        val t = currentInputEditorInfo?.inputType ?: return false
        if ((t and InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT) return false
        if ((t and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) return false
        return when (t and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_URI,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> false
            else -> true
        }
    }

    // ---------- clipboard ----------

    private fun copyToClipboard(text: String) {
        try {
            clipboard.setPrimaryClip(ClipData.newPlainText("Keyboard", text))
        } catch (e: Exception) {
            // Some devices refuse clipboard writes in rare states; insertion still works.
        }
    }

    private fun captureClipboard() {
        if (!store.historyEnabled || !store.captureExternal) return
        try {
            val clip = clipboard.primaryClip ?: return
            if (clip.itemCount == 0) return
            val sensitive = clip.description.extras?.getBoolean("android.content.extra.IS_SENSITIVE", false) == true
            if (sensitive) return
            val text = clip.getItemAt(0).coerceToText(this)?.toString()?.trim() ?: return
            if (text.isNotEmpty() && text.length <= 300) store.addHistory(text)
        } catch (e: Exception) {
            // Clipboard not readable right now; ignore.
        }
    }
}
