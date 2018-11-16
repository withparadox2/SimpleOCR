package com.withparadox2.simpleocr.support.view

import android.animation.Animator
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

class CropImageView(context: Context, attributeSet: AttributeSet) : ImageView(context, attributeSet), CropRotationWheel.Callback {
    private val mPaint: Paint = Paint()
    private val mCropRect = RectF()
    private var mRectTemp = RectF()

    // The initial image matrix that make bitmap be filled in bounds defined by mDefaultRect, and it
    // is used to help to return back after manipulating the image
    private val mInitMatrix = Matrix()

    private val mActiveBarSlop: Float = dp2px(10f, context)
    private var mActiveBarFlag = BAR_UNDEFINED
    private var mLastTouchX: Float = 0f
    private var mLastTouchY: Float = 0f

    private var mBitmapScale: Float = 1f

    private var mInitContentRect = RectF()
    private val mRectHandle = RectF()

    private var mAnimateLineRatio = 0.0f
    private var mAnimateLineIndex = BAR_UNDEFINED
    private val mLineAnimator = ObjectAnimator.ofFloat(this, "animateLineRatio", 1.0f, 0.0f).setDuration(500)
    private var mGridLineAnimator: ObjectAnimator? = null

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
        if ((bitmap.width / bitmap.height.toFloat() > width / height)) {
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
        mInitContentRect.set(tx, ty, tx + bitmap.width * scale, ty + bitmap.height * scale)
        mCropRect.set(mInitContentRect)
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
                mActiveBarFlag = getActiveBar(event.x, event.y)
                mRectTemp.set(mCropRect)

                mLastTouchX = event.x
                mLastTouchY = event.y

                if (mActiveBarFlag != 0) {
                    mAnimateLineIndex = mActiveBarFlag
                    if (mLineAnimator.isRunning) {
                        mLineAnimator.cancel()
                    }
                    mLineAnimator.start()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val moveX = event.x - mLastTouchX
                val moveY = event.y - mLastTouchY
                val localIndex = mActiveBarFlag

                val minSize = 4 * mActiveBarSlop

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

                if (mCropRect.left <= mInitContentRect.left) {
                    mCropRect.left = mInitContentRect.left
                    mCropRect.right = Math.max(mCropRect.right, mCropRect.left + minSize)
                }
                if (mCropRect.right >= mInitContentRect.right) {
                    mCropRect.right = mInitContentRect.right
                    mCropRect.left = Math.min(mCropRect.left, mCropRect.right - minSize)
                }
                if (mCropRect.top <= mInitContentRect.top) {
                    mCropRect.top = mInitContentRect.top
                    mCropRect.bottom = Math.max(mCropRect.bottom, mCropRect.top + minSize)
                }
                if (mCropRect.bottom >= mInitContentRect.bottom) {
                    mCropRect.bottom = mInitContentRect.bottom
                    mCropRect.top = Math.min(mCropRect.top, mCropRect.bottom - minSize)
                }

                resetStartRotateScale()
                fitBound(false)
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                startGridLineAnimation {
                    mActiveBarFlag = BAR_UNDEFINED
                }
            }
        }
        return true
    }

    private val mBarTempRect = RectF()
    private fun getActiveBar(x: Float, y: Float): Int {
        var barFlag = BAR_UNDEFINED

        mBarTempRect.set(mCropRect)
        mBarTempRect.inset(-mActiveBarSlop, -mActiveBarSlop)
        if (mBarTempRect.contains(x, y)) {

            mBarTempRect.set(mCropRect)
            mBarTempRect.inset(mActiveBarSlop, mActiveBarSlop)
            if (!mBarTempRect.contains(x, y)) {
                if (x < mBarTempRect.left) {
                    barFlag = barFlag or BAR_LEFT
                } else if (x > mBarTempRect.right) {
                    barFlag = barFlag or BAR_RIGHT
                }

                if (y < mBarTempRect.top) {
                    barFlag = barFlag or BAR_TOP
                } else if (y > mBarTempRect.bottom) {
                    barFlag = barFlag or BAR_BOTTOM
                }
            }
        }
        return barFlag
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

        // we are moving or rotating, draw grids
        if (mActiveBarFlag != BAR_UNDEFINED || mRotating) {
            mPaint.strokeWidth = dp2px(1).toFloat()

            // during cancel animation
            if (mAnimateGridRatio != 0.0f && mAnimateGridRatio != 1.0f) {
                mPaint.alpha = (120 * mAnimateGridRatio).toInt()
            }

            val gapCount = if (mRotating) 6 else 3
            val gridSizeX = mCropRect.width() / gapCount
            val gridSizeY = mCropRect.height() / gapCount
            for (i in 1 until gapCount) {
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
    private var mPreScale = 1.0f
    private var mRotateStartScale = 1.0f
    private val mBoundRect = RectF()
    private val mContentRect = RectF()
    private val mTempMatrix = Matrix()
    private var mRotating = false

    private fun fitBound(scaleBack: Boolean) {
        val rotation = mPreRotation
        val centerX = mCropRect.centerX()
        val centerY = mCropRect.centerY()

        val translation = PointF(0f, 0f)
        var targetScale = mPreScale

        mapContentRect(mContentRect, rotation)
        mapBoundRect(mBoundRect, rotation)

        if (!mContentRect.contains(mBoundRect)) {
            if (mBoundRect.height() > mContentRect.height() || mBoundRect.width() > mContentRect.width()) {
                val ratio = getScaleToFitBound()
                targetScale = ratio * mPreScale
                fitScale(ratio)
            }
            fitTranslation(mBoundRect, mContentRect, Math.toRadians(rotation.toDouble()), translation)
        } else if (scaleBack) {
            var ratio = getScaleToFitBound()
            val newScale = ratio * mPreScale
            if (newScale < mRotateStartScale) {
                ratio = 1.0f
            }
            targetScale = ratio * mPreScale
            fitScale(ratio)
            fitTranslation(mBoundRect, mContentRect, Math.toRadians(rotation.toDouble()), translation)
        }

        val ratio = targetScale / mPreScale
        imageMatrix.postScale(ratio, ratio, centerX, centerY)
        imageMatrix.postTranslate(translation.x, translation.y)

        mPreScale = targetScale

        invalidate()
    }

    private fun getScaleToFitBound(): Float {
        return if (mContentRect.width() / mContentRect.height() > mBoundRect.width() / mBoundRect.height()) {
            mBoundRect.height() / mContentRect.height()
        } else {
            mBoundRect.width() / mContentRect.width()
        }
    }

    private fun fitScale(ratio: Float) {
        // calculate new content rect after scale of ratio
        mTempMatrix.setScale(ratio, ratio, mBoundRect.centerX(), mBoundRect.centerY())
        mTempMatrix.mapRect(mContentRect)
    }

    private fun fitTranslation(small: RectF, big: RectF, rotation: Double, translation: PointF) {
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

        translation.x = (Math.cos(rotation) * offsetX - Math.sin(rotation) * offsetY).toFloat()
        translation.y = (Math.sin(rotation) * offsetX + Math.cos(rotation) * offsetY).toFloat()
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

    override fun onRotationChanged(degree: Float) {
        imageMatrix.postRotate(degree - mPreRotation, mCropRect.centerX(), mCropRect.centerY())
        mPreRotation = degree
        fitBound(true)
    }

    override fun onRotationStart() {
        if (mRotateStartScale < 0.00001f) {
            mRotateStartScale = mPreScale
        }
        mRotating = true
    }

    override fun onRotationEnd() {
        startGridLineAnimation {
            mRotating = false
        }
    }

    private fun resetStartRotateScale() {
        mRotateStartScale = 0.0f
    }

    fun reset() {
        mCropRect.set(mInitContentRect)
        imageMatrix.set(mInitMatrix)
        mPreRotation = 0.0f
        resetStartRotateScale()
        invalidate()
    }

    private fun startGridLineAnimation(onAnimationEnd: (Animator) -> Unit): ObjectAnimator {
        var animator = mGridLineAnimator
        if (animator != null && animator.isRunning) {
            animator.end()
            animator.cancel()
        }
        animator = ObjectAnimator.ofFloat(this, "animateGridRatio", 1.0f, 0.0f).setDuration(200)
        animator.doOnEnd(onAnimationEnd)
        mGridLineAnimator = animator
        animator.start()
        return animator
    }
}
