package com.colorgrid.keyboard

import android.animation.AnimatorListenerAdapter
import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.graphics.Outline
import android.widget.ImageView
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

private const val MP = ViewGroup.LayoutParams.MATCH_PARENT
private const val WC = ViewGroup.LayoutParams.WRAP_CONTENT

private val LETTER_ROWS = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")

/** Small gray characters in the corner of a key; long-press the key to type them (like Gboard). */
private val LETTER_HINTS = mapOf(
    'q' to "1", 'w' to "2", 'e' to "3", 'r' to "4", 't' to "5", 'y' to "6", 'u' to "7", 'i' to "8", 'o' to "9", 'p' to "0",
    'a' to "@", 's' to "#", 'd' to "&", 'f' to "*", 'g' to "-", 'h' to "+", 'j' to "=", 'k' to "(", 'l' to ")",
    'z' to "_", 'x' to "\$", 'c' to "\"", 'v' to "'", 'b' to ":", 'n' to ";", 'm' to "/"
)

private val SYM1 = listOf("@#\$_&-+()/", "*\"':;!?")
private val SYM2 = listOf("~`|•√π÷×¶∆", "€£¥¢^°={}\\", "©®™✓[]%")
private val PUNCT = listOf(",", "?", "!", "'", "\"", ":", ";", "-")

private val EMOJI_TAB_ICONS = mapOf(
    "Recent" to "🕒", "Smileys & Emotion" to "😀", "People & Body" to "👋", "Animals & Nature" to "🐱",
    "Food & Drink" to "🍔", "Travel & Places" to "🚗", "Activities" to "⚽", "Objects" to "💡",
    "Symbols" to "🔣", "Flags" to "🏁"
)

/** Emoji list from assets/emoji.txt (Unicode order), loaded once per process. */
private object EmojiData {
    private var groups: LinkedHashMap<String, List<String>>? = null
    private val supported = HashMap<String, List<String>>()

    fun groups(context: Context): LinkedHashMap<String, List<String>> {
        groups?.let { return it }
        val map = LinkedHashMap<String, List<String>>()
        try {
            var name: String? = null
            context.assets.open("emoji.txt").bufferedReader().useLines { lines ->
                for (line in lines) {
                    if (line.startsWith("#")) {
                        name = line.substring(1).trim()
                    } else if (line.isNotBlank() && name != null) {
                        map[name!!] = line.trim().split(" ").filter { it.isNotEmpty() }
                    }
                }
            }
        } catch (e: Exception) {
            // fall through with whatever was read
        }
        groups = map
        return map
    }

    /** Only emojis this phone can actually draw (newer ones show as empty boxes on older Android versions). */
    fun supported(context: Context, group: String): List<String> {
        supported[group]?.let { return it }
        val all = groups(context)[group].orEmpty()
        val paint = Paint()
        val ok = all.filter { paint.hasGlyph(it) }
        supported[group] = ok
        return ok
    }
}

