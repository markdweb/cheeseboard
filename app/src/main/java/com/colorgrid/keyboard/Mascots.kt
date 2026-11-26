package com.colorgrid.keyboard

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.SweepGradient
import android.view.View
import kotlin.math.PI
import kotlin.math.sin

/**
 * Little characters that sit on the space bar, one per theme. All of them are drawn in code
 * (simple shapes in a 100 x 100 box), so no image files are needed.
 */
object Mascots {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val r = RectF()

    private fun col(s: String) = Color.parseColor(s)

    fun draw(c: Canvas, kind: String, cx: Float, cy: Float, size: Float, accent: Int) {
        val u = size / 100f
        val ox = cx - size / 2f
        val oy = cy - size / 2f
        fun x(v: Float) = ox + v * u
        fun y(v: Float) = oy + v * u

        fun fill(color: Int) {
            p.style = Paint.Style.FILL
            p.shader = null
            p.color = color
        }
        fun stroke(color: Int, w: Float) {
            p.style = Paint.Style.STROKE
            p.shader = null
            p.color = color
            p.strokeWidth = w * u
            p.strokeCap = Paint.Cap.ROUND
        }
        fun circle(px: Float, py: Float, pr: Float, color: Int) {
            fill(color)
            c.drawCircle(x(px), y(py), pr * u, p)
        }
        fun oval(l: Float, t: Float, rr: Float, b: Float, color: Int) {
            fill(color)
            r.set(x(l), y(t), x(rr), y(b))
            c.drawOval(r, p)
        }
        fun rrect(l: Float, t: Float, rr: Float, b: Float, rad: Float, color: Int) {
            fill(color)
            r.set(x(l), y(t), x(rr), y(b))
            c.drawRoundRect(r, rad * u, rad * u, p)
        }
        fun poly(color: Int, vararg pts: Float) {
            fill(color)
            path.reset()
            path.moveTo(x(pts[0]), y(pts[1]))
            var i = 2
            while (i < pts.size) {
                path.lineTo(x(pts[i]), y(pts[i + 1]))
                i += 2
            }
            path.close()
            c.drawPath(path, p)
        }
        fun arc(l: Float, t: Float, rr: Float, b: Float, start: Float, sweep: Float, color: Int, w: Float) {
            stroke(color, w)
            r.set(x(l), y(t), x(rr), y(b))
            c.drawArc(r, start, sweep, false, p)
        }
        fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, w: Float) {
            stroke(color, w)
            c.drawLine(x(x1), y(y1), x(x2), y(y2), p)
        }
        val dark = col("#2A1F1B")

