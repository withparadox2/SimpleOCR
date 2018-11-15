package com.withparadox2.simpleocr.support.view

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import androidx.core.graphics.applyCanvas
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.util.dp2px


/**
 * Created by withparadox2 on 2018/5/15.
 */
const val BAR_UNDEFINED = 0
const val BAR_TOP = 1 shl 0
const val BAR_RIGHT = 1 shl 1
const val BAR_BOTTOM = 1 shl 2
const val BAR_LEFT = 1 shl 3

class CropImageView(context: Context, attributeSet: AttributeSet) : ImageView(context, attributeSet) {
    private val mPaint: Paint = Paint()
    private val mCropRect = RectF()
    private var mRectTemp = RectF()

    // The limitation for crop-rect calculated by view-bounds minus padding, bitmap can draw beyond
    // this bounds while user rotating, scaling or translating the image
    private val mDefaultRect = RectF()

    // The initial image matrix that make bitmap be filled in bounds defined by mDefaultRect, and it
    // is used to help to return back after manipulating the image
    private val mInitMatrix = Matrix()

    private val mAddMatrix = Matrix()

    private val mDotRadius: Float = dp2px(10f, context).toFloat()
    private var mTouchIndex = BAR_UNDEFINED
    private var mLastTouchX: Float = 0f
    private var mLastTouchY: Float = 0f

    private var mBitmapScale: Float = 1f

    private var mBitmapRect = RectF()
    private val mRectHandle = RectF()

    private var mAnimateLineRatio = 0.0f
    private var mAnimateLineIndex = BAR_UNDEFINED
    private val mLineAnimator = ObjectAnimator.ofFloat(this, "animateLineRatio", 1.0f, 0.0f).setDuration(500)

    private var mAnimateGridRatio = 0.0f

    private lateinit var mBitmap: Bitmap


    init {
        mPaint.color = Color.WHITE
        mPaint.style = Paint.Style.STROKE
        mLineAnimator.doOnEnd {
            mAnimateLineIndex = BAR_UNDEFINED
        }
    }

    override fun setImageBitmap(bitmap: Bitmap) {
        super.setImageBitmap(bitmap)
        mBitmap = bitmap

        val padding = dp2px(10).toFloat()
        val matrix = Matrix()
        val scale: Float
        var tx = padding
        var ty = padding


        val width = measuredWidth - padding - padding
        val height = measuredHeight - padding - padding
        if ((bitmap.width / bitmap.height.toFloat() > width / height.toFloat())) {
            scale = width / bitmap.width
            ty += (height - bitmap.height * scale) / 2
        } else {
            scale = height / bitmap.height
            tx += (width - bitmap.width * scale) / 2
        }

        matrix.setScale(scale, scale)
        matrix.postTranslate(tx, ty)

        mInitMatrix.set(matrix)

        imageMatrix = matrix

        mBitmapScale = scale
        mBitmapRect.set(tx, ty, tx + bitmap.width * scale, ty + bitmap.height * scale)
        mCropRect.set(mBitmapRect)
    }

    private fun setAnimateLineRatio(value: Float) {
        mAnimateLineRatio = value
        invalidate()
    }

