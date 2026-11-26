package com.colorgrid.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View

/** Draws an evenly divided cols x rows grid with a number in every cell. */
class GridOverlayView(
    context: Context,
    private val cols: Int,
    private val rows: Int
) : View(context) {

    var target: Int? = null
        set(value) {
            field = value
            invalidate()
        }

    private val d = resources.displayMetrics.density

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * d
    }
    private val hitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF2D95")
        style = Paint.Style.STROKE
        strokeWidth = 3f * d
    }
    private val labelBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val cw = w / cols
        val rh = h / rows

        // grid lines (pantay ang hati)
        for (i in 0..cols) canvas.drawLine(i * cw, 0f, i * cw, h, linePaint)
        for (r in 0..rows) canvas.drawLine(0f, r * rh, w, r * rh, linePaint)

        // highlight sa napiling cell
        val hit = target?.takeIf { it in 1..(cols * rows) }
        if (hit != null) {
            val hr = (hit - 1) / cols
            val hc = (hit - 1) % cols
            canvas.drawRect(hc * cw, hr * rh, (hc + 1) * cw, (hr + 1) * rh, hitPaint)
        }

        // number labels
        val base = (minOf(cw, rh) * 0.32f).coerceIn(8f * d, 15f * d)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val n = r * cols + c + 1
                val isHit = n == hit
                textPaint.textSize = if (isHit) base * 1.4f else base
                val s = n.toString()
                val fm = textPaint.fontMetrics
                val lh = (fm.descent - fm.ascent) + 2f * d
                val lw = minOf(textPaint.measureText(s) + 6f * d, cw - 2f * d)
                val x0 = c * cw + 2f * d
                val y0 = r * rh + 2f * d
                rect.set(x0, y0, x0 + lw, y0 + lh)
                labelBg.color = if (isHit) Color.parseColor("#FF2D95") else Color.parseColor("#CC000000")
                canvas.drawRoundRect(rect, 3f * d, 3f * d, labelBg)
                canvas.drawText(s, rect.centerX(), rect.centerY() - (fm.ascent + fm.descent) / 2f, textPaint)
            }
        }
    }
}
