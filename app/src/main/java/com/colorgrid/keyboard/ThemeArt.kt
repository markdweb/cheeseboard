package com.colorgrid.keyboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Procedurally drawn keyboard backgrounds (no image files needed). */
enum class Texture { NONE, TOK, BRUSHED, WOOD, LAVA, RAIN, LUMEN, GRID, BEADBLAST, WAVES, PIXELS, VINYL, PORCELAIN, HORIZON }

/** Gradient + texture background. The artwork is drawn once into a bitmap and reused. */
class ThemeBackground(
    private val colors: IntArray,
    private val texture: Texture,
    private val dark: Boolean
) : Drawable() {

    private var bmp: Bitmap? = null

    override fun onBoundsChange(bounds: Rect) {
        bmp = null
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        var bm = bmp
        if (bm == null || bm.width != b.width() || bm.height != b.height()) {
            bm = render(b.width(), b.height())
            bmp = bm
        }
        canvas.drawBitmap(bm, b.left.toFloat(), b.top.toFloat(), null)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.OPAQUE

    private fun render(w: Int, h: Int): Bitmap {
        val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bm)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val cols = if (colors.size >= 2) colors else intArrayOf(colors[0], colors[0])
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), cols, null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null
        val rnd = Random(11)
        when (texture) {
            Texture.NONE -> {}
            Texture.TOK -> blob(c, w * 0.95f, h * 0.05f, w * 0.7f, Color.parseColor("#FFB9A3"), 150)
                .also { blob(c, w * 0.05f, h * 0.95f, w * 0.65f, Color.parseColor("#B5EBD6"), 150) }
            Texture.BRUSHED -> brushed(c, w, h, rnd)
            Texture.WOOD -> wood(c, w, h, rnd)
            Texture.LAVA -> lava(c, w, h, rnd)
            Texture.RAIN -> rain(c, w, h, rnd)
            Texture.GRID -> grid(c, w, h)
            Texture.BEADBLAST -> beadBlast(c, w, h, rnd)
            Texture.WAVES -> waves(c, w, h, rnd)
            Texture.PIXELS -> pixels(c, w, h, rnd)
            Texture.VINYL -> vinyl(c, w, h)
            Texture.PORCELAIN -> porcelain(c, w, h, rnd)
            Texture.HORIZON -> horizon(c, w, h)
            Texture.LUMEN -> {
                blob(c, w * 0.05f, h * 0.95f, w * 0.7f, Color.parseColor("#FF3DA5"), 190)
                blob(c, w * 0.55f, h * 1.05f, w * 0.6f, Color.parseColor("#FF8A1F"), 170)
                blob(c, w * 0.98f, h * 0.45f, w * 0.55f, Color.parseColor("#12E2B0"), 170)
                blob(c, w * 0.35f, h * 0.05f, w * 0.5f, Color.parseColor("#7A4DFF"), 120)
            }
        }
        return bm
    }

    private fun grid(c: Canvas, w: Int, h: Int) {
        blob(c, w * 0.9f, h * 0.1f, w * 0.55f, Color.parseColor("#7A4DFF"), 90)
        blob(c, w * 0.1f, h * 0.95f, w * 0.5f, Color.parseColor("#12E2B0"), 80)
        val p = Paint()
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f
        p.color = Color.argb(46, 46, 242, 208)
        val step = 24f
        var x = 0f
        while (x < w) { c.drawLine(x, 0f, x, h.toFloat(), p); x += step }
        var y = 0f
        while (y < h) { c.drawLine(0f, y, w.toFloat(), y, p); y += step }
    }

    private fun beadBlast(c: Canvas, w: Int, h: Int, rnd: Random) {
        val p = Paint()
        p.strokeWidth = 1.6f
        repeat(w * h / 55) {
            val light = rnd.nextBoolean()
            val a = 10 + rnd.nextInt(26)
            p.color = if (light) Color.argb(a + 10, 255, 255, 255) else Color.argb(a, 40, 50, 65)
            c.drawPoint(rnd.nextFloat() * w, rnd.nextFloat() * h, p)
        }
        blob(c, w * 0.3f, h * 0.0f, w * 0.7f, Color.WHITE, 70)
    }

    private fun waves(c: Canvas, w: Int, h: Int, rnd: Random) {
        blob(c, w * 0.1f, h * 0.1f, w * 0.6f, Color.parseColor("#B94BD6"), 120)
        blob(c, w * 0.95f, h * 0.9f, w * 0.6f, Color.parseColor("#12B8C8"), 110)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        for (i in 0 until 14) {
            val path = Path()
            val y0 = h * (i / 13f)
            val amp = 6f + rnd.nextFloat() * 16f
            val freq = 0.004f + rnd.nextFloat() * 0.006f
            val phase = rnd.nextFloat() * 6f
            path.moveTo(0f, y0)
            var x = 0
            while (x <= w) {
                path.lineTo(x.toFloat(), y0 + sin(x * freq + phase) * amp)
                x += 14
            }
            p.strokeWidth = 1.4f
            p.color = Color.argb(30 + rnd.nextInt(30), 220, 200, 255)
            c.drawPath(path, p)
        }
    }

    private fun pixels(c: Canvas, w: Int, h: Int, rnd: Random) {
        val p = Paint()
        p.style = Paint.Style.FILL
        repeat(70) {
            val s = if (rnd.nextInt(4) == 0) 6f else 3f
            val px = (rnd.nextInt(w / 6) * 6).toFloat()
            val py = (rnd.nextInt(h / 6) * 6).toFloat()
            val pick = rnd.nextInt(3)
            p.color = when (pick) {
                0 -> Color.argb(190, 255, 233, 90)
                1 -> Color.argb(170, 92, 242, 255)
                else -> Color.argb(150, 255, 61, 129)
            }
            c.drawRect(px, py, px + s, py + s, p)
        }
        p.color = Color.argb(26, 0, 0, 0)
        var y = 0f
        while (y < h) { c.drawRect(0f, y, w.toFloat(), y + 2f, p); y += 4f }
    }

    private fun vinyl(c: Canvas, w: Int, h: Int) {
        blob(c, w * 0.85f, h * 0.05f, w * 0.6f, Color.parseColor("#FFB38A"), 90)
        val cx = w * 0.5f
        val cy = h * 0.58f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.color = Color.argb(150, 12, 8, 16)
        c.drawCircle(cx, cy, h * 0.78f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f
        var r = h * 0.26f
        var i = 0
        while (r < h * 0.76f) {
            p.color = if (i % 3 == 0) Color.argb(40, 255, 255, 255) else Color.argb(36, 0, 0, 0)
            c.drawCircle(cx, cy, r, p)
            r += 4f
            i++
        }
        p.style = Paint.Style.FILL
        p.color = Color.argb(200, 244, 165, 122)
        c.drawCircle(cx, cy, h * 0.2f, p)
        p.color = Color.argb(230, 20, 12, 22)
        c.drawCircle(cx, cy, h * 0.03f, p)
    }

    private fun porcelain(c: Canvas, w: Int, h: Int, rnd: Random) {
        blob(c, w * 0.12f, h * 0.18f, w * 0.34f, Color.parseColor("#2147A6"), 70)
        blob(c, w * 0.88f, h * 0.82f, w * 0.4f, Color.parseColor("#3B6FD8"), 60)
        blob(c, w * 0.7f, h * 0.1f, w * 0.18f, Color.parseColor("#2147A6"), 50)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.color = Color.argb(70, 255, 255, 255)
        val band = Path()
        band.moveTo(w * 0.15f, 0f); band.lineTo(w * 0.4f, 0f); band.lineTo(w * 0.1f, h.toFloat()); band.lineTo(w * -0.15f, h.toFloat()); band.close()
        c.drawPath(band, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f
        p.color = Color.argb(26, 33, 71, 166)
        repeat(8) {
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h
            c.drawLine(x, y, x + 30f + rnd.nextFloat() * 50f, y + 8f - rnd.nextFloat() * 16f, p)
        }
    }

    private fun horizon(c: Canvas, w: Int, h: Int) {
        val hy = h * 0.6f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        // sun
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, hy - h * 0.34f, 0f, hy, Color.parseColor("#FFD35A"), Color.parseColor("#FF3D9A"), Shader.TileMode.CLAMP)
        val save = c.save()
        c.clipRect(0f, 0f, w.toFloat(), hy)
        c.drawCircle(w * 0.5f, hy - h * 0.04f, h * 0.3f, p)
        p.shader = null
        p.color = Color.parseColor("#1B0838")
        var yy = hy - h * 0.18f
        var th = 2f
        while (yy < hy) {
            c.drawRect(0f, yy, w.toFloat(), yy + th, p)
            yy += th + 7f
            th += 1.4f
        }
        c.restoreToCount(save)
        // grid floor
        p.style = Paint.Style.FILL
        p.color = Color.argb(150, 10, 4, 28)
        c.drawRect(0f, hy, w.toFloat(), h.toFloat(), p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.3f
        p.color = Color.argb(150, 255, 61, 154)
        c.drawLine(0f, hy, w.toFloat(), hy, p)
        for (i in -10..10) {
            c.drawLine(w * 0.5f + i * 6f, hy, w * 0.5f + i * w * 0.14f, h.toFloat(), p)
        }
        var gy = hy
        var gap = 5f
        while (gy < h) {
            gy += gap
            gap *= 1.35f
            c.drawLine(0f, gy, w.toFloat(), gy, p)
        }
    }

    private fun blob(c: Canvas, cx: Float, cy: Float, r: Float, color: Int, alpha: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val center = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        val edge = Color.argb(0, Color.red(color), Color.green(color), Color.blue(color))
        p.shader = RadialGradient(cx, cy, r, center, edge, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, p)
    }

    private fun brushed(c: Canvas, w: Int, h: Int, rnd: Random) {
        val p = Paint()
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val len = 60 + rnd.nextInt(260)
                val light = rnd.nextBoolean()
                val a = 8 + rnd.nextInt(if (dark) 22 else 34)
                p.color = if (light) Color.argb(a, 255, 255, 255) else Color.argb(a / 2 + 4, 0, 0, 0)
                c.drawLine(x.toFloat(), y.toFloat(), (x + len).toFloat(), y.toFloat(), p)
                x += len + rnd.nextInt(30)
            }
            y += 2
        }
    }

    private fun wood(c: Canvas, w: Int, h: Int, rnd: Random) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        var i = 0
        var y = 0f
        while (y < h) {
            val path = Path()
            val amp = 2f + rnd.nextFloat() * 5f
            val freq = 0.006f + rnd.nextFloat() * 0.01f
            val phase = rnd.nextFloat() * 6f
            path.moveTo(0f, y)
            var x = 0
            while (x <= w) {
                path.lineTo(x.toFloat(), y + sin(x * freq + phase + i * 0.5f) * amp)
                x += 16
            }
            val darkLine = rnd.nextInt(3) != 0
            p.strokeWidth = 1f + rnd.nextFloat() * 2f
            p.color = if (darkLine) Color.argb(30 + rnd.nextInt(60), 20, 8, 2) else Color.argb(14 + rnd.nextInt(20), 255, 200, 140)
            c.drawPath(path, p)
            y += 3f + rnd.nextFloat() * 4f
            i++
        }
    }

    private fun lava(c: Canvas, w: Int, h: Int, rnd: Random) {
        blob(c, w * 0.15f, h * 1.0f, w * 0.75f, Color.parseColor("#FF5A00"), 140)
        blob(c, w * 0.9f, h * 0.9f, w * 0.5f, Color.parseColor("#FF7A1A"), 90)
        val glow = Paint(Paint.ANTI_ALIAS_FLAG)
        glow.style = Paint.Style.STROKE
        glow.strokeWidth = 9f
        glow.color = Color.argb(70, 255, 110, 20)
        glow.maskFilter = BlurMaskFilter(9f, BlurMaskFilter.Blur.NORMAL)
        val core = Paint(Paint.ANTI_ALIAS_FLAG)
        core.style = Paint.Style.STROKE
        core.strokeWidth = 2.2f
        core.color = Color.argb(230, 255, 138, 31)
        repeat(9) {
            var x = rnd.nextFloat() * w
            var y = if (rnd.nextBoolean()) 0f else h.toFloat()
            var ang = if (y == 0f) 1.57f else -1.57f
            ang += (rnd.nextFloat() - 0.5f) * 1.2f
            val path = Path()
            path.moveTo(x, y)
            repeat(10) {
                ang += (rnd.nextFloat() - 0.5f) * 1.3f
                val len = 40f + rnd.nextFloat() * 70f
                x += cos(ang) * len
                y += sin(ang) * len
                path.lineTo(x, y)
            }
            c.drawPath(path, glow)
            c.drawPath(path, core)
        }
    }

    private fun rain(c: Canvas, w: Int, h: Int, rnd: Random) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        repeat(90) {
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h
            val len = 40f + rnd.nextFloat() * 110f
            p.strokeWidth = 1f + rnd.nextFloat() * 1.4f
            p.color = Color.argb(20 + rnd.nextInt(50), 170, 205, 255)
            c.drawLine(x, y, x + len * 0.12f, y + len, p)
        }
    }
}