    private fun setAnimateGridRatio(value: Float) {
        mAnimateGridRatio = value
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mTouchIndex = getTouchPointIndex(event.x, event.y)
                mRectTemp.set(mCropRect)

                mLastTouchX = event.x
                mLastTouchY = event.y

                if (mTouchIndex != 0) {
                    mAnimateLineIndex = mTouchIndex
                    if (mLineAnimator.isRunning) {
                        mLineAnimator.cancel()
                    }
                    mLineAnimator.start()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val moveX = event.x - mLastTouchX
                val moveY = event.y - mLastTouchY
                val localIndex = mTouchIndex

                val minSize = 4 * mDotRadius

                if (localIndex and BAR_TOP != 0) {
                    mCropRect.top = Math.min(mRectTemp.top + moveY, mCropRect.bottom - minSize)
                }
                if (localIndex and BAR_BOTTOM != 0) {
                    mCropRect.bottom = Math.max(mRectTemp.bottom + moveY, mCropRect.top + minSize)
                }

                if (localIndex and BAR_LEFT != 0) {
                    mCropRect.left = Math.min(mRectTemp.left + moveX, mCropRect.right - minSize)
                }

                if (localIndex and BAR_RIGHT != 0) {
                    mCropRect.right = Math.max(mRectTemp.right + moveX, mCropRect.left + minSize)
                }

                if (mCropRect.left <= mBitmapRect.left) {
                    mCropRect.left = mBitmapRect.left
                    mCropRect.right = Math.max(mCropRect.right, mCropRect.left + minSize)
                }
                if (mCropRect.right >= mBitmapRect.right) {
                    mCropRect.right = mBitmapRect.right
                    mCropRect.left = Math.min(mCropRect.left, mCropRect.right - minSize)
                }
                if (mCropRect.top <= mBitmapRect.top) {
                    mCropRect.top = mBitmapRect.top
                    mCropRect.bottom = Math.max(mCropRect.bottom, mCropRect.top + minSize)
                }
                if (mCropRect.bottom >= mBitmapRect.bottom) {
                    mCropRect.bottom = mBitmapRect.bottom
                    mCropRect.top = Math.min(mCropRect.top, mCropRect.bottom - minSize)
                }

                fitBound()
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                mTouchIndex = BAR_UNDEFINED
                ObjectAnimator.ofFloat(this, "animateGridRatio", 1.0f, 0.0f).setDuration(200).start()
            }
        }
        return true
    }

    private fun getTouchPointIndex(x: Float, y: Float): Int {
        val slop = 4 * mDotRadius * mDotRadius
        val distLeft = Math.abs(x - mCropRect.left)
        val distRight = Math.abs(x - mCropRect.right)
        val distTop = Math.abs(y - mCropRect.top)
        val distBottom = Math.abs(y - mCropRect.bottom)
        val minimum = Math.min(Math.min(distLeft, distRight), Math.min(distBottom, distTop))

        val inY = mCropRect.top < y && y < mCropRect.bottom
        val inX = mCropRect.left < x && x < mCropRect.right
        return when {
            distanceSquare(x, y, mCropRect.left, mCropRect.top) < slop -> BAR_TOP or BAR_LEFT
            distanceSquare(x, y, mCropRect.right, mCropRect.top) < slop -> BAR_TOP or BAR_RIGHT
            distanceSquare(x, y, mCropRect.left, mCropRect.bottom) < slop -> BAR_LEFT or BAR_BOTTOM
            distanceSquare(x, y, mCropRect.right, mCropRect.bottom) < slop -> BAR_RIGHT or BAR_BOTTOM
            x < mCropRect.left && inY -> BAR_LEFT
            x > mCropRect.right && inY -> BAR_RIGHT
            y < mCropRect.top && inX -> BAR_TOP
            y > mCropRect.bottom && inX -> BAR_BOTTOM
            minimum == distLeft -> BAR_LEFT
            minimum == distRight -> BAR_RIGHT
            minimum == distTop -> BAR_TOP
            minimum == distBottom -> BAR_BOTTOM
            else -> BAR_UNDEFINED
        }
    }

    private fun distanceSquare(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(0xAA000000.toInt())

        canvas.save()
        canvas.clipRect(mCropRect)
        super.onDraw(canvas)
        canvas.restore()

        val outlineWidth = dp2px(1).toFloat() * 2.0f
        val handleWidth = dp2px(3).toFloat()

        mPaint.alpha = 120
        mPaint.strokeWidth = outlineWidth
        canvas.drawRect(mCropRect, mPaint)

        // we are moving, draw grids
        if (mTouchIndex != BAR_UNDEFINED || mAnimateGridRatio != 0.0f) {
            mPaint.strokeWidth = dp2px(1).toFloat()

            // during cancel animation
            if (mTouchIndex == BAR_UNDEFINED) {
                mPaint.alpha = (120 * mAnimateGridRatio).toInt()
            }

            val gridSizeX = mCropRect.width() / 3.0f
            val gridSizeY = mCropRect.height() / 3.0f
            for (i in 1..2) {
                canvas.drawLine(mCropRect.left, mCropRect.top + gridSizeY * i, mCropRect.right, mCropRect.top + gridSizeY * i, mPaint)
                canvas.drawLine(mCropRect.left + gridSizeX * i, mCropRect.top, mCropRect.left + gridSizeX * i, mCropRect.bottom, mPaint)
            }
        }

        if (mAnimateLineRatio != 0.0f) {
            mPaint.strokeWidth = outlineWidth

            // before set alpha
            mPaint.color = resources.getColor(R.color.colorAccent)
            mPaint.alpha = (255 * mAnimateLineRatio).toInt()
            if (BAR_BOTTOM and mAnimateLineIndex != 0) {
                canvas.drawLine(mCropRect.left, mCropRect.bottom, mCropRect.right, mCropRect.bottom, mPaint)
            }
            if (BAR_TOP and mAnimateLineIndex != 0) {
                canvas.drawLine(mCropRect.left, mCropRect.top, mCropRect.right, mCropRect.top, mPaint)
            }
            if (BAR_LEFT and mAnimateLineIndex != 0) {
                canvas.drawLine(mCropRect.left, mCropRect.top, mCropRect.left, mCropRect.bottom, mPaint)
            }
            if (BAR_RIGHT and mAnimateLineIndex != 0) {
                canvas.drawLine(mCropRect.right, mCropRect.top, mCropRect.right, mCropRect.bottom, mPaint)
            }
            invalidate()
        }

        mPaint.alpha = 255
        mPaint.color = Color.WHITE
        mPaint.strokeWidth = handleWidth
        mRectHandle.set(mCropRect)
        mRectHandle.inset(outlineWidth / 2 - handleWidth / 2, outlineWidth / 2 - handleWidth / 2)
        drawHandles(canvas, mRectHandle, handleWidth / 2)
    }

    private fun drawHandles(canvas: Canvas, rect: RectF, halfWidth: Float) {
        val handleLength = dp2px(15)

        canvas.drawLine(rect.left - halfWidth, rect.top, rect.left + handleLength, rect.top, mPaint)
        canvas.drawLine(rect.left - halfWidth, rect.bottom, rect.left + handleLength, rect.bottom, mPaint)

        canvas.drawLine(rect.right + halfWidth, rect.top, rect.right - handleLength, rect.top, mPaint)
        canvas.drawLine(rect.right + halfWidth, rect.bottom, rect.right - handleLength, rect.bottom, mPaint)

        canvas.drawLine(rect.left, rect.top - halfWidth, rect.left, rect.top + handleLength, mPaint)
        canvas.drawLine(rect.right, rect.top - halfWidth, rect.right, rect.top + handleLength, mPaint)

        canvas.drawLine(rect.left, rect.bottom + halfWidth, rect.left, rect.bottom - handleLength, mPaint)
        canvas.drawLine(rect.right, rect.bottom + halfWidth, rect.right, rect.bottom - handleLength, mPaint)
    }

    fun getCropBitmap(): Bitmap {
        return Bitmap.createBitmap(mCropRect.width().toInt(), mCropRect.height().toInt(), Bitmap.Config.ARGB_8888).applyCanvas {
            val matrix = Matrix(imageMatrix)
            matrix.postTranslate(-mCropRect.left, -mCropRect.top)
            drawBitmap((drawable as BitmapDrawable).bitmap, matrix, null)
        }
    }

    private var mPreRotation = 0.0f

    private val mBoundRect = RectF()
    private val mContentRect = RectF()

    private val mTempMatrix = Matrix()
    fun rotate(rotation: Float) {
        imageMatrix.postRotate(rotation - mPreRotation, mCropRect.centerX(), mCropRect.centerY())
        mPreRotation = rotation

        fitBound()
    }

    private fun fitBound() {
        val rotation = mPreRotation
        val centerX = mCropRect.centerX()
        val centerY = mCropRect.centerY()

        mapContentRect(mContentRect, rotation)
        mapBoundRect(mBoundRect, rotation)

        if (!mContentRect.contains(mBoundRect)) {
            if (mBoundRect.height() > mContentRect.height() || mBoundRect.width() > mContentRect.width()) {
                val ratio = if (mContentRect.width() / mContentRect.height() > mBoundRect.width() / mBoundRect.height()) {
                    mBoundRect.height() / mContentRect.height()
                } else {
                    mBoundRect.width() / mContentRect.width()
                }

                // calculate new content rect after scale of ratio
                mTempMatrix.setScale(ratio, ratio, centerX, centerY)
                mTempMatrix.mapRect(mContentRect)

                imageMatrix.postScale(ratio, ratio, centerX, centerY)
            }

            val translation = fitTranslation(mBoundRect, mContentRect, Math.toRadians(rotation.toDouble()))
            imageMatrix.postTranslate(translation.x, translation.y)
        }

        invalidate()
    }

    private fun fitTranslation(small: RectF, big: RectF, rotation: Double): PointF {
        var offsetX = 0.0f
        var offsetY = 0.0f
        if (small.left < big.left) {
            offsetX = small.left - big.left
        } else if (small.right > big.right) {
            offsetX = small.right - big.right
        }
        if (small.top < big.top) {
            offsetY = small.top - big.top
        } else if (small.bottom > big.bottom) {
            offsetY = small.bottom - big.bottom
        }

        val deltaX = Math.cos(rotation) * offsetX - Math.sin(rotation) * offsetY
        val deltaY = Math.sin(rotation) * offsetX + Math.cos(rotation) * offsetY
        return PointF(deltaX.toFloat(), deltaY.toFloat())
    }

    private val mBitmapPoints = FloatArray(4)
    private fun mapContentRect(contentRect: RectF, rotation: Float) {
        mBitmapPoints[0] = 0f
        mBitmapPoints[1] = 0f
        mBitmapPoints[2] = mBitmap.width.toFloat()
        mBitmapPoints[3] = mBitmap.height.toFloat()

        mTempMatrix.set(imageMatrix)
        mTempMatrix.postRotate(-rotation, mCropRect.centerX(), mCropRect.centerY())
        mTempMatrix.mapPoints(mBitmapPoints)

        contentRect.set(mBitmapPoints[0], mBitmapPoints[1], mBitmapPoints[2], mBitmapPoints[3])
    }

    private fun mapBoundRect(dest: RectF, rotation: Float) {
        val matrix = Matrix()
        matrix.postRotate(rotation, mCropRect.centerX(), mCropRect.centerY())
        matrix.mapRect(dest, mCropRect)
    }

    fun reset() {
        mCropRect.set(mBitmapRect)
        imageMatrix.set(mInitMatrix)
        mPreRotation = 0.0f
        invalidate()
    }

}
