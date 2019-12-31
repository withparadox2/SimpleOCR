package com.withparadox2.simpleocr.support.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.withparadox2.simpleocr.util.dp2px

private const val STATE_ANIM_SHOW = 0
private const val STATE_ANIM_HIDE = 1
private const val STATE_DEFAULT = -1
private const val DURATION_SCALE = 500
private const val DURATION_ROTATE = 500

class LoadingView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mLastDrawTime = 0L
    private val mInitRotations: Array<Float> = arrayOf(0f, 90f, 180f)
    private val mTempRectF = RectF()
    private val mGap = dp2px(2f)
    private var mScales: Array<Float> = arrayOf(1f, 1f, 1f)
    private var mVisibilityState = STATE_DEFAULT
    private var mLastVisibleDrawTime = 0L
    private var mShowAction: Runnable? = null
    private var mHideAction: Runnable? = null

    init {
        mPaint.style = Paint.Style.FILL
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val minSize = Math.min(measuredHeight, measuredWidth)
        setMeasuredDimension(minSize, minSize)
    }

    override fun onDraw(canvas: Canvas) {
        val currentTime = System.currentTimeMillis()
        val itemCount = 3

        if (mVisibilityState != STATE_DEFAULT) {
            if (mLastVisibleDrawTime == 0L) {
                mLastVisibleDrawTime = currentTime
            }

            var visibleFraction = (currentTime - mLastVisibleDrawTime) / DURATION_SCALE.toFloat()
            val visibleRound = Math.floor(visibleFraction.toDouble()).toInt()
            visibleFraction -= visibleRound

            if (visibleRound > 0) {
                if (mVisibilityState == STATE_ANIM_SHOW) {
                    onShow()
                } else {
                    onHide()
                }
                visibleFraction = 1f
            }

            val delay = 0.1f * (itemCount - 1)
            val weight = 1 / (1 - (itemCount - 1) * delay)
            (0 until itemCount).forEach { pos ->
                var scale = weight * (visibleFraction - delay * pos)
                if (scale > 1f) {
                    scale = 1f
                } else if (scale < 0f) {
                    scale = 0f
                }

                if (mVisibilityState == STATE_ANIM_HIDE) {
                    scale = 1 - scale
                }
                mScales[pos] = scale
            }

            if (visibleRound > 0) {
                mVisibilityState = STATE_DEFAULT
            }
        } else {
            mScales.indices.forEach {
                mScales[it] = 1f
            }
        }

        if (mLastDrawTime == 0L) {
            mLastDrawTime = currentTime
        }

        var fraction = (currentTime - mLastDrawTime) / DURATION_ROTATE.toFloat()

        val totalRound = Math.floor(fraction.toDouble()).toInt()
        val roundNum = totalRound / 3
        val activeIndex = totalRound - roundNum * 3
        fraction -= totalRound.toFloat()

        val squareSize = ((measuredHeight / 2f - mGap) / Math.sqrt(2.toDouble())).toFloat()
        val cx = measuredWidth / 2f
        val cy = measuredHeight / 2f
        val borderRadius = dp2px(2f)
        (0 until itemCount).forEach { pos ->
            var rotation = mInitRotations[pos] - roundNum * 90
            if (pos < activeIndex) {
                rotation -= 90
            } else if (pos == activeIndex) {
                rotation -= fraction * 90
            }

            canvas.save()
            canvas.rotate(rotation, cx, cy)

            mTempRectF.set(cx + mGap, cy - mGap - squareSize, cx + mGap + squareSize, cy - mGap)
            val insetSize = (1 - mScales[pos]) * squareSize / 2
            mTempRectF.inset(insetSize, insetSize)
            when (pos) {
                0 -> mPaint.color = Color.RED
                1 -> mPaint.color = Color.CYAN
                else -> mPaint.color = Color.YELLOW
            }
            canvas.drawRoundRect(mTempRectF, borderRadius, borderRadius, mPaint)
            canvas.restore()
        }

        invalidate()
    }

    fun show(showAction: Runnable? = null) {
        mVisibilityState = STATE_ANIM_SHOW
        mLastVisibleDrawTime = 0
        visibility = VISIBLE
        mShowAction = showAction
        invalidate()
    }

    fun isShow(): Boolean {
        return visibility == View.VISIBLE
    }

    fun isAnimating(): Boolean {
        return mVisibilityState == STATE_ANIM_SHOW || mVisibilityState == STATE_ANIM_HIDE
    }

    private fun onShow() {
        mShowAction?.run()
    }

    fun hide(hideAction: Runnable? = null) {
        mVisibilityState = STATE_ANIM_HIDE
        mLastVisibleDrawTime = 0
        mHideAction = hideAction
    }

    private fun onHide() {
        visibility = GONE
        mHideAction?.run()
    }
}