package com.netcorte.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

class FloatingButtonView(context: Context) : View(context) {

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val fg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeCap = Paint.Cap.ROUND
    }
    private val oval = RectF()
    private var cut = false

    init { bg.color = COLOR_OK }

    fun setCut(value: Boolean) {
        if (value == cut) return
        cut = value
        bg.color = if (cut) COLOR_CUT else COLOR_OK
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val cx = w / 2f
        val cy = height / 2f
        canvas.drawCircle(cx, cy, w / 2f, bg)

        val r = w * 0.20f
        fg.strokeWidth = w * 0.07f
        oval.set(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(oval, -45f, 270f, false, fg)
        canvas.drawLine(cx, cy - r * 1.15f, cx, cy - r * 0.1f, fg)
    }

    companion object {
        private val COLOR_OK = Color.parseColor("#2E7D32")
        private val COLOR_CUT = Color.parseColor("#E53935")
    }
}
