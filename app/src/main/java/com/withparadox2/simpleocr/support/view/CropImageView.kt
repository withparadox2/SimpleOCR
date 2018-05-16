package com.withparadox2.simpleocr.support.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import com.withparadox2.simpleocr.util.dp2px

/**
 * Created by withparadox2 on 2018/5/15.
 */
class CropImageView(context: Context, attributeSet: AttributeSet) : ImageView(context, attributeSet) {
    private val mPaint: Paint = Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private lateinit var mRect: RectF
    private lateinit var mRectTemp: RectF
    private val mDotRadius: Float = dp2px(10, context).toFloat()
    private var mTouchIndex = -1
    private var mLastTouchX: Float = 0f
    private var mLastTouchY: Float = 0f

    private var mBitmapScale: Float = 1f
    private var mBitmapTx: Float = 0f
    private var mBitmapTy: Float = 0f

    init {
        mPaint.color = Color.WHITE
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = 3f
        mPaint.alpha = 200
    }

    fun setBitmapScale(scale: Float) {
        mBitmapScale = scale
    }

    fun setBitmapTx(tx: Float) {
        mBitmapTx = tx
    }

    fun setBitmapTy(ty: Float) {
        mBitmapTy = ty
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mRect = RectF(w * 0.25f, h * 0.25f, w * 0.75f, h * 0.75f)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val index = getTouchPointIndex(event.x, event.y)
                mTouchIndex = index
                if (mTouchIndex != -1) {
                    mRectTemp = RectF(mRect)
                }
                mLastTouchX = event.x
                mLastTouchY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val moveX = event.x - mLastTouchX
                val moveY = event.y - mLastTouchY

                if (mTouchIndex >= 0) {
                    val minSize = 4 * mDotRadius

                    if (mTouchIndex == 0 || mTouchIndex == 1) {
                        mRect.top = Math.min(mRectTemp.top + moveY, mRect.bottom - minSize)
                    } else {
                        mRect.bottom = Math.max(mRectTemp.bottom + moveY, mRect.top + minSize)
                    }

                    if (mTouchIndex == 0 || mTouchIndex == 2) {
                        mRect.left = Math.min(mRectTemp.left + moveX, mRect.right - minSize)
                    } else {
                        mRect.right = Math.max(mRectTemp.right + moveX, mRect.left + minSize)
                    }
                } else if (mTouchIndex == -2) {
                    mRect.set(mRectTemp)
                    mRect.offset(moveX, moveY)
                }
                invalidate()
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                mTouchIndex = -1
            }
        }
        return true
    }

    private fun getTouchPointIndex(x: Float, y: Float): Int {
        val slop = 4 * mDotRadius * mDotRadius
        return when {
            distanceSquare(x, y, mRect.left, mRect.top) < slop -> 0
            distanceSquare(x, y, mRect.right, mRect.top) < slop -> 1
            distanceSquare(x, y, mRect.left, mRect.bottom) < slop -> 2
            distanceSquare(x, y, mRect.right, mRect.bottom) < slop -> 3
            mRect.contains(x, y) -> -2
            else -> -1
        }
    }

    private fun distanceSquare(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mPaint.style = Paint.Style.STROKE
        canvas.drawRect(mRect, mPaint)
        mPaint.style = Paint.Style.FILL
        canvas.drawCircle(mRect.left, mRect.top, mDotRadius, mPaint)
        canvas.drawCircle(mRect.left, mRect.bottom, mDotRadius, mPaint)
        canvas.drawCircle(mRect.right, mRect.top, mDotRadius, mPaint)
        canvas.drawCircle(mRect.right, mRect.bottom, mDotRadius, mPaint)
    }

    fun getCropBitmap(): Bitmap {
        val offTop = mRect.top - mBitmapTy
        val offLeft = mRect.left - mBitmapTx

        return Bitmap.createBitmap((drawable as BitmapDrawable).bitmap,
                (offLeft / mBitmapScale).toInt(),
                (offTop / mBitmapScale).toInt(),
                (mRect.width() / mBitmapScale).toInt(),
                (mRect.height() / mBitmapScale).toInt()
        )
    }
}