        when (kind) {
            "chick" -> {
                circle(50f, 56f, 34f, col("#FFE9A8"))
                circle(50f, 18f, 7f, col("#F4694A")); circle(41f, 23f, 5.5f, col("#F4694A")); circle(59f, 23f, 5.5f, col("#F4694A"))
                poly(col("#FF9F43"), 44f, 56f, 56f, 56f, 50f, 66f)
                circle(37f, 50f, 4f, dark); circle(63f, 50f, 4f, dark)
                circle(29f, 62f, 5f, col("#FFB3A0")); circle(71f, 62f, 5f, col("#FFB3A0"))
            }
            "robot" -> {
                line(50f, 24f, 50f, 11f, col("#7A8594"), 4f)
                circle(50f, 9f, 5f, accent)
                rrect(6f, 44f, 15f, 62f, 3f, col("#566170")); rrect(85f, 44f, 94f, 62f, 3f, col("#566170"))
                rrect(14f, 24f, 86f, 84f, 16f, col("#3A4350"))
                rrect(24f, 40f, 76f, 68f, 12f, col("#0B1115"))
                circle(40f, 54f, 6f, accent); circle(60f, 54f, 6f, accent)
            }
            "gear" -> {
                val body = col("#F2711C")
                for (i in 0 until 8) {
                    c.save()
                    c.rotate(i * 45f, x(50f), y(52f))
                    rrect(43f, 14f, 57f, 30f, 3f, body)
                    c.restore()
                }
                circle(50f, 52f, 29f, body)
                circle(41f, 50f, 8f, Color.WHITE); circle(59f, 50f, 8f, Color.WHITE)
                circle(42f, 51f, 4f, dark); circle(60f, 51f, 4f, dark)
                arc(40f, 56f, 60f, 70f, 20f, 140f, dark, 3f)
            }
            "penguin" -> {
                oval(22f, 12f, 78f, 92f, col("#2B3340"))
                oval(34f, 40f, 66f, 88f, col("#F5F7FA"))
                circle(41f, 34f, 6f, Color.WHITE); circle(59f, 34f, 6f, Color.WHITE)
                circle(42f, 35f, 3f, dark); circle(58f, 35f, 3f, dark)
                poly(col("#FFA133"), 43f, 44f, 57f, 44f, 50f, 54f)
                oval(28f, 88f, 46f, 99f, col("#FFA133")); oval(54f, 88f, 72f, 99f, col("#FFA133"))
            }
            "owl" -> {
                poly(col("#8A5A38"), 20f, 30f, 28f, 8f, 42f, 26f)
                poly(col("#8A5A38"), 80f, 30f, 72f, 8f, 58f, 26f)
                oval(16f, 20f, 84f, 94f, col("#8A5A38"))
                oval(28f, 62f, 72f, 90f, col("#C99A6B"))
                circle(36f, 42f, 15f, col("#F3E2CC")); circle(64f, 42f, 15f, col("#F3E2CC"))
                circle(37f, 43f, 6f, col("#2A180C")); circle(63f, 43f, 6f, col("#2A180C"))
                poly(col("#F2C14E"), 45f, 52f, 55f, 52f, 50f, 63f)
            }
            "flame" -> {
                poly(col("#FF6A00"), 22f, 58f, 50f, 2f, 78f, 58f)
                circle(50f, 64f, 30f, col("#FF6A00"))
                circle(50f, 70f, 17f, col("#FFC83D"))
                circle(42f, 64f, 3.5f, dark); circle(58f, 64f, 3.5f, dark)
            }
            "drop" -> {
                poly(col("#5FB4FF"), 24f, 50f, 50f, 4f, 76f, 50f)
                circle(50f, 64f, 31f, col("#5FB4FF"))
                circle(38f, 56f, 6f, col("#BFE3FF"))
                circle(42f, 66f, 4f, col("#0B2A4A")); circle(58f, 66f, 4f, col("#0B2A4A"))
                arc(42f, 68f, 58f, 80f, 20f, 140f, col("#0B2A4A"), 3f)
            }
            "orb" -> {
                val sg = SweepGradient(
                    x(50f), y(52f),
                    intArrayOf(col("#FF3DA5"), col("#FF8A1F"), col("#F5E04D"), col("#12E2B0"), col("#4D9BFF"), col("#7A4DFF"), col("#FF3DA5")),
                    null
                )
                p.style = Paint.Style.FILL
                p.shader = sg
                c.drawCircle(x(50f), y(52f), 36f * u, p)
                p.shader = null
                circle(50f, 52f, 27f, col("#101018"))
                circle(41f, 50f, 5f, col("#FF7AC0")); circle(59f, 50f, 5f, col("#3EF0C8"))
                arc(41f, 54f, 59f, 68f, 20f, 140f, col("#F5E04D"), 3f)
            }
            "bubble" -> {
                fill(Color.argb(95, 255, 255, 255))
                c.drawCircle(x(50f), y(52f), 36f * u, p)
                stroke(Color.argb(220, 255, 255, 255), 2.5f)
                c.drawCircle(x(50f), y(52f), 36f * u, p)
                oval(30f, 28f, 46f, 40f, Color.argb(200, 255, 255, 255))
                circle(41f, 56f, 4f, col("#2A4A7A")); circle(59f, 56f, 4f, col("#2A4A7A"))
                arc(41f, 58f, 59f, 72f, 20f, 140f, col("#2A4A7A"), 3f)
                circle(32f, 66f, 4f, Color.argb(120, 255, 130, 170)); circle(68f, 66f, 4f, Color.argb(120, 255, 130, 170))
            }
            "cat" -> {
                poly(col("#F5A25D"), 20f, 44f, 24f, 10f, 46f, 30f)
                poly(col("#F5A25D"), 80f, 44f, 76f, 10f, 54f, 30f)
                circle(50f, 56f, 33f, col("#F5A25D"))
                circle(40f, 52f, 4f, dark); circle(60f, 52f, 4f, dark)
                poly(col("#FF8FA3"), 46f, 61f, 54f, 61f, 50f, 66f)
                line(30f, 62f, 14f, 58f, dark, 2f); line(30f, 68f, 14f, 70f, dark, 2f)
                line(70f, 62f, 86f, 58f, dark, 2f); line(70f, 68f, 86f, 70f, dark, 2f)
            }
            "lofi" -> {
                poly(col("#8E8A99"), 22f, 46f, 26f, 14f, 46f, 32f)
                poly(col("#8E8A99"), 78f, 46f, 74f, 14f, 54f, 32f)
                circle(50f, 58f, 31f, col("#8E8A99"))
                circle(41f, 56f, 3.5f, dark); circle(59f, 56f, 3.5f, dark)
                poly(col("#FFB0B8"), 46f, 63f, 54f, 63f, 50f, 68f)
                arc(14f, 6f, 86f, 80f, 190f, 160f, col("#F4A57A"), 6f)
                circle(18f, 56f, 9f, col("#F4A57A")); circle(82f, 56f, 9f, col("#F4A57A"))
            }
            "ghost" -> {
                fill(col("#EDEBFF"))
                path.reset()
                path.moveTo(x(20f), y(88f))
                path.lineTo(x(20f), y(46f))
                r.set(x(20f), y(16f), x(80f), y(76f))
                path.arcTo(r, 180f, 180f)
                path.lineTo(x(80f), y(88f))
                path.quadTo(x(70f), y(100f), x(60f), y(88f))
                path.quadTo(x(50f), y(100f), x(40f), y(88f))
                path.quadTo(x(30f), y(100f), x(20f), y(88f))
                path.close()
                c.drawPath(path, p)
                circle(40f, 48f, 5f, dark); circle(60f, 48f, 5f, dark)
                oval(45f, 58f, 55f, 70f, dark)
            }
            "cassette" -> {
                rrect(6f, 22f, 94f, 82f, 9f, col("#D9C7A1"))
                rrect(16f, 30f, 84f, 52f, 5f, col("#FFF7E0"))
                circle(40f, 41f, 3.2f, dark); circle(60f, 41f, 3.2f, dark)
                rrect(26f, 58f, 74f, 76f, 9f, col("#3B2F25"))
                circle(38f, 67f, 6f, col("#F0E6D0")); circle(62f, 67f, 6f, col("#F0E6D0"))
                circle(38f, 67f, 2.5f, col("#3B2F25")); circle(62f, 67f, 2.5f, col("#3B2F25"))
            }
            "alien" -> {
                line(32f, 24f, 24f, 8f, col("#5FD35F"), 3f); line(68f, 24f, 76f, 8f, col("#5FD35F"), 3f)
                circle(24f, 8f, 5f, accent); circle(76f, 8f, 5f, accent)
                oval(14f, 20f, 86f, 90f, col("#8CF28C"))
                oval(22f, 44f, 46f, 70f, col("#0B1A0B")); oval(54f, 44f, 78f, 70f, col("#0B1A0B"))
                circle(34f, 52f, 4f, Color.WHITE); circle(66f, 52f, 4f, Color.WHITE)
            }
            "bunny" -> {
                oval(28f, 0f, 45f, 48f, col("#FFF4F8")); oval(55f, 0f, 72f, 48f, col("#FFF4F8"))
                oval(33f, 8f, 41f, 40f, col("#FFB3CB")); oval(59f, 8f, 67f, 40f, col("#FFB3CB"))
                circle(50f, 64f, 30f, col("#FFF4F8"))
                circle(40f, 62f, 4f, dark); circle(60f, 62f, 4f, dark)
                circle(50f, 70f, 3.5f, col("#FF7FA8"))
                circle(32f, 70f, 5f, col("#FFC1D4")); circle(68f, 70f, 5f, col("#FFC1D4"))
            }
            "frog" -> {
                oval(12f, 36f, 88f, 92f, col("#7BCB86"))
                circle(30f, 36f, 15f, col("#7BCB86")); circle(70f, 36f, 15f, col("#7BCB86"))
                circle(30f, 36f, 10f, Color.WHITE); circle(70f, 36f, 10f, Color.WHITE)
                circle(31f, 37f, 5f, dark); circle(69f, 37f, 5f, dark)
                arc(28f, 52f, 72f, 82f, 20f, 140f, col("#2F6B3A"), 3.5f)
                circle(44f, 56f, 1.8f, col("#2F6B3A")); circle(56f, 56f, 1.8f, col("#2F6B3A"))
            }
            "nut" -> {
                poly(col("#AEB4BD"), 50f, 12f, 82f, 31f, 82f, 69f, 50f, 88f, 18f, 69f, 18f, 31f)
                circle(50f, 50f, 22f, col("#C9CED6"))
                circle(43f, 47f, 4f, dark); circle(57f, 47f, 4f, dark)
                arc(41f, 50f, 59f, 64f, 20f, 140f, dark, 3f)
            }
            "gem" -> {
                poly(col("#B26CFF"), 50f, 8f, 90f, 40f, 50f, 94f, 10f, 40f)
                poly(col("#D3A5FF"), 50f, 8f, 70f, 40f, 30f, 40f)
                line(10f, 40f, 90f, 40f, col("#8A45D6"), 2f)
                circle(41f, 54f, 4f, Color.WHITE); circle(59f, 54f, 4f, Color.WHITE)
                circle(42f, 55f, 2f, dark); circle(60f, 55f, 2f, dark)
            }
            "speaker" -> {
                rrect(22f, 6f, 78f, 94f, 11f, col("#5B3A22"))
                circle(50f, 26f, 10f, col("#2A1A0F")); circle(50f, 26f, 5f, col("#F0A12F"))
                circle(50f, 64f, 24f, col("#2A1A0F"))
                stroke(col("#EFE3C8"), 3f)
                c.drawCircle(x(50f), y(64f), 20f * u, p)
                circle(50f, 64f, 11f, col("#F0A12F"))
                circle(46f, 61f, 2.5f, dark); circle(54f, 61f, 2.5f, dark)
            }
            "invader" -> {
                val rows = arrayOf(
                    "00100000100", "00010001000", "00111111100", "01101110110",
                    "11111111111", "10111111101", "10100000101", "00011011000"
                )
                val cell = 9f
                val ax = (100f - cell * 11f) / 2f
                val ay = (100f - cell * 8f) / 2f
                for ((ri, row) in rows.withIndex()) {
                    for ((ci, ch) in row.withIndex()) {
                        if (ch == '1') {
                            fill(accent)
                            c.drawRect(x(ax + ci * cell), y(ay + ri * cell), x(ax + (ci + 1) * cell - 0.6f), y(ay + (ri + 1) * cell - 0.6f), p)
                        }
                    }
                }
            }
            "teacup" -> {
                fill(Color.WHITE)
                r.set(x(18f), y(18f), x(78f), y(88f))
                c.drawArc(r, 0f, 180f, true, p)
                oval(18f, 44f, 78f, 58f, col("#DCE6FF"))
                arc(18f, 18f, 78f, 88f, 0f, 180f, col("#2147A6"), 3f)
                arc(68f, 50f, 92f, 76f, -80f, 200f, col("#2147A6"), 4f)
                circle(40f, 68f, 3.2f, col("#2147A6")); circle(56f, 68f, 3.2f, col("#2147A6"))
                circle(34f, 74f, 3.5f, col("#FFB3C6")); circle(62f, 74f, 3.5f, col("#FFB3C6"))
                arc(36f, 4f, 46f, 20f, 270f, 180f, Color.argb(150, 33, 71, 166), 2.5f)
                arc(52f, 4f, 62f, 20f, 270f, 180f, Color.argb(150, 33, 71, 166), 2.5f)
            }
            "sun" -> {
                circle(50f, 52f, 34f, col("#FFB347"))
                circle(50f, 64f, 24f, col("#FF5FA8"))
                circle(50f, 52f, 34f, Color.argb(0, 0, 0, 0))
                rrect(14f, 42f, 86f, 60f, 8f, col("#0E0A1A"))
                rrect(18f, 45f, 46f, 57f, 5f, col("#29E7FF")); rrect(54f, 45f, 82f, 57f, 5f, col("#29E7FF"))
                arc(38f, 62f, 62f, 78f, 20f, 140f, col("#0E0A1A"), 3.5f)
            }
            else -> {}
        }
    }
}

/** The mascot on the space bar. Hops a little every time you type. */
class MascotView(context: Context, private val kind: String, private val accent: Int) : View(context) {

    private var hop = 0f

    override fun onDraw(canvas: Canvas) {
        val s = minOf(width, height).toFloat() * 0.92f
        canvas.save()
        canvas.translate(0f, -hop * s * 0.16f)
        Mascots.draw(canvas, kind, width / 2f, height / 2f, s, accent)
        canvas.restore()
    }

    fun bounce() {
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 240
        a.addUpdateListener {
            hop = sin(it.animatedFraction * PI).toFloat()
            invalidate()
        }
        a.start()
    }
}