/** A short effect drawn on top of a key while/after it is pressed. */
class PressFx(
    private val kind: String,
    private val color: Int,
    private val density: Float,
    private val cx: Float,
    private val cy: Float,
    private val w: Int,
    private val h: Int,
    private val radius: Float,
    private val inset: Float
) : Drawable() {

    var progress = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val angles = FloatArray(8) { it * 0.785f + (Random(it + 5).nextFloat() - 0.5f) * 0.5f }

    private fun withAlpha(a: Float): Int =
        Color.argb((a.coerceIn(0f, 1f) * 255).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    override fun draw(canvas: Canvas) {
        val a = 1f - progress
        when (kind) {
            "ripple" -> {
                val save = canvas.save()
                val path = Path()
                path.addRoundRect(RectF(inset, inset, w - inset, h - inset), radius, radius, Path.Direction.CW)
                canvas.clipPath(path)
                paint.style = Paint.Style.FILL
                paint.color = withAlpha(0.55f * a)
                canvas.drawCircle(cx, cy, 6f * density + progress * max(w, h) * 0.9f, paint)
                canvas.restoreToCount(save)
            }
            "halo" -> {
                val g = inset + 2f * density + progress * 9f * density
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.2f * density
                paint.color = withAlpha(0.85f * a)
                canvas.drawRoundRect(RectF(inset - g, inset - g, w - inset + g, h - inset + g), radius + g, radius + g, paint)
            }
            "sparks" -> {
                paint.style = Paint.Style.FILL
                paint.color = withAlpha(0.95f * a)
                val dist = 8f * density + progress * 30f * density
                val r = 2.6f * density * (1f - progress * 0.6f)
                for (ang in angles) {
                    canvas.drawCircle(cx + cos(ang) * dist, cy + sin(ang) * dist, r, paint)
                }
            }
        }
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/** The bubble that shows the pressed letter above the finger ("Key previews"). */
class PreviewBubble(
    private val text: String,
    private val fill: Int,
    private val stroke: Int,
    private val textColor: Int,
    private val density: Float
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: Canvas) {
        val r = RectF(bounds)
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(60, 0, 0, 0)
        canvas.drawRoundRect(RectF(r.left, r.top + 3f * density, r.right, r.bottom + 3f * density), 14f * density, 14f * density, paint)
        paint.color = fill
        canvas.drawRoundRect(r, 14f * density, 14f * density, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = density
        paint.color = stroke
        canvas.drawRoundRect(r, 14f * density, 14f * density, paint)
        paint.style = Paint.Style.FILL
        paint.color = textColor
        paint.textSize = r.height() * 0.52f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT
        val y = r.centerY() - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, r.centerX(), y, paint)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/** Small picture of a keyboard theme for the Themes page (uses the same colors and background as the real keyboard). */
class ThemePreviewView(context: Context, themeId: String) : View(context) {

    private val pal = Palette.resolve(context, 1, themeId)
    private val mascotKind = KeyboardThemes.mascotFor(themeId)
    private val d = context.resources.displayMetrics.density
    private val bg: Drawable = ThemeBackground(
        pal.bgGradient ?: intArrayOf(pal.bg, pal.bg), pal.texture, pal.dark
    )
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clip = Path()

    private val rows = listOf("qwertyuiop", "asdfghjkl", "⇧zxcvbnm⌫")

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        clip.reset()
        clip.addRoundRect(RectF(0f, 0f, w, h), 18f * d, 18f * d, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clip)
        bg.setBounds(0, 0, width, height)
        bg.draw(canvas)

        val pad = 8f * d
        val stripH = 26f * d
        // suggestion strip
        paint.style = Paint.Style.FILL
        paint.color = pal.text
        paint.textSize = 13f * d
        paint.textAlign = Paint.Align.CENTER
        val ty = stripH / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText("I", w * 0.28f, ty, paint)
        canvas.drawText("the", w * 0.5f, ty, paint)
        canvas.drawText("so", w * 0.72f, ty, paint)

        val rowH = (h - stripH - pad) / 4f
        val gap = 3f * d
        val unit = (w - pad * 2f) / 10f
        for ((ri, row) in rows.withIndex()) {
            val n = row.length
            var x = pad + if (ri == 1) unit * 0.5f else 0f
            for ((ci, ch) in row.withIndex()) {
                val special = ch == '⇧' || ch == '⌫'
                val kw = if (special) unit * 1.5f else unit
                val style = when {
                    ch == '⌫' -> 2
                    special -> 1
                    else -> 0
                }
                drawKey(canvas, x, stripH + ri * rowH, kw, rowH, gap, ch.toString(), style, ri * 10 + ci)
                x += kw
                if (n == 0) break
            }
        }
        // bottom row: 123, emoji, space, ., enter
        val by = stripH + 3 * rowH
        var x = pad
        val parts = listOf("123" to 1.4f, "☺" to 1.2f, "" to 4.8f, "." to 1.1f, "↵" to 1.5f)
        for ((i, p) in parts.withIndex()) {
            val kw = unit * p.second
            val style = when (i) {
                2 -> 3
                0, 1 -> 1
                4 -> 1
                else -> 0
            }
            drawKey(canvas, x, by, kw, rowH, gap, p.first, style, 30 + i)
            x += kw
        }
        canvas.restore()
    }

    /** style: 0 letter, 1 special, 2 accent, 3 space */
    private fun drawKey(c: Canvas, x: Float, y: Float, kw: Float, kh: Float, gap: Float, label: String, style: Int, idx: Int) {
        val r = pal.radius * d * 0.8f
        val rect = RectF(x + gap, y + gap, x + kw - gap, y + kh - gap)
        val fill = when (style) {
            2 -> pal.accent
            1 -> pal.special
            3 -> pal.spaceFrom
            else -> pal.key
        }
        val opaque = Color.argb(255, Color.red(fill), Color.green(fill), Color.blue(fill))
        val lipH = if (pal.keycap) 2.5f * d else 0f
        if (pal.keycap) {
            paint.style = Paint.Style.FILL
            paint.color = if (style == 2) Color.argb(255, Color.red(pal.accent) / 2, Color.green(pal.accent) / 2, Color.blue(pal.accent) / 2) else pal.lip
            c.drawRoundRect(rect, r, r, paint)
        }
        val top = RectF(rect.left, rect.top, rect.right, rect.bottom - lipH)
        paint.style = Paint.Style.FILL
        if (style == 3) {
            paint.shader = LinearGradient(top.left, 0f, top.right, 0f, pal.spaceFrom, pal.spaceTo, Shader.TileMode.CLAMP)
            c.drawRoundRect(top, r, r, paint)
            paint.shader = null
        } else {
            paint.color = if (Color.alpha(fill) < 255) fill else opaque
            c.drawRoundRect(top, r, r, paint)
        }
        val strokeColor = when {
            pal.rgb -> Color.HSVToColor(floatArrayOf(((idx * 14) % 360).toFloat(), 0.85f, 1f))
            style != 2 -> pal.keyStroke
            else -> 0
        }
        if (strokeColor != 0) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = if (pal.rgb) 1.4f * d else d
            paint.color = strokeColor
            c.drawRoundRect(top, r, r, paint)
        }
        if (style == 3 && mascotKind.isNotEmpty()) {
            val ms = top.height() * 0.78f
            Mascots.draw(c, mascotKind, top.right - ms * 0.75f, top.centerY(), ms, pal.accent)
        }
        if (label.isNotEmpty()) {
            paint.style = Paint.Style.FILL
            paint.color = when {
                style == 2 -> pal.accentText
                pal.letterRainbow -> Color.HSVToColor(floatArrayOf(((idx % 10) * 36).toFloat(), 0.6f, 1f))
                style == 0 -> pal.keyText
                else -> pal.modText
            }
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = (if (label.length > 1) 11f else 16f) * d
            val shown = if (pal.capsLabels && label.length == 1 && label[0].isLetter()) label.uppercase() else label
            val ty = top.centerY() - (paint.descent() + paint.ascent()) / 2f
            c.drawText(shown, top.centerX(), ty, paint)
        }
    }

    @Suppress("unused")
    private fun clamp(v: Float) = min(max(v, 0f), 1f)
}