@SuppressLint("ClickableViewAccessibility", "ViewConstructor")
class KeyboardView(
    context: Context,
    private val store: Store,
    private val host: KeyboardHost
) : LinearLayout(context) {

    private enum class Page { LETTERS, SYMBOLS1, SYMBOLS2, NUMPAD, CLIPBOARD, EMOJI, CALCULATOR, SEARCH }
    private enum class Shift { OFF, ON, LOCK }
    private enum class Style { LETTER, SPECIAL, ACCENT, ENTER, SPACE }

    private class Key(val cell: FrameLayout, val face: TextView)

    private val pal = Palette.resolve(context, store.themeMode, store.activeTheme)
    private val density = context.resources.displayMetrics.density
    private val cfg = context.resources.configuration
    private val landscape = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE
    private val rowH = dp((if (landscape) 38f else 48f) * store.heightScale)
    private val stripH = dp(if (landscape) 38f else 46f)
    private val keyPageRows = if (store.numberRow) 5 else 4
    private val searchHeaderH = if (landscape) dp(36f) else dp(40f)
    private val searchResH = if (landscape) dp(62f) else dp(100f)
    private val searchRowH = if (landscape) dp(30f) else dp(40f)
    private val searchH = searchHeaderH + searchResH + searchRowH * 4 + dp(14f)
    private val emojiExtra = if (store.emojiRow) dp(40f) else 0
    private val keysH = rowH * keyPageRows + dp(8f) + emojiExtra
    private val calcH = if (landscape) {
        dp(cfg.screenHeightDp.toFloat().times(0.68f).coerceIn(210f, 260f))
    } else {
        dp(cfg.screenHeightDp.toFloat().times(0.52f).coerceIn(340f, 405f))
    }
    private val keyScale = store.keyScale
    private val handler = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val suggester = Suggester(context, store)

    @Suppress("DEPRECATION")
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private var page = Page.LETTERS
    private var shift = Shift.OFF
    private var lastShiftTap = 0L
    private var calcExpression = StringBuilder()
    private var calcResult: String? = null
    private var repeatRunnable: Runnable? = null
    private var emojiCategory = ""

    // suggestion strip state
    private var toolsMode = false
    private var punctMode = false
    private var currentResult: Suggester.Result? = null
    private val slotViews = arrayOfNulls<TextView>(3)
    private var leftButtonIcon: IconView? = null
    private lateinit var leftButton: FrameLayout
    private lateinit var rightButton: FrameLayout
    private lateinit var stripMiddle: LinearLayout
    private val suggestionRefresh = Runnable { refreshSuggestions() }

    // RGB backlight wave (RGB Gamer theme)
    private var keySeq = 0
    private var rgbShift = 0
    private val rgbFaces = ArrayList<Pair<TextView, Int>>()
    private val rgbRunnable = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow) return
            rgbShift = (rgbShift + 8) % 360
            for ((face, idx) in rgbFaces) {
                val bg = face.background
                val g = (if (bg is LayerDrawable) bg.getDrawable(1) else bg) as? GradientDrawable
                g?.setStroke(dp(1.5f), rgbColor(idx))
            }
            handler.postDelayed(this, 70)
        }
    }

    /** The overlay can also be stopped from its own panel / notification; redraw the Grid button when that happens. */
    private val gridListener: () -> Unit = {
        handler.post { if (isAttachedToWindow && (toolsMode || !store.predictEnabled) && isKeyPage(page)) refreshStrip() }
    }

    private val strip = LinearLayout(context)
    private val content = FrameLayout(context)
    private val letterKeys = ArrayList<Pair<TextView, Char>>()
    private var shiftIcon: IconView? = null
    private var shiftFace: TextView? = null
    private var enterFace: TextView? = null
    private var enterIcon: IconView? = null
    private var calcPreview: TextView? = null
    private var calcDisplay: TextView? = null

    // emoji / GIF search page
    private var searchMode = "emoji"
    private val searchQuery = StringBuilder()
    private var searchQueryView: TextView? = null
    private var searchResults: LinearLayout? = null
    private var searchGen = 0
    private val searchRunnable = Runnable { runSearch() }
    private var mascotView: MascotView? = null

    init {
        orientation = VERTICAL
        val bgGrad = pal.bgGradient
        if (pal.texture != Texture.NONE) {
            background = ThemeBackground(bgGrad ?: intArrayOf(pal.bg, pal.bg), pal.texture, pal.dark)
        } else if (bgGrad != null) {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, bgGrad)
        } else {
            setBackgroundColor(pal.bg)
        }
        clipChildren = false
        content.clipChildren = false
        setPadding(dp(4f), 0, dp(4f), 0)
        buildStrip()
        addView(strip, LinearLayout.LayoutParams(MP, stripH))
        addView(content, LinearLayout.LayoutParams(MP, keysH))
        if (Build.VERSION.SDK_INT >= 30) {
            // Keep the keys above the navigation bar. Insets only count the part that overlaps this window, so
            // this adds nothing where the system already places the keyboard above the navigation bar
            // (and covers the Android 15+ edge-to-edge case where it extends behind it).
            setOnApplyWindowInsetsListener { v, insets ->
                val nav = insets.getInsets(WindowInsets.Type.navigationBars()).bottom
                val tap = insets.getInsets(WindowInsets.Type.tappableElement()).bottom
                v.setPadding(dp(4f), 0, dp(4f), maxOf(nav, tap))
                insets
            }
        }
        if (store.sound) VoiceEngine.prepare(context, store.soundConfig())
        showPage(Page.LETTERS)
    }

    private fun startRgb() {
        handler.removeCallbacks(rgbRunnable)
        if (pal.rgb && store.keyEffects && isKeyPage(page)) handler.post(rgbRunnable)
    }

    private fun stopRgb() {
        handler.removeCallbacks(rgbRunnable)
    }

    // ---------- public API used by the service ----------

    fun onInputStarted(numeric: Boolean, caps: Boolean, restarting: Boolean) {
        if (!restarting) {
            toolsMode = false
            punctMode = false
            val target = if (numeric) Page.NUMPAD else Page.LETTERS
            if (page != target) showPage(target)
        }
        if (!restarting || shift != Shift.LOCK) {
            shift = if (caps) Shift.ON else Shift.OFF
        }
        refreshShift()
        refreshEnter()
        scheduleSuggestions()
        startRgb()
    }

    fun onInputFinished() {
        stopRgb()
        suggester.flush()
        if (page != Page.LETTERS) showPage(Page.LETTERS)
    }

    /** The service reports every cursor/text change; keep the suggestions in step with it. */
    fun onSelectionChanged() {
        scheduleSuggestions()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        requestApplyInsets()
        GridOverlayService.listener = gridListener
        startRgb()
    }

    override fun onDetachedFromWindow() {
        if (GridOverlayService.listener === gridListener) GridOverlayService.listener = null
        stopRepeat()
        stopRgb()
        handler.removeCallbacks(suggestionRefresh)
        suggester.flush()
        super.onDetachedFromWindow()
    }

    // ---------- helpers ----------

    private fun dp(v: Float): Int = (v * density).roundToInt()

    private fun rounded(color: Int, radiusDp: Float, strokeColor: Int? = null): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = dp(radiusDp).toFloat()
        d.setColor(color)
        if (strokeColor != null && strokeColor != 0) d.setStroke(dp(1f), strokeColor)
        return d
    }

    private fun blend(a: Int, b: Int, t: Float): Int = Color.argb(
        Color.alpha(a),
        (Color.red(a) * (1 - t) + Color.red(b) * t).toInt(),
        (Color.green(a) * (1 - t) + Color.green(b) * t).toInt(),
        (Color.blue(a) * (1 - t) + Color.blue(b) * t).toInt()
    )

    private fun pressed(base: Int): Int {
        val c = blend(base, if (pal.dark) Color.WHITE else Color.BLACK, 0.14f)
        return if (Color.alpha(c) < 255) {
            Color.argb(minOf(255, Color.alpha(c) + 70), Color.red(c), Color.green(c), Color.blue(c))
        } else c
    }

    private fun baseFill(style: Style): Int = when (style) {
        Style.LETTER -> pal.key
        Style.SPECIAL -> pal.special
        Style.ACCENT -> pal.accent
        Style.ENTER -> pal.enterBg
        Style.SPACE -> pal.key
    }

    /** Rainbow backlight color of key number [idx]; [rgbShift] moves along while the wave runs. */
    private fun rgbColor(idx: Int): Int =
        Color.HSVToColor(floatArrayOf(((idx * 14 + rgbShift) % 360).toFloat(), 0.85f, 1f))

    /**
     * Background of a key. [fill] overrides the key color (used by the press glow), [idx] is the key's
     * position (used for its RGB outline color). Keycap themes get a darker lip underneath that shrinks when pressed.
     */
    private fun keyBg(style: Style, isPressed: Boolean, fill: Int? = null, idx: Int = 0): Drawable {
        val r = dp(pal.radius).toFloat()
        val strokeColor = when {
            pal.rgb -> rgbColor(idx)
            style == Style.ACCENT -> 0
            else -> pal.keyStroke
        }
        val top = GradientDrawable()
        if (style == Style.SPACE) {
            val from = if (isPressed) pressed(pal.spaceFrom) else pal.spaceFrom
            val to = if (isPressed) pressed(pal.spaceTo) else pal.spaceTo
            top.orientation = GradientDrawable.Orientation.LEFT_RIGHT
            top.colors = intArrayOf(from, to)
        } else {
            val base = fill ?: baseFill(style)
            top.setColor(if (isPressed && fill == null) pressed(base) else base)
        }
        top.cornerRadius = r
        if (strokeColor != 0) top.setStroke(if (pal.rgb) dp(if (isPressed) 2.5f else 1.5f) else dp(1f), strokeColor)
        if (!pal.keycap) return top

        val lip = GradientDrawable()
        lip.cornerRadius = r
        lip.setColor(if (style == Style.ACCENT) blend(pal.accent, Color.BLACK, 0.4f) else pal.lip)
        val layers = LayerDrawable(arrayOf<Drawable>(lip, top))
        layers.setLayerInset(1, 0, 0, 0, dp(if (isPressed) 1f else 3f))
        return layers
    }

    private fun fxKind(): String =
        if (!store.keyEffects) "none" else store.pressEffect.let { if (it == "auto") pal.fx else it }

    private fun fxColor(idx: Int): Int = when {
        pal.rgb || pal.letterRainbow -> rgbColor(idx)
        pal.glow != 0 -> pal.glow
        else -> pal.accent
    }

    private fun rainbowText(idx: Int): Int =
        Color.HSVToColor(floatArrayOf(((idx % 10) * 36).toFloat(), 0.6f, 1f))

    private fun keyLabelColor(style: Style, idx: Int): Int = when {
        style == Style.ACCENT -> pal.accentText
        pal.letterRainbow -> rainbowText(idx)
        style == Style.LETTER -> pal.keyText
        else -> pal.modText
    }

    /** Ripple / halo / sparks drawn over the pressed key. */
    private fun startFx(cell: FrameLayout, idx: Int, x: Float, y: Float) {
        val kind = fxKind()
        if (kind != "ripple" && kind != "halo" && kind != "sparks") return
        if (cell.width <= 0 || cell.height <= 0) return
        val fx = PressFx(kind, fxColor(idx), density, x, y, cell.width, cell.height, dp(pal.radius).toFloat(), dp(3f).toFloat())
        fx.setBounds(0, 0, cell.width, cell.height)
        cell.overlay.add(fx)
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 420
        a.addUpdateListener {
            fx.progress = it.animatedFraction
            cell.invalidate()
        }
        a.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                cell.overlay.remove(fx)
                cell.invalidate()
            }
        })
        a.start()
    }

    // ---------- key preview bubble ----------

    private var bubble: PreviewBubble? = null
    private val hideBubble = Runnable { removeBubble() }

    private fun removeBubble() {
        bubble?.let { overlay.remove(it) }
        bubble = null
        invalidate()
    }

    private fun showPreview(cell: View, face: TextView, style: Style, idx: Int) {
        if (!store.keyPreviews || style != Style.LETTER) return
        val label = face.text?.toString().orEmpty()
        if (label.isEmpty() || label.length > 2) return
        handler.removeCallbacks(hideBubble)
        removeBubble()
        val cl = IntArray(2)
        val rl = IntArray(2)
        cell.getLocationInWindow(cl)
        getLocationInWindow(rl)
        val bw = (cell.width * 1.2f).toInt().coerceAtLeast(dp(40f))
        val bh = dp(52f)
        var left = cl[0] - rl[0] + cell.width / 2 - bw / 2
        left = left.coerceIn(0, (width - bw).coerceAtLeast(0))
        val bottom = cl[1] - rl[1] + dp(6f)
        val top = (bottom - bh).coerceAtLeast(0)
        val fill = pal.key.let { Color.argb(255, Color.red(it), Color.green(it), Color.blue(it)) }
        val b = PreviewBubble(label, fill, pal.stroke, keyLabelColor(Style.LETTER, idx), density)
        b.setBounds(left, top, left + bw, top + bh)
        overlay.add(b)
        bubble = b
        invalidate()
    }

    private fun hidePreviewSoon() {
        handler.removeCallbacks(hideBubble)
        handler.postDelayed(hideBubble, 70)
    }

    /** Short glow of the key color when a key is released (themes with a glow color, or RGB). */
    private fun flashKey(face: TextView, style: Style, idx: Int, setAnim: (ValueAnimator?) -> Unit) {
        if (style == Style.SPACE || fxKind() != "glow") return
        val glow = fxColor(idx)
        val base = baseFill(style)
        val from = blend(Color.argb(255, Color.red(base), Color.green(base), Color.blue(base)), glow, 0.8f)
        val anim = ValueAnimator.ofObject(ArgbEvaluator(), from, base)
        anim.duration = 320
        anim.addUpdateListener { face.background = keyBg(style, false, it.animatedValue as Int, idx) }
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                face.background = keyBg(style, false, null, idx)
            }
        })
        setAnim(anim)
        anim.start()
    }

    private fun feedback(fx: Int) {
        if (store.sound) {
            val category = when (fx) {
                AudioManager.FX_KEYPRESS_SPACEBAR -> "space"
                AudioManager.FX_KEYPRESS_RETURN -> "enter"
                AudioManager.FX_KEYPRESS_DELETE -> "delete"
                else -> "letter"
            }
            VoiceEngine.play(context, store.soundConfig(), category)
        }
        if (store.vibrate && store.hapticStrength > 0) {
            val heavy = fx == AudioManager.FX_KEYPRESS_SPACEBAR || fx == AudioManager.FX_KEYPRESS_RETURN
            Haptics.play(vibrator, Haptics.resolve(store.hapticPattern, store.activeTheme), store.hapticStrength, heavy)
        }
    }

    private fun pressAnim(v: View) {
        v.setOnTouchListener { view, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(60).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    view.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
            }
            false
        }
    }

    private fun startRepeat(action: () -> Unit) {
        stopRepeat()
        val r = object : Runnable {
            override fun run() {
                action()
                handler.postDelayed(this, 45)
            }
        }
        repeatRunnable = r
        handler.postDelayed(r, 380)
    }

    private fun stopRepeat() {
        repeatRunnable?.let { handler.removeCallbacks(it) }
        repeatRunnable = null
    }

    private fun isKeyPage(p: Page) = p == Page.LETTERS || p == Page.SYMBOLS1 || p == Page.SYMBOLS2

    // ---------- suggestion strip (top row) ----------

    private fun stripButton(icon: IconView): FrameLayout {
        val b = FrameLayout(context)
        b.background = rounded(pal.chip, 12f, pal.keyStroke)
        b.addView(icon, FrameLayout.LayoutParams(dp(22f), dp(22f), Gravity.CENTER))
        pressAnim(b)
        return b
    }

    private fun buildStrip() {
        strip.orientation = HORIZONTAL
        strip.gravity = Gravity.CENTER_VERTICAL
        strip.setPadding(dp(2f), dp(3f), dp(2f), dp(3f))

        val leftIcon = IconView(context, IconView.Kind.GRID, pal.accent)
        leftButtonIcon = leftIcon
        leftButton = stripButton(leftIcon)
        leftButton.setOnClickListener {
            feedback(AudioManager.FX_KEYPRESS_STANDARD)
            toolsMode = !toolsMode
            punctMode = false
            refreshStrip()
            scheduleSuggestions()
        }
        rightButton = stripButton(IconView(context, IconView.Kind.CLIPBOARD, pal.subtext))
        rightButton.setOnClickListener {
            feedback(AudioManager.FX_KEYPRESS_STANDARD)
            showPage(Page.CLIPBOARD)
        }
        stripMiddle = LinearLayout(context)
        stripMiddle.orientation = HORIZONTAL
        stripMiddle.gravity = Gravity.CENTER_VERTICAL

        val bw = dp(44f)
        strip.addView(leftButton, LinearLayout.LayoutParams(bw, MP).apply { setMargins(dp(2f), 0, dp(2f), 0) })
        strip.addView(stripMiddle, LinearLayout.LayoutParams(0, MP, 1f))
        strip.addView(rightButton, LinearLayout.LayoutParams(bw, MP).apply { setMargins(dp(2f), 0, dp(2f), 0) })
    }

    private fun toolCell(content: View, size: Int = WC, onClick: () -> Unit): View {
        val cell = FrameLayout(context)
        cell.addView(content, FrameLayout.LayoutParams(size, size, Gravity.CENTER))
        cell.setOnClickListener {
            feedback(AudioManager.FX_KEYPRESS_STANDARD)
            onClick()
        }
        pressAnim(cell)
        return cell
    }

    private fun emojiText(s: String): TextView {
        val t = TextView(context)
        t.text = s
        t.textSize = 21f
        t.gravity = Gravity.CENTER
        t.includeFontPadding = false
        return t
    }

    private fun refreshStrip() {
        val predict = store.predictEnabled
        leftButton.visibility = if (predict) View.VISIBLE else View.GONE
        rightButton.visibility = if (predict) View.VISIBLE else View.GONE
        leftButtonIcon?.let {
            // "X" while the tools are open, the 4-square icon otherwise
            val kind = if (toolsMode) IconView.Kind.CLOSE else IconView.Kind.GRID
            if (it.kind != kind) {
                it.kind = kind
                it.invalidate()
            }
        }
        stripMiddle.removeAllViews()
        slotViews.fill(null)

        if (punctMode) {
            for (p in PUNCT) {
                val t = TextView(context)
                t.text = p
                t.textSize = 22f
                t.gravity = Gravity.CENTER
                t.setTextColor(pal.text)
                t.background = rounded(pal.chip, 10f)
                t.setOnClickListener {
                    feedback(AudioManager.FX_KEYPRESS_STANDARD)
                    punctMode = false
                    typeChar(p)
                    refreshStrip()
                }
                pressAnim(t)
                stripMiddle.addView(t, LinearLayout.LayoutParams(0, MP, 1f).apply { setMargins(dp(2f), 0, dp(2f), 0) })
            }
            return
        }

        if (predict && !toolsMode) {
            for (i in 0 until 3) {
                if (i > 0) {
                    val d = View(context)
                    d.setBackgroundColor(Color.argb(90, Color.red(pal.subtext), Color.green(pal.subtext), Color.blue(pal.subtext)))
                    stripMiddle.addView(d, LinearLayout.LayoutParams(dp(1f), dp(22f)))
                }
                val t = TextView(context)
                t.textSize = 17f
                t.gravity = Gravity.CENTER
                t.maxLines = 1
                t.ellipsize = TextUtils.TruncateAt.END
                t.setTextColor(pal.text)
                t.setPadding(dp(4f), 0, dp(4f), 0)
                val idx = i
                t.setOnClickListener { onSuggestionTap(idx) }
                slotViews[i] = t
                stripMiddle.addView(t, LinearLayout.LayoutParams(0, MP, 1f))
            }
            return
        }

        // tools: calculator, clipboard (only when the right-hand button is hidden), settings, hide keyboard
        val items = ArrayList<View>()
        items.add(toolCell(emojiText("🧮")) { showPage(Page.CALCULATOR) })
        items.add(gridToggleCell())
        if (!predict) {
            items.add(toolCell(IconView(context, IconView.Kind.CLIPBOARD, pal.text), dp(24f)) { showPage(Page.CLIPBOARD) })
        }
        items.add(toolCell(emojiText("⚙️")) { host.openApp("settings") })
        items.add(toolCell(IconView(context, IconView.Kind.CHEVRON_DOWN, pal.text), dp(24f)) { host.hideKeyboard() })
        for (v in items) stripMiddle.addView(v, LinearLayout.LayoutParams(0, MP, 1f))
    }

    /** "Grid" pill: highlighted while the floating Color Grid overlay is on (default: off). */
    private fun gridToggleCell(): View {
        val on = host.isGridOn()
        val label = TextView(context)
        label.text = "Grid"
        label.textSize = 14f
        label.typeface = Typeface.DEFAULT_BOLD
        label.gravity = Gravity.CENTER
        label.includeFontPadding = false
        label.setPadding(dp(12f), 0, dp(12f), 0)
        label.setTextColor(if (on) pal.accentText else pal.text)
        label.background = rounded(if (on) pal.accent else pal.chip, 14f, if (on) null else pal.keyStroke)
        val cell = FrameLayout(context)
        cell.addView(label, FrameLayout.LayoutParams(WC, dp(32f), Gravity.CENTER))
        cell.setOnClickListener {
            feedback(AudioManager.FX_KEYPRESS_STANDARD)
            host.setGrid(!host.isGridOn())
            // the button redraws itself through gridListener once the overlay really starts/stops
        }
        pressAnim(cell)
        return cell
    }

    private fun scheduleSuggestions() {
        handler.removeCallbacks(suggestionRefresh)
        handler.postDelayed(suggestionRefresh, 25)
    }

    private fun refreshSuggestions() {
        if (!store.predictEnabled || toolsMode || punctMode || !isKeyPage(page)) return
        if (slotViews[0] == null) return
        if (!host.suggestionsAllowed()) {
            currentResult = null
            slotViews.forEach { it?.text = "" }
            return
        }
        val before = host.textBeforeCursor(60)
        val r = suggester.suggest(before, host.cursorCaps())
        currentResult = r
        for (i in 0 until 3) slotViews[i]?.text = r.slots[i]
    }

    private fun onSuggestionTap(i: Int) {
        val r = currentResult ?: return
        val word = r.slots.getOrNull(i).orEmpty()
        if (word.isEmpty()) return
        feedback(AudioManager.FX_KEYPRESS_STANDARD)
        if (host.suggestionsAllowed()) {
            val before = host.textBeforeCursor(60)
            if (before.length >= r.prefix.length) {
                suggester.learn(before.substring(0, before.length - r.prefix.length) + word)
            }
        }
        host.replaceBeforeCursor(r.prefix.length, "$word ")
        afterTyped()
    }

    // ---------- page switching ----------

    private fun showPage(p: Page) {
        page = p
        stopRepeat()
        stopRgb()
        punctMode = false
        content.removeAllViews()
        letterKeys.clear()
        rgbFaces.clear()
        keySeq = 0
        shiftIcon = null
        shiftFace = null
        enterFace = null
        enterIcon = null
        calcPreview = null
        calcDisplay = null
        mascotView = null
        searchQueryView = null
        searchResults = null
        handler.removeCallbacks(searchRunnable)

        val v: View = when (p) {
            Page.LETTERS, Page.SYMBOLS1, Page.SYMBOLS2 -> buildKeys(p)
            Page.NUMPAD -> buildNumpad()
            Page.CLIPBOARD -> buildClipboardPanel()
            Page.EMOJI -> buildEmojiPanel()
            Page.CALCULATOR -> buildCalculatorPanel()
            Page.SEARCH -> buildSearchPanel()
        }
        val keyPage = isKeyPage(p)
        strip.visibility = if (keyPage) View.VISIBLE else View.GONE
        val h = when (p) {
            Page.CALCULATOR -> calcH
            Page.SEARCH -> searchH
            Page.LETTERS, Page.SYMBOLS1, Page.SYMBOLS2 -> keysH
            else -> keysH + stripH
        }
        content.setPadding(0, dp(4f), 0, dp(4f))
        content.layoutParams = content.layoutParams.apply { height = h }
        // Keep page rendering immediate. Animating the whole keyboard content can
        // look like a flash/flicker when Android restarts the input view.
        content.addView(v, FrameLayout.LayoutParams(MP, MP))

        refreshShift()
        refreshEnter()
        if (keyPage) {
            refreshStrip()
            scheduleSuggestions()
            startRgb()
        }
    }

    // ---------- typing keyboard ----------

    private fun newKey(
        label: String,
        weight: Float,
        style: Style,
        sizeSp: Float = 22f,
        repeat: Boolean = false,
        fx: Int = AudioManager.FX_KEYPRESS_STANDARD,
        hint: String? = null,
        onLong: (() -> Unit)? = null,
        action: () -> Unit
    ): Key {
        val cell = FrameLayout(context)
        val face = TextView(context)
        val idx = keySeq++
        face.tag = idx
        face.text = label
        face.gravity = Gravity.CENTER
        face.includeFontPadding = false
        face.typeface = Typeface.DEFAULT
        face.textSize = sizeSp * keyScale
        face.setTextColor(keyLabelColor(style, idx))
        face.background = keyBg(style, false, null, idx)
        // keycap themes: keep the label on the key's top surface, above the lip
        if (pal.keycap) face.setPadding(0, 0, 0, dp(3f))
        if (pal.rgb) rgbFaces.add(Pair(face, idx))

        val m = (dp(3f) + (1f - keyScale) * dp(6f)).roundToInt().coerceAtLeast(dp(1f))
        val lp = FrameLayout.LayoutParams(MP, MP)
        lp.setMargins(m, m, m, m)
        cell.addView(face, lp)
        if (hint != null) {
            val h = TextView(context)
            h.text = hint
            h.textSize = 10f
            h.includeFontPadding = false
            val hintBase = baseFill(style).let { Color.argb(255, Color.red(it), Color.green(it), Color.blue(it)) }
            h.setTextColor(blend(keyLabelColor(style, idx), hintBase, 0.45f))
            val hl = FrameLayout.LayoutParams(WC, WC, Gravity.TOP or Gravity.END)
            hl.setMargins(0, m + dp(3f), m + dp(6f), 0)
            cell.addView(h, hl)
        }
        cell.layoutParams = LinearLayout.LayoutParams(0, MP, weight)

        var longFired = false
        var flashAnim: ValueAnimator? = null
        val longRunnable = Runnable {
            longFired = true
            onLong?.invoke()
        }
        cell.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    longFired = false
                    flashAnim?.cancel()
                    face.background = keyBg(style, true, null, idx)
                    startFx(cell, idx, ev.x, ev.y)
                    showPreview(cell, face, style, idx)
                    feedback(fx)
                    if (repeat) {
                        action()
                        startRepeat(action)
                    }
                    if (onLong != null) handler.postDelayed(longRunnable, 340)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (onLong != null && (ev.x < 0f || ev.x > v.width || ev.y < 0f || ev.y > v.height)) {
                        handler.removeCallbacks(longRunnable)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    face.background = keyBg(style, false, null, idx)
                    flashKey(face, style, idx) { flashAnim = it }
                    hidePreviewSoon()
                    stopRepeat()
                    handler.removeCallbacks(longRunnable)
                    val inside = ev.x >= 0f && ev.x <= v.width && ev.y >= 0f && ev.y <= v.height
                    if (!repeat && !longFired && inside) action()
                }
                MotionEvent.ACTION_CANCEL -> {
                    face.background = keyBg(style, false, null, idx)
                    hidePreviewSoon()
                    stopRepeat()
                    handler.removeCallbacks(longRunnable)
                }
            }
            true
        }
        return Key(cell, face)
    }

    private fun addIcon(key: Key, icon: IconView) {
        key.cell.addView(icon, FrameLayout.LayoutParams(dp(24f), dp(24f), Gravity.CENTER))
    }

    private fun charKey(s: String, weight: Float = 1f): Key = newKey(s, weight, Style.LETTER) { typeChar(s) }

    private fun letterKey(c: Char): Key {
        val hint = if (store.hintSymbols && !(store.numberRow && c in "qwertyuiop")) LETTER_HINTS[c] else null
        val key = newKey(
            c.toString(), 1f, Style.LETTER, hint = hint,
            onLong = if (hint != null) ({ typeChar(hint) }) else null
        ) {
            typeChar(if (shift == Shift.OFF) c.toString() else c.uppercaseChar().toString())
        }
        letterKeys.add(Pair(key.face, c))
        return key
    }

    private fun backspaceKey(style: Style = Style.ACCENT, weight: Float = 1.5f): Key {
        val key = newKey("", weight, style, repeat = true, fx = AudioManager.FX_KEYPRESS_DELETE) {
            host.backspace()
            afterTyped()
        }
        addIcon(key, IconView(context, IconView.Kind.BACKSPACE, if (style == Style.ACCENT) pal.accentText else pal.modText))
        return key
    }

    private fun typeChar(s: String) {
        if (s.length == 1) {
            val c = s[0]
            if (c.isWhitespace() || (!c.isLetterOrDigit() && c != '\'' && c != '\u2019')) learnBeforeSeparator()
        }
        host.typeText(s)
        afterTyped()
    }

    private fun learnBeforeSeparator() {
        if (!store.predictEnabled || !store.learnWords) return
        if (!host.suggestionsAllowed()) return
        suggester.learn(host.textBeforeCursor(60))
    }

    private fun afterTyped() {
        mascotView?.bounce()
        updateAutoShift(host.cursorCaps())
        if (punctMode) {
            punctMode = false
            refreshStrip()
        }
        scheduleSuggestions()
    }

    private fun updateAutoShift(caps: Boolean) {
        if (shift == Shift.LOCK) return
        val next = if (caps) Shift.ON else Shift.OFF
        if (next != shift) {
            shift = next
            refreshShift()
        }
    }

    private fun onShiftTap() {
        val now = System.currentTimeMillis()
        shift = when {
            shift == Shift.LOCK -> Shift.OFF
            shift == Shift.ON && now - lastShiftTap < 350 -> Shift.LOCK
            shift == Shift.ON -> Shift.OFF
            else -> Shift.ON
        }
        lastShiftTap = now
        refreshShift()
    }

    private fun refreshShift() {
        for ((tv, c) in letterKeys) {
            tv.text = if (shift == Shift.OFF && !pal.capsLabels) c.toString() else c.uppercaseChar().toString()
        }
        shiftIcon?.let {
            it.filled = shift != Shift.OFF
            it.locked = shift == Shift.LOCK
            it.tint = if (shift != Shift.OFF) pal.accentText else pal.modText
            it.invalidate()
        }
        shiftFace?.let {
            it.background = keyBg(if (shift == Shift.OFF) Style.SPECIAL else Style.ACCENT, false, null, (it.tag as? Int) ?: 0)
        }
    }

    private fun refreshEnter() {
        val face = enterFace ?: return
        val icon = enterIcon ?: return
        val label = host.enterLabel()
        if (label == null) {
            face.text = ""
            icon.visibility = View.VISIBLE
        } else {
            face.text = label
            face.textSize = 15f * keyScale
            icon.visibility = View.GONE
        }
    }

    private fun enterKey(weight: Float): Key {
        val enter = newKey("", weight, Style.ENTER, sizeSp = 15f, fx = AudioManager.FX_KEYPRESS_RETURN) {
            learnBeforeSeparator()
            host.enter()
            afterTyped()
        }
        val eIcon = IconView(context, IconView.Kind.ENTER, pal.modText)
        addIcon(enter, eIcon)
        enterFace = enter.face
        enterIcon = eIcon
        return enter
    }

    private fun buildKeys(p: Page): View {
        val col = LinearLayout(context)
        col.orientation = VERTICAL
        val letters = p == Page.LETTERS
        col.clipChildren = false
        val rowsOnPage = if (letters) keyPageRows else 4
        val rh = (rowH * keyPageRows + (if (letters) 0 else emojiExtra)) / rowsOnPage

        fun addRow(keys: List<Key>, spacerWeight: Float = 0f) {
            val row = LinearLayout(context)
            row.orientation = HORIZONTAL
            row.clipChildren = false
            if (spacerWeight > 0f) row.addView(View(context), LinearLayout.LayoutParams(0, MP, spacerWeight))
            keys.forEach { row.addView(it.cell) }
            if (spacerWeight > 0f) row.addView(View(context), LinearLayout.LayoutParams(0, MP, spacerWeight))
            col.addView(row, LinearLayout.LayoutParams(MP, rh))
        }

        if (letters && store.emojiRow) col.addView(buildEmojiRow(), LinearLayout.LayoutParams(MP, emojiExtra))
        if (letters) {
            if (store.numberRow) addRow("1234567890".map { charKey(it.toString()) })
            addRow(LETTER_ROWS[0].map { letterKey(it) })
            addRow(LETTER_ROWS[1].map { letterKey(it) }, 0.5f)
            val shiftKey = newKey("", 1.5f, Style.SPECIAL) { onShiftTap() }
            val sIcon = IconView(context, IconView.Kind.SHIFT, pal.modText)
            addIcon(shiftKey, sIcon)
            shiftIcon = sIcon
            shiftFace = shiftKey.face
            addRow(listOf(shiftKey) + LETTER_ROWS[2].map { letterKey(it) } + listOf(backspaceKey()))
        } else if (p == Page.SYMBOLS1) {
            addRow("1234567890".map { charKey(it.toString()) })
            addRow(SYM1[0].map { charKey(it.toString()) })
            val toggle = newKey("=\\<", 1.5f, Style.SPECIAL, sizeSp = 15f) { showPage(Page.SYMBOLS2) }
            addRow(listOf(toggle) + SYM1[1].map { charKey(it.toString()) } + listOf(backspaceKey()))
        } else {
            addRow(SYM2[0].map { charKey(it.toString()) })
            addRow(SYM2[1].map { charKey(it.toString()) })
            val toggle = newKey("?123", 1.5f, Style.SPECIAL, sizeSp = 15f) { showPage(Page.SYMBOLS1) }
            addRow(listOf(toggle) + SYM2[2].map { charKey(it.toString()) } + listOf(backspaceKey()))
        }

        val modeKey = newKey(if (letters) "123" else "ABC", 1.4f, Style.SPECIAL, sizeSp = 16f) {
            showPage(if (letters) Page.SYMBOLS1 else Page.LETTERS)
        }
        val emojiKey = newKey("", 1.2f, Style.SPECIAL) { showPage(Page.EMOJI) }
        addIcon(emojiKey, IconView(context, IconView.Kind.SMILEY, pal.modText))
        val arrows = store.arrowKeys
        // Long-press the space bar to switch keyboards (the globe key of other keyboards)
        val space = newKey(
            "", if (arrows) 3.0f else 4.8f, Style.SPACE, fx = AudioManager.FX_KEYPRESS_SPACEBAR,
            onLong = { host.switchKeyboard() }
        ) { typeChar(" ") }
        val mascot = KeyboardThemes.mascotFor(store.activeTheme)
        if (store.spacebarMascot && mascot.isNotEmpty()) {
            val size = minOf(dp(32f), (rowH * 0.66f).toInt())
            val mv = MascotView(context, mascot, pal.accent)
            space.cell.addView(mv, FrameLayout.LayoutParams(size, size, Gravity.END or Gravity.CENTER_VERTICAL).apply {
                marginEnd = dp(12f)
            })
            mascotView = mv
        }
        val period = newKey(
            ".", 1.1f, Style.LETTER,
            onLong = {
                punctMode = true
                refreshStrip()
            }
        ) { typeChar(".") }
        if (arrows) {
            val left = newKey("\u2039", 0.9f, Style.SPECIAL, sizeSp = 24f, repeat = true) { host.moveCursor(true) }
            val right = newKey("\u203A", 0.9f, Style.SPECIAL, sizeSp = 24f, repeat = true) { host.moveCursor(false) }
            addRow(listOf(modeKey, emojiKey, left, space, right, period, enterKey(1.5f)))
        } else {
            addRow(listOf(modeKey, emojiKey, space, period, enterKey(1.5f)))
        }
        return col
    }

    /** "Emoji row": the emoji you used last, one tap to type. */
    private fun buildEmojiRow(): View {
        val row = LinearLayout(context)
        row.orientation = HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.clipChildren = false
        val recent = store.loadRecentEmoji()
        val defaults = listOf("😀", "😂", "❤️", "👍", "🙏", "😭", "🔥", "✨", "🥺", "😍", "🎉", "😊")
        val paint = Paint()
        val list = (recent + defaults.filter { it !in recent }).filter { paint.hasGlyph(it) }.take(9)
        for (e in list) {
            val t = emojiText(e)
            t.setOnClickListener {
                feedback(AudioManager.FX_KEYPRESS_STANDARD)
                host.typeText(e)
                store.addRecentEmoji(e)
            }
            pressAnim(t)
            row.addView(t, LinearLayout.LayoutParams(0, MP, 1f))
        }
        return row
    }

    // ---------- number pad (number / phone fields) ----------

    private fun buildNumpad(): View {
        val col = LinearLayout(context)
        col.orientation = VERTICAL

        fun row(vararg keys: Key) {
            val r = LinearLayout(context)
            r.orientation = HORIZONTAL
            keys.forEach { r.addView(it.cell) }
            col.addView(r, LinearLayout.LayoutParams(MP, 0, 1f))
        }

        fun digit(s: String, hint: String? = null): Key = newKey(
            s, 1f, Style.LETTER, sizeSp = 26f, hint = hint,
            onLong = if (hint != null) ({ typeChar(hint) }) else null
        ) { typeChar(s) }

        row(digit("1"), digit("2"), digit("3"), backspaceKey(Style.ACCENT, 1f))
        row(digit("4"), digit("5"), digit("6"), newKey("?", 1f, Style.SPECIAL, sizeSp = 24f) { typeChar("?") })
        row(digit("7"), digit("8"), digit("9"), newKey("\$", 1f, Style.SPECIAL, sizeSp = 24f) { typeChar("\$") })
        row(
            newKey("ABC", 1f, Style.SPECIAL, sizeSp = 16f) { showPage(Page.LETTERS) },
            digit("0", "-"),
            digit(".", ","),
            enterKey(1f)
        )
        return col
    }

    // ---------- shared panel widgets ----------

    private fun styleTab(tv: TextView, selected: Boolean) {
        tv.background = rounded(if (selected) pal.accent else pal.chip, 16f, if (selected) null else pal.keyStroke)
        tv.setTextColor(if (selected) pal.accentText else pal.text)
    }

    private fun pill(label: String, selected: Boolean = false, onClick: () -> Unit): TextView {
        val tv = TextView(context)
        tv.text = label
        tv.textSize = 13.5f
        tv.gravity = Gravity.CENTER
        tv.includeFontPadding = false
        tv.setPadding(dp(14f), 0, dp(14f), 0)
        styleTab(tv, selected)
        tv.setOnClickListener { onClick() }
        pressAnim(tv)
        return tv
    }

    private fun headerLp(): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(WC, MP)
        lp.setMargins(dp(3f), dp(5f), dp(3f), dp(5f))
        return lp
    }

    private fun headerRow(): LinearLayout {
        val h = LinearLayout(context)
        h.orientation = HORIZONTAL
        h.gravity = Gravity.CENTER_VERTICAL
        return h
    }

    private fun panelRoot(): LinearLayout {
        val r = LinearLayout(context)
        r.orientation = VERTICAL
        r.setPadding(dp(6f), 0, dp(6f), 0)
        return r
    }

    private fun emptyText(msg: String): TextView {
        val t = TextView(context)
        t.text = msg
        t.textSize = 14f
        t.gravity = Gravity.CENTER
        t.setTextColor(pal.subtext)
        t.setPadding(dp(24f), dp(28f), dp(24f), dp(28f))
        return t
    }

    // ---------- Calculator panel ----------

    private fun lipDrawable(top: Int, lip: Int, radiusDp: Float): LayerDrawable {
        val l = LayerDrawable(arrayOf(rounded(lip, radiusDp), rounded(top, radiusDp)))
        l.setLayerInset(1, 0, 0, 0, dp(3f))
        return l
    }

    private fun refreshCalc() {
        calcPreview?.text = calcExpression.toString().ifEmpty { " " }
        calcDisplay?.text = calcResult ?: evaluateCalculator(calcExpression.toString()) ?: "0"
    }

    private fun calcPress(label: String) {
        when (label) {
            "AC" -> { calcExpression.clear(); calcResult = null }
            "⌫" -> {
                if (calcResult != null) calcResult = null
                else if (calcExpression.isNotEmpty()) calcExpression.deleteCharAt(calcExpression.lastIndex)
            }
            "=" -> {
                val value = evaluateCalculator(calcExpression.toString())
                calcResult = value ?: "Error"
                if (value != null) calcExpression = StringBuilder(value)
            }
            else -> {
                if (calcResult != null) {
                    if (label !in setOf("+", "−", "×", "÷")) calcExpression.clear()
                    calcResult = null
                }
                val token = when (label) {
                    "÷" -> "/"; "×" -> "*"; "−" -> "-"; else -> label
                }
                if (token == ".") {
                    val start = calcExpression.indexOfLast { it in "+-*/" } + 1
                    val current = calcExpression.substring(start)
                    if ("." in current) return
                    if (current.isEmpty()) calcExpression.append("0")
                }
                if (token == "%") {
                    if (calcExpression.isNotEmpty() && calcExpression.last().isDigit()) calcExpression.append('%')
                } else if (token in listOf("+", "-", "*", "/")) {
                    if (calcExpression.isEmpty() && token != "-") return
                    if (calcExpression.isNotEmpty() && calcExpression.last() in "+-*/") {
                        calcExpression.setCharAt(calcExpression.lastIndex, token[0])
                    } else calcExpression.append(token)
                } else calcExpression.append(token)
            }
        }
        refreshCalc()
    }

    private fun calcKey(label: String): View {
        val util = label == "⌫" || label == "AC"
        val op = label in setOf("%", "÷", "×", "−", "+")
        val eq = label == "="
        val cell = FrameLayout(context)
        cell.background = when {
            eq -> lipDrawable(pal.calcEq, pal.calcEqLip, 14f)
            util -> rounded(pal.calcUtil, 14f)
            op -> rounded(pal.calcOp, 14f)
            else -> rounded(pal.calcKey, 14f)
        }
        if (label == "⌫") {
            cell.addView(
                IconView(context, IconView.Kind.BACKSPACE, pal.calcUtilText),
                FrameLayout.LayoutParams(dp(26f), dp(26f), Gravity.CENTER)
            )
        } else {
            val tv = TextView(context)
            tv.text = label
            tv.textSize = if (label == "AC") 18f else if (op || eq) 24f else 23f
            tv.gravity = Gravity.CENTER
            tv.includeFontPadding = false
            tv.setTextColor(
                when {
                    eq -> Color.WHITE
                    util -> pal.calcUtilText
                    op -> pal.calcOpText
                    else -> pal.text
                }
            )
            if (eq) tv.setPadding(0, 0, 0, dp(3f))
            cell.addView(tv, FrameLayout.LayoutParams(MP, MP))
        }
        val repeats = label == "⌫"
        cell.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.7f
                    feedback(if (repeats) AudioManager.FX_KEYPRESS_DELETE else AudioManager.FX_KEYPRESS_STANDARD)
                    if (repeats) {
                        calcPress(label)
                        startRepeat { calcPress(label) }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    v.alpha = 1f
                    stopRepeat()
                    val inside = ev.x >= 0f && ev.x <= v.width && ev.y >= 0f && ev.y <= v.height
                    if (!repeats && inside) calcPress(label)
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1f
                    stopRepeat()
                }
            }
            true
        }
        return cell
    }

    private fun buildCalculatorPanel(): View {
        val root = LinearLayout(context)
        root.orientation = if (landscape) HORIZONTAL else VERTICAL
        root.setPadding(dp(8f), dp(2f), dp(8f), dp(4f))

        // display: expression preview on top, big green result below
        val displayBox = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(pal.calcDisplay, 16f)
            setPadding(dp(16f), dp(4f), dp(16f), dp(4f))
        }
        val preview = TextView(context).apply {
            text = calcExpression.toString().ifEmpty { " " }
            textSize = 14f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.START
            setTextColor(pal.subtext)
        }
        val display = TextView(context).apply {
            text = calcResult ?: evaluateCalculator(calcExpression.toString()) ?: "0"
            textSize = 32f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(pal.calcResult)
            typeface = Typeface.DEFAULT_BOLD
        }
        calcPreview = preview
        calcDisplay = display
        displayBox.addView(preview, LinearLayout.LayoutParams(MP, 0, 0.7f))
        displayBox.addView(display, LinearLayout.LayoutParams(MP, 0, 1.3f))

        val close = FrameLayout(context)
        close.background = rounded(pal.calcDisplay, 22f)
        close.addView(IconView(context, IconView.Kind.CLOSE, pal.subtext), FrameLayout.LayoutParams(dp(20f), dp(20f), Gravity.CENTER))
        close.setOnClickListener { showPage(Page.LETTERS) }
        pressAnim(close)

        val topRow = LinearLayout(context)
        topRow.orientation = HORIZONTAL
        topRow.gravity = Gravity.CENTER_VERTICAL
        topRow.addView(displayBox, LinearLayout.LayoutParams(0, MP, 1f))
        topRow.addView(close, LinearLayout.LayoutParams(dp(44f), dp(44f)).apply { marginStart = dp(8f) })

        // key card (white rounded card, same keys / colors as the expense-tracker calculator)
        val card = LinearLayout(context)
        card.orientation = VERTICAL
        card.background = rounded(pal.calcPanel, 22f)
        card.elevation = dp(3f).toFloat()
        card.setPadding(dp(8f), dp(8f), dp(8f), dp(8f))
        val rows = listOf(
            listOf("⌫", "AC", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("00", "0", ".", "=")
        )
        for (items in rows) {
            val row = LinearLayout(context)
            row.orientation = HORIZONTAL
            for (label in items) {
                row.addView(calcKey(label), LinearLayout.LayoutParams(0, MP, 1f).apply {
                    setMargins(dp(3f), dp(3f), dp(3f), dp(3f))
                })
            }
            card.addView(row, LinearLayout.LayoutParams(MP, 0, 1f))
        }

        val copy = TextView(context).apply {
            text = "Copy"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            includeFontPadding = false
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(3f))
            background = lipDrawable(pal.calcEq, pal.calcEqLip, 16f)
            setOnClickListener {
                feedback(AudioManager.FX_KEYPRESS_STANDARD)
                copyCalculatorResult()
            }
        }
        pressAnim(copy)

        if (landscape) {
            val left = LinearLayout(context)
            left.orientation = VERTICAL
            left.addView(topRow, LinearLayout.LayoutParams(MP, 0, 1f))
            left.addView(copy, LinearLayout.LayoutParams(MP, dp(44f)).apply { topMargin = dp(8f) })
            root.addView(left, LinearLayout.LayoutParams(0, MP, 1f))
            root.addView(card, LinearLayout.LayoutParams(0, MP, 1.5f).apply { marginStart = dp(10f) })
        } else {
            root.addView(topRow, LinearLayout.LayoutParams(MP, dp(62f)))
            root.addView(card, LinearLayout.LayoutParams(MP, 0, 1f).apply { topMargin = dp(8f) })
            root.addView(copy, LinearLayout.LayoutParams(MP, dp(48f)).apply { topMargin = dp(8f) })
        }
        return root
    }

    private fun copyCalculatorResult() {
        val value = calcResult?.takeIf { it != "Error" }
            ?: evaluateCalculator(calcExpression.toString())
            ?: calcExpression.toString().takeIf { it.matches(Regex("\\d+(\\.\\d+)?")) }
        if (value.isNullOrBlank()) {
            Toast.makeText(context, "Enter a valid calculation first", Toast.LENGTH_SHORT).show()
            return
        }
        if (host.insertCalculatorResult(value)) {
            calcExpression = StringBuilder()
            calcResult = null
            showPage(Page.LETTERS)
            host.hideKeyboard()
        }
    }

    private fun evaluateCalculator(source: String): String? {
        if (source.isBlank()) return null
        return try {
            class Parser(private val input: String) {
                var pos = 0
                fun expression(): Double {
                    var value = term()
                    while (true) {
                        skip(); if (pos >= input.length) return value
                        when (input[pos]) {
                            '+' -> { pos++; value += term() }
                            '-' -> { pos++; value -= term() }
                            else -> return value
                        }
                    }
                }
                fun term(): Double {
                    var value = factor()
                    while (true) {
                        skip(); if (pos >= input.length) return value
                        when (input[pos]) {
                            '*' -> { pos++; value *= factor() }
                            '/' -> { pos++; val d = factor(); if (d == 0.0) throw ArithmeticException(); value /= d }
                            else -> return value
                        }
                    }
                }
                fun factor(): Double {
                    skip()
                    if (pos < input.length && (input[pos] == '+' || input[pos] == '-')) {
                        val sign = input[pos++]; val n = factor(); return if (sign == '-') -n else n
                    }
                    val start = pos
                    while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) pos++
                    if (start == pos) throw IllegalArgumentException()
                    var value = input.substring(start, pos).toDouble()
                    if (pos < input.length && input[pos] == '%') { pos++; value /= 100.0 }
                    return value
                }
                fun skip() { while (pos < input.length && input[pos].isWhitespace()) pos++ }
            }
            val parser = Parser(source)
            val number = parser.expression()
            parser.skip()
            if (parser.pos != source.length || !number.isFinite()) null
            else if (number == number.toLong().toDouble()) number.toLong().toString()
            else number.toBigDecimal().stripTrailingZeros().toPlainString()
        } catch (_: Exception) { null }
    }

    // ---------- Clipboard panel ----------

    private fun buildClipboardPanel(): View {
        val root = panelRoot()
        val list = LinearLayout(context)
        list.orientation = VERTICAL
        val scroll = ScrollView(context)
        scroll.addView(list)

        fun smallButton(label: String, alpha: Float, onClick: () -> Unit): TextView {
            val b = TextView(context)
            b.text = label
            b.textSize = 15f
            b.gravity = Gravity.CENTER
            b.alpha = alpha
            b.setTextColor(pal.text)
            b.setOnClickListener { onClick() }
            return b
        }

        fun fill() {
            list.removeAllViews()
            val entries = store.sortedHistory()
            if (entries.isEmpty()) {
                list.addView(emptyText("Nothing here yet.\nText you copy and calculator results will appear here."))
                return
            }
            for (e in entries) {
                val row = LinearLayout(context)
                row.orientation = HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                row.background = rounded(pal.chip, 12f, pal.keyStroke)
                row.setPadding(dp(12f), 0, dp(4f), 0)

                val t = TextView(context)
                t.text = e.text
                t.textSize = 14f
                t.maxLines = 2
                t.ellipsize = TextUtils.TruncateAt.END
                t.setTextColor(pal.text)
                row.addView(t, LinearLayout.LayoutParams(0, WC, 1f))

                row.addView(
                    smallButton("📌", if (e.pinned) 1f else 0.3f) {
                        store.togglePin(e.text)
                        fill()
                    },
                    LinearLayout.LayoutParams(dp(40f), dp(40f))
                )
                row.addView(
                    smallButton("✕", 0.6f) {
                        store.removeHistory(e.text)
                        fill()
                    },
                    LinearLayout.LayoutParams(dp(40f), dp(40f))
                )

                row.setOnClickListener {
                    if (host.insertClip(e.text)) showPage(Page.LETTERS)
                }
                pressAnim(row)
                val lp = LinearLayout.LayoutParams(MP, dp(48f))
                lp.setMargins(dp(2f), dp(3f), dp(2f), dp(3f))
                list.addView(row, lp)
            }
        }

        val header = headerRow()
        header.addView(pill("ABC") { showPage(Page.LETTERS) }, headerLp())
        val title = TextView(context)
        title.text = "Clipboard"
        title.textSize = 15f
        title.typeface = Typeface.DEFAULT_BOLD
        title.gravity = Gravity.CENTER
        title.setTextColor(pal.text)
        header.addView(title, LinearLayout.LayoutParams(0, MP, 1f))
        header.addView(pill("Clear") {
            store.clearHistory(true)
            fill()
        }, headerLp())

        root.addView(header, LinearLayout.LayoutParams(MP, dp(40f)))
        root.addView(scroll, LinearLayout.LayoutParams(MP, 0, 1f))
        fill()
        return root
    }

    // ---------- Emoji / GIF search (Gboard-style: results on top, small keyboard below) ----------

    private fun openSearch(mode: String) {
        searchMode = mode
        searchQuery.setLength(0)
        showPage(Page.SEARCH)
        runSearch()
    }

    private fun appendQuery(c: String) {
        searchQuery.append(c)
        onQueryChanged()
    }

    private fun backspaceQuery() {
        if (searchQuery.isNotEmpty()) searchQuery.deleteCharAt(searchQuery.length - 1)
        onQueryChanged()
    }

    private fun onQueryChanged() {
        updateQueryText()
        handler.removeCallbacks(searchRunnable)
        // GIF search waits a moment after the last key so every letter does not hit the network
        handler.postDelayed(searchRunnable, if (searchMode == "gif") 380L else 40L)
    }

    private fun updateQueryText() {
        val tv = searchQueryView ?: return
        if (searchQuery.isEmpty()) {
            tv.text = if (searchMode == "gif") "Search GIFs" else "Search emoji"
            tv.setTextColor(pal.subtext)
        } else {
            tv.text = searchQuery.toString()
            tv.setTextColor(pal.text)
        }
    }

    private fun runSearch() {
        val results = searchResults ?: return
        val q = searchQuery.toString().trim()
        if (searchMode == "emoji") showEmojiResults(results, q) else loadGifs(results, q)
    }

    private fun resultsMessage(results: LinearLayout, msg: String) {
        results.removeAllViews()
        val t = TextView(context)
        t.text = msg
        t.textSize = 14f
        t.gravity = Gravity.CENTER_VERTICAL
        t.setTextColor(pal.subtext)
        t.setPadding(dp(12f), 0, dp(12f), 0)
        results.addView(t, LinearLayout.LayoutParams(WC, MP))
    }

    private fun showEmojiResults(results: LinearLayout, q: String) {
        results.removeAllViews()
        val list = if (q.isEmpty()) store.loadRecentEmoji().take(30) else EmojiSearch.search(context, q)
        if (list.isEmpty()) {
            resultsMessage(results, if (q.isEmpty()) "Type to find an emoji, like happy, cat or heart" else "No emoji found")
            return
        }
        for (e in list) {
            val t = TextView(context)
            t.text = e
            t.textSize = 30f
            t.gravity = Gravity.CENTER
            t.includeFontPadding = false
            t.setOnClickListener {
                feedback(AudioManager.FX_KEYPRESS_STANDARD)
                host.typeText(e)
                store.addRecentEmoji(e)
            }
            pressAnim(t)
            results.addView(t, LinearLayout.LayoutParams(dp(46f), MP))
        }
    }

    private fun loadGifs(results: LinearLayout, q: String) {
        if (!GifClient.configured(store)) {
            resultsMessage(results, "Add a free GIF key first: open the Cheeseboard app > Settings > GIF search")
            return
        }
        val gen = ++searchGen
        resultsMessage(results, "Searching...")
        GifClient.search(store, q) { items, err ->
            if (gen != searchGen || searchResults !== results) return@search
            if (items.isEmpty()) {
                resultsMessage(results, err ?: "No GIFs found")
                return@search
            }
            results.removeAllViews()
            val h = searchResH - dp(8f)
            val w = (h * 1.3f).toInt()
            val radius = dp(10f).toFloat()
            for (item in items) {
                val iv = ImageView(context)
                iv.scaleType = ImageView.ScaleType.CENTER_CROP
                iv.setBackgroundColor(pal.chip)
                iv.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, radius)
                    }
                }
                iv.clipToOutline = true
                iv.setOnClickListener { sendGif(item) }
                results.addView(iv, LinearLayout.LayoutParams(w, h).apply { setMargins(dp(3f), dp(4f), dp(3f), dp(4f)) })
                GifClient.loadPreview(context, item.previewUrl) { d ->
                    if (d != null && iv.isAttachedToWindow) {
                        iv.setImageDrawable(d)
                        if (d is AnimatedImageDrawable) d.start()
                    }
                }
            }
            val credit = TextView(context)
            credit.text = "Powered by " + GifClient.providerName(store)
            credit.textSize = 11f
            credit.gravity = Gravity.CENTER
            credit.setTextColor(pal.subtext)
            credit.setPadding(dp(10f), 0, dp(10f), 0)
            results.addView(credit, LinearLayout.LayoutParams(WC, MP))
        }
    }

    private fun sendGif(item: GifItem) {
        feedback(AudioManager.FX_KEYPRESS_STANDARD)
        GifClient.download(context, item.fullUrl) { file ->
            if (file == null) {
                Toast.makeText(context, "Couldn't download that GIF", Toast.LENGTH_SHORT).show()
            } else {
                host.commitGif(file, item.fullUrl)
            }
        }
    }

    private fun buildSearchPanel(): View {
        val root = LinearLayout(context)
        root.orientation = VERTICAL
        root.setPadding(dp(2f), 0, dp(2f), 0)
        root.clipChildren = false

        // header: back, query, clear
        val header = LinearLayout(context)
        header.orientation = HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        header.addView(pill("\u2039") { showPage(Page.EMOJI) }, LinearLayout.LayoutParams(dp(40f), MP).apply { setMargins(dp(2f), dp(4f), dp(4f), dp(4f)) })
        val qv = TextView(context)
        qv.textSize = 16f
        qv.gravity = Gravity.CENTER_VERTICAL
        qv.maxLines = 1
        qv.ellipsize = TextUtils.TruncateAt.START
        qv.setPadding(dp(14f), 0, dp(14f), 0)
        qv.background = rounded(pal.chip, 14f, pal.keyStroke)
        searchQueryView = qv
        header.addView(qv, LinearLayout.LayoutParams(0, MP, 1f).apply { setMargins(0, dp(4f), dp(4f), dp(4f)) })
        header.addView(pill("\u2715") {
            searchQuery.setLength(0)
            onQueryChanged()
        }, LinearLayout.LayoutParams(dp(40f), MP).apply { setMargins(0, dp(4f), dp(2f), dp(4f)) })
        root.addView(header, LinearLayout.LayoutParams(MP, searchHeaderH))

        // results (scrolls sideways)
        val results = LinearLayout(context)
        results.orientation = HORIZONTAL
        results.gravity = Gravity.CENTER_VERTICAL
        searchResults = results
        val scroll = HorizontalScrollView(context)
        scroll.isHorizontalScrollBarEnabled = false
        scroll.addView(results, FrameLayout.LayoutParams(WC, MP))
        root.addView(scroll, LinearLayout.LayoutParams(MP, searchResH))

        // small keyboard that types into the search box instead of the app
        fun row(keys: List<Key>) {
            val r = LinearLayout(context)
            r.orientation = HORIZONTAL
            r.clipChildren = false
            keys.forEach { r.addView(it.cell) }
            root.addView(r, LinearLayout.LayoutParams(MP, searchRowH))
        }
        fun letter(ch: Char): Key = newKey(ch.toString(), 1f, Style.LETTER, sizeSp = 18f) { appendQuery(ch.toString()) }
        row("qwertyuiop".map { letter(it) })
        row("asdfghjkl".map { letter(it) })
        val back = newKey("", 1.5f, Style.SPECIAL, repeat = true, fx = AudioManager.FX_KEYPRESS_DELETE) { backspaceQuery() }
        addIcon(back, IconView(context, IconView.Kind.BACKSPACE, pal.modText))
        row("zxcvbnm".map { letter(it) } + listOf(back))
        val abc = newKey("ABC", 1.5f, Style.SPECIAL, sizeSp = 14f) { showPage(Page.LETTERS) }
        val space = newKey("", 4.6f, Style.SPACE, fx = AudioManager.FX_KEYPRESS_SPACEBAR) { appendQuery(" ") }
        val go = newKey("\uD83D\uDD0D", 1.6f, Style.ACCENT, sizeSp = 18f) {
            handler.removeCallbacks(searchRunnable)
            runSearch()
        }
        row(listOf(abc, space, go))

        updateQueryText()
        return root
    }

    // ---------- Emoji panel (Gboard-style: grid on top, category bar + backspace at the bottom) ----------

    private fun buildEmojiPanel(): View {
        val root = LinearLayout(context)
        root.orientation = VERTICAL
        root.setPadding(dp(2f), 0, dp(2f), 0)

        val groups = EmojiData.groups(context)
        val tabs = listOf("Recent") + groups.keys.toList()
        if (emojiCategory.isEmpty() || emojiCategory !in tabs) {
            emojiCategory = if (store.loadRecentEmoji().isNotEmpty()) "Recent" else tabs.getOrElse(1) { "Recent" }
        }

        val label = TextView(context).apply {
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.06f
            setTextColor(pal.subtext)
            setPadding(dp(10f), dp(2f), 0, 0)
            gravity = Gravity.CENTER_VERTICAL
        }
        val grid = LinearLayout(context).apply { orientation = VERTICAL }
        val scroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            addView(grid)
        }
        val cols = (cfg.screenWidthDp / 42).coerceIn(7, 12)
        val tabViews = LinkedHashMap<String, TextView>()

        fun render() {
            grid.removeAllViews()
            label.text = emojiCategory.uppercase()
            val list = if (emojiCategory == "Recent") store.loadRecentEmoji() else EmojiData.supported(context, emojiCategory)
            if (list.isEmpty()) {
                grid.addView(emptyText("Emojis you use will show up here."))
            }
            for (chunk in list.chunked(cols)) {
                val row = LinearLayout(context)
                row.orientation = HORIZONTAL
                for (e in chunk) {
                    val t = TextView(context)
                    t.text = e
                    t.textSize = 25f
                    t.gravity = Gravity.CENTER
                    t.includeFontPadding = false
                    t.setOnClickListener {
                        feedback(AudioManager.FX_KEYPRESS_STANDARD)
                        host.typeText(e)
                        store.addRecentEmoji(e)
                    }
                    pressAnim(t)
                    row.addView(t, LinearLayout.LayoutParams(0, dp(44f), 1f))
                }
                repeat(cols - chunk.size) { row.addView(View(context), LinearLayout.LayoutParams(0, dp(44f), 1f)) }
                grid.addView(row, LinearLayout.LayoutParams(MP, WC))
            }
            scroll.scrollTo(0, 0)
            for ((name, tv) in tabViews) {
                tv.background = if (name == emojiCategory) rounded(pal.selected, 11f) else null
            }
        }

        val chips = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        for (name in tabs) {
            val tv = TextView(context)
            tv.text = EMOJI_TAB_ICONS[name] ?: "🙂"
            tv.textSize = 20f
            tv.gravity = Gravity.CENTER
            tv.includeFontPadding = false
            tv.setOnClickListener {
                feedback(AudioManager.FX_KEYPRESS_STANDARD)
                emojiCategory = name
                render()
            }
            tabViews[name] = tv
            chips.addView(tv, LinearLayout.LayoutParams(dp(42f), dp(36f)).apply { setMargins(dp(1f), 0, dp(1f), 0) })
        }
        val chipScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(chips)
        }

        val bottom = LinearLayout(context)
        bottom.orientation = HORIZONTAL
        bottom.gravity = Gravity.CENTER_VERTICAL
        bottom.addView(pill("ABC") { showPage(Page.LETTERS) }, headerLp())
        bottom.addView(pill("GIF") { openSearch("gif") }, headerLp())
        bottom.addView(chipScroll, LinearLayout.LayoutParams(0, MP, 1f))
        val bk = backspaceKey(Style.SPECIAL)
        bk.cell.layoutParams = LinearLayout.LayoutParams(dp(52f), MP)
        bottom.addView(bk.cell)

        val topRow = LinearLayout(context)
        topRow.orientation = HORIZONTAL
        topRow.gravity = Gravity.CENTER_VERTICAL
        topRow.addView(label, LinearLayout.LayoutParams(0, MP, 1f))
        topRow.addView(pill("\uD83D\uDD0D Search") { openSearch("emoji") }, LinearLayout.LayoutParams(WC, dp(26f)).apply { setMargins(0, 0, dp(4f), 0) })
        root.addView(topRow, LinearLayout.LayoutParams(MP, dp(28f)))
        root.addView(scroll, LinearLayout.LayoutParams(MP, 0, 1f))
        root.addView(bottom, LinearLayout.LayoutParams(MP, dp(44f)))
        render()
        return root
    }
}
