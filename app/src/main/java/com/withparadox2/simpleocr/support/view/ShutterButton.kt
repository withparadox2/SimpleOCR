package com.withparadox2.simpleocr.support.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.withparadox2.simpleocr.util.dp2px

class ShutterButton(context: Context, attr : AttributeSet?) : View(context, attr) {
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mInterpolator = DecelerateInterpolator()
    private var mClickListener : OnClickListener? = null

    constructor(context : Context) : this(context, null)

    init {
        mPaint.color = Color.WHITE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = Math.max(measuredHeight, measuredWidth)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        val size = measuredWidth
        val ringWidth = dp2px(4)
        val halfSize = size / 2.toFloat()

        mPaint.style = Paint.Style.STROKE
        mPaint.alpha = 255
        mPaint.strokeWidth = ringWidth.toFloat()

        canvas.drawCircle(halfSize, halfSize, halfSize - ringWidth / 2, mPaint)

        if (scaleX != 1.0f) {
            val scale = (scaleX - 1.0f) / 0.06f
            mPaint.alpha = (255 * scale).toInt()
            mPaint.style = Paint.Style.FILL
            canvas.drawCircle(halfSize, halfSize, (size - ringWidth * 4) * 0.5f, mPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(event.action == MotionEvent.ACTION_DOWN) {
            animSize(true)
        } else if (event.action == MotionEvent.ACTION_UP) {
            animSize(false)
            if (event.x >= 0 && event.y >= 0 && event.x <= measuredWidth && event.y <= measuredHeight) {
                mClickListener?.onClick(this)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            animSize(false)
        }
        return true
    }

    override fun setOnClickListener(l: OnClickListener?) {
        mClickListener = l
    }

    private fun animSize(scaleUp : Boolean) {
        val animatorSet = AnimatorSet()
        if (scaleUp) {
            animatorSet.playTogether(
                    ObjectAnimator.ofFloat(this, "scaleX", 1.06f),
                    ObjectAnimator.ofFloat(this, "scaleY", 1.06f))
        } else {
            animatorSet.playTogether(
                    ObjectAnimator.ofFloat(this, "scaleX", 1.0f),
                    ObjectAnimator.ofFloat(this, "scaleY", 1.0f))
            animatorSet.startDelay = 40
        }
        animatorSet.duration = 120
        animatorSet.interpolator = mInterpolator
        animatorSet.start()
    }

    override fun setScaleX(scaleX: Float) {
        super.setScaleX(scaleX)
        invalidate()
    }

    override fun setScaleY(scaleY: Float) {
        super.setScaleY(scaleY)
        invalidate()
    }
}