package com.withparadox2.simpleocr.support.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.TextView
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.util.dp2px

/**
 * Created by withparadox2 on 2018/11/13.
 */
class CropRotationWheel(context: Context, attributeSet: AttributeSet) : FrameLayout(context, attributeSet) {
    private val whitePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val colorPaint = Paint().apply {
        color = resources.getColor(R.color.colorAccent)
        style = Paint.Style.FILL
    }

    private var degreeLabel: TextView = TextView(getContext()).apply {
        setTextColor(Color.WHITE)
    }

    private val DELTA_ANGLE = 5
    private val MAX_ANGLE = 45f
    private var rotateDegree = 0.0f
    private val density = context.resources.displayMetrics.density
    private var preMotionX = 0.0f

    private val tempRect = RectF()

    init {
        addView(degreeLabel, LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            gravity = Gravity.CENTER or Gravity.TOP
        })
        setWillNotDraw(false)
        setRotateDegree(0f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val angle = -2 * rotateDegree
        val delta = angle % DELTA_ANGLE
        val segments = Math.floor(angle / DELTA_ANGLE.toDouble()).toInt()

        val centerY = height - dp2px(11)
        val centerX = width / 2
        (0..15).forEach { i ->
            var paint = whitePaint
            var a = i
            if (a < segments || a == 0 && delta < 0)
                paint = colorPaint

            drawLine(canvas, a, delta, centerX, centerY, a == segments || a == 0 && segments == -1, paint)

            if (i != 0) {
                a = -i
                paint = if (a > segments) colorPaint else whitePaint
                drawLine(canvas, a, delta, centerX, centerY, a == segments + 1, paint)
            }
        }

        colorPaint.alpha = 255

        tempRect.left = (width - dp2px(2.5f)) / 2f
        tempRect.top = centerY - dp2px(11f)
        tempRect.right = (width + dp2px(2.5f)) / 2f
        tempRect.bottom = centerY + dp2px(11f)
        canvas.drawRoundRect(tempRect, dp2px(2f), dp2px(2f), colorPaint)
    }

    private fun drawLine(canvas: Canvas, i: Int, delta: Float, centerX: Int, centerY: Int, center: Boolean, paint: Paint) {
        var drawPaint = paint
        val radius = (centerX - dp2px(80))

        val angle = 90 - (i * DELTA_ANGLE + delta)
        val offset = (radius * Math.cos(Math.toRadians(angle.toDouble()))).toInt()
        val x = centerX + offset

        val f = Math.abs(offset) / radius.toFloat()
        val alpha = Math.min(255, Math.max(0, ((1.0f - f * f) * 255).toInt()))

        if (center)
            drawPaint = colorPaint

        drawPaint.alpha = alpha

        val w = if (center) 4 else 2
        val h = if (center) dp2px(16) else dp2px(12)

        canvas.drawRect((x - w / 2).toFloat(), (centerY - h / 2).toFloat(), (x + w / 2).toFloat(), (centerY + h / 2).toFloat(), drawPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                preMotionX = event.x
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = preMotionX - event.x

                var newDegree = rotateDegree + (deltaX / density / Math.PI / 1.65f).toFloat()
                newDegree = Math.max(-MAX_ANGLE, Math.min(newDegree, MAX_ANGLE))

                if (Math.abs(newDegree - rotateDegree) > 0.001) {
                    if (Math.abs(newDegree) < 0.05) {
                        newDegree = 0f
                    }
                    setRotateDegree(newDegree)
                    preMotionX = event.x

                }
            }
        }
        return true
    }

    private fun setRotateDegree(degree: Float) {
        this.rotateDegree = degree
        var value = this.rotateDegree
        if (Math.abs(value) < 0.1 - 0.001)
            value = Math.abs(value)
        degreeLabel.text = String.format("%.1fº", value)
        invalidate()
    }
}