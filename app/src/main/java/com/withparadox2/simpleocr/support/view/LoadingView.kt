package com.withparadox2.simpleocr.support.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.withparadox2.simpleocr.util.dp2px

class LoadingView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mLastDrawTime = 0L
    private val mInitRotations: Array<Float> = arrayOf(0f, 90f, 180f)
    private val mTempRectF = RectF()
    private val mGap = dp2px(2f)

    init {
        mPaint.color = Color.RED
        mPaint.style = Paint.Style.FILL
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val minSize = Math.min(measuredHeight, measuredWidth)
        setMeasuredDimension(minSize, minSize)
    }

    override fun onDraw(canvas: Canvas) {
        if (mLastDrawTime == 0L) {
            mLastDrawTime = System.currentTimeMillis()
        }

        var fraction = (System.currentTimeMillis() - mLastDrawTime) / 500f
        val totalRound = Math.floor(fraction.toDouble()).toInt()
        val roundNum = totalRound / 3
        val activeIndex = totalRound - roundNum * 3
        fraction -= totalRound.toFloat()

        val squareSize = ((measuredHeight / 2f - mGap) / Math.sqrt(2.toDouble())).toFloat()
        val cx = measuredWidth / 2f
        val cy = measuredHeight / 2f
        (0..2).forEach { pos ->
            var rotation = mInitRotations[pos] - roundNum * 90
            if (pos < activeIndex) {
                rotation -= 90
            } else if (pos == activeIndex) {
                rotation -= fraction * 90
            }

            canvas.save()
            canvas.rotate(rotation, cx, cy)
            mTempRectF.set(cx + mGap, cy - mGap - squareSize, cx + mGap + squareSize, cy - mGap)
            when (pos) {
                0 -> mPaint.color = Color.RED
                1 -> mPaint.color = Color.BLUE
                else -> mPaint.color = Color.YELLOW
            }
            canvas.drawRoundRect(mTempRectF, 5f, 5f, mPaint)
            canvas.restore()
        }

        invalidate()
    }
}