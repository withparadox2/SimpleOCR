package com.withparadox2.simpleocr.support.view

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import androidx.core.graphics.applyCanvas
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.util.dp2px
import kotlin.Float.Companion.NEGATIVE_INFINITY
import kotlin.Float.Companion.NaN
import kotlin.Float.Companion.POSITIVE_INFINITY


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

  // The initial image matrix that make bitmap be filled in bounds defined by mDefaultRect, and it
  // is used to help to return back after manipulating the image
  private val mInitMatrix = Matrix()

  private val mActiveBarSlop: Float = dp2px(15f)
  private var mActiveBarFlag = BAR_UNDEFINED
  private var mLastTouchX: Float = 0f
  private var mLastTouchY: Float = 0f

  private var mBitmapScale: Float = 1f

  private val mMaxCropRect = RectF()
  private var mInitContentRect = RectF()
  private val mRectHandle = RectF()

  private var mAnimateLineRatio = 0.0f
  private var mAnimateActiveBarFlag = BAR_UNDEFINED
  private val mLineAnimator = ObjectAnimator.ofFloat(this, "animateLineRatio", 1.0f, 0.0f).setDuration(500)
  private var mGridLineAnimator: ObjectAnimator? = null

  private var mAnimateGridRatio = 0.0f
  private var mShowGridLines = false

  private var mDragTouchSlop = dp2px(5f)

  private lateinit var mBitmap: Bitmap
  private var mRotateTimes = 0

  private val mScaleDetector: ScaleGestureDetector = ScaleGestureDetector(getContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
    override fun onScale(detector: ScaleGestureDetector): Boolean {
      if (mIsAnimating) {
        return false
      }

      var scale = detector.scaleFactor

      if (scale == NaN || scale == POSITIVE_INFINITY || scale == NEGATIVE_INFINITY) {
        return false
      }

      if (mPreScale * scale > 30f) {
        scale = 30f / mPreScale
      }

      imageMatrix.postScale(scale, scale, detector.focusX, detector.focusY)
      mPreScale *= scale
      invalidate()
      return true
    }
  })


  init {
    mPaint.color = Color.WHITE
    mPaint.style = Paint.Style.STROKE
    mLineAnimator.doOnEnd {
      mAnimateActiveBarFlag = BAR_UNDEFINED
    }
  }

  override fun setImageBitmap(bitmap: Bitmap) {
    super.setImageBitmap(bitmap)
    mBitmap = bitmap
    resetCropState(bitmap)
  }


  private fun resetCropState(bitmap: Bitmap, preRotation: Int = 0) {
    val padding = dp2px(10).toFloat()
    val scale: Float
    var tx = padding
    var ty = padding

    val width = measuredWidth - padding - padding
    val height = measuredHeight - padding - padding
    var bw = bitmap.width.toFloat()
    var bh = bitmap.height.toFloat()
    if (preRotation == 90 || preRotation == 270) {
      val temp = bw
      bw = bh
      bh = temp
    }
    if ((bw / bh > width / height)) {
      scale = width / bw
      ty += (height - bh * scale) / 2
    } else {
      scale = height / bh
      tx += (width - bw * scale) / 2
    }

    mBitmapScale = scale
    mInitContentRect.set(tx, ty, tx + bw * scale, ty + bh * scale)
    mCropRect.set(mInitContentRect)
    mMaxCropRect.set(padding, padding, measuredWidth - padding, measuredHeight - padding)

    val matrix = Matrix()
    matrix.postRotate(preRotation.toFloat())
    if (preRotation == 90) {
      matrix.postTranslate(bw, 0f)
    } else if (preRotation == 180) {
      matrix.postTranslate(bw, bh)
    } else if (preRotation == 270) {
      matrix.postTranslate(0f, bh)
    }
    matrix.postScale(scale, scale)
    matrix.postTranslate(tx, ty)

    mInitMatrix.set(matrix)
    imageMatrix = matrix
  }

  private fun setAnimateLineRatio(value: Float) {
    mAnimateLineRatio = value
    invalidate()
  }

  private fun setAnimateGridRatio(value: Float) {
    mAnimateGridRatio = value
    invalidate()
  }

  private var mIsDragging = false
  private var mIsAnimating = false
  private var mActivePointerId = 0
  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (mIsAnimating) {
      return true
    }
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        mActiveBarFlag = getActiveBar(event.x, event.y)

        mLastTouchX = event.x
        mLastTouchY = event.y

        if (mActiveBarFlag != BAR_UNDEFINED) {
          mAnimateActiveBarFlag = mActiveBarFlag
          if (mLineAnimator.isRunning) {
            mLineAnimator.cancel()
          }
          mLineAnimator.start()
        }
        mIsDragging = false
        mShowGridLines = true
        mActivePointerId = event.getPointerId(0)
        invalidate()
      }
      MotionEvent.ACTION_MOVE -> {
        if (mActiveBarFlag != BAR_UNDEFINED) {
          // change crop area
          changeCropArea(event.x - mLastTouchX, event.y - mLastTouchY)
          fitBound(false)
          mLastTouchX = event.x
          mLastTouchY = event.y
        } else {
          // handle other gesture
          val index = event.findPointerIndex(mActivePointerId)
          if (index != MotionEvent.INVALID_POINTER_ID) {
            val newX = event.getX(index)
            val newY = event.getY(index)
            var dx = newX - mLastTouchX
            var dy = newY - mLastTouchY
            if (!mIsDragging) {
              mIsDragging = Math.sqrt(dx * dx + dy * dy.toDouble()) > mDragTouchSlop
              dx = 0f
              dy = 0f
            }
            if (mIsDragging) {
              imageMatrix.postTranslate(dx, dy)
              invalidate()

              mLastTouchX = newX
              mLastTouchY = newY
            }
          }
        }
      }
      MotionEvent.ACTION_POINTER_UP -> {
        if (mIsDragging) {
          val idToUp = event.getPointerId(event.actionIndex)
          if (idToUp == mActivePointerId) {
            val newPointerIndex = if (event.actionIndex == 0) 1 else 0
            mActivePointerId = event.getPointerId(newPointerIndex)
            mLastTouchX = event.getX(newPointerIndex)
            mLastTouchY = event.getY(newPointerIndex)
          }
        }
      }
      MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
        mActivePointerId = MotionEvent.INVALID_POINTER_ID
        if (mActiveBarFlag != BAR_UNDEFINED) {
          mActiveBarFlag = BAR_UNDEFINED
        } else if (mIsDragging) {
          mIsDragging = false
          fitBound(false, true)
        }
        if (mShowGridLines) {
          startGridLineAnimation()
        }
        resetStartRotateScale()
      }
    }
    mScaleDetector.onTouchEvent(event)
    return true
  }

  private fun changeCropArea(deltaX: Float, deltaY: Float) {
    val minSize = 2.5f * mActiveBarSlop

    if (mActiveBarFlag and BAR_TOP != 0) {
      mCropRect.top = Math.min(Math.max(mCropRect.top + deltaY, mMaxCropRect.top), mCropRect.bottom - minSize)
    } else if (mActiveBarFlag and BAR_BOTTOM != 0) {
      mCropRect.bottom = Math.max(Math.min(mCropRect.bottom + deltaY, mMaxCropRect.bottom), mCropRect.top + minSize)
    }

    if (mActiveBarFlag and BAR_LEFT != 0) {
      mCropRect.left = Math.min(Math.max(mCropRect.left + deltaX, mMaxCropRect.left), mCropRect.right - minSize)
    } else if (mActiveBarFlag and BAR_RIGHT != 0) {
      mCropRect.right = Math.max(Math.min(mCropRect.right + deltaX, mMaxCropRect.right), mCropRect.left + minSize)
    }
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

    canvas.save()
    canvas.clipRect(mCropRect, Region.Op.DIFFERENCE)
    canvas.drawColor(0xAA000000.toInt())
    canvas.restore()

    val outlineWidth = dp2px(1).toFloat() * 2.0f
    val handleWidth = dp2px(3).toFloat()

    mPaint.alpha = 120
    mPaint.strokeWidth = outlineWidth
    canvas.drawRect(mCropRect, mPaint)

    // we are moving or rotating, draw grids
    if (mShowGridLines) {
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
      if (BAR_BOTTOM and mAnimateActiveBarFlag != 0) {
        canvas.drawLine(mCropRect.left, mCropRect.bottom, mCropRect.right, mCropRect.bottom, mPaint)
      }
      if (BAR_TOP and mAnimateActiveBarFlag != 0) {
        canvas.drawLine(mCropRect.left, mCropRect.top, mCropRect.right, mCropRect.top, mPaint)
      }
      if (BAR_LEFT and mAnimateActiveBarFlag != 0) {
        canvas.drawLine(mCropRect.left, mCropRect.top, mCropRect.left, mCropRect.bottom, mPaint)
      }
      if (BAR_RIGHT and mAnimateActiveBarFlag != 0) {
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

  private fun fitBound(scaleBack: Boolean, animate: Boolean = false) {
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

    val ratio = Math.abs(targetScale / mPreScale)
    mPreScale = targetScale

    if (!animate) {
      imageMatrix.postScale(ratio, ratio, centerX, centerY)
      imageMatrix.postTranslate(translation.x, translation.y)
      invalidate()
    } else {
      mIsAnimating = true
      val animator = ValueAnimator.ofFloat(0f, 1f)
      var preX = 0.0f
      var preY = 0.0f
      var preScale = 1.0f
      animator.addUpdateListener {
        val value: Float = it.animatedValue as Float
        val deltaX = value * translation.x - preX
        val deltaY = value * translation.y - preY
        preX += deltaX
        preY += deltaY

        // Here maybe the most confused part to understand. Assume the animation needs to update
        // n times, each time we scale up by ΔS_i and make a translation by ΔL_i, at the end we
        // can confirm that ΔS_1*ΔS_2*...*ΔS_n = S, where S is `ratio` defined above. For translation,
        // things get complicated for the following scale event will also apply to the earlier
        // translation. For ΔL_i, by the end we will get an amount of ΔL_i*(ΔS_i*ΔS_i+1*...*ΔS_n), which
        // is larger than expectation. So the answer becomes clear: eliminating the scale effect in advance
        // by divide the following scale. For ΔL_i, the final value set is ΔL_i/(ΔS_i*ΔS_i+1*...*ΔS_n) =
        // ΔL_i/(S/(ΔS_1*ΔS_2*...*ΔS_i-1)) = ΔL_i*(ΔS_1*ΔS_2*...*ΔS_i-1)/S = ΔL_i * preScale_i / S
        imageMatrix.postTranslate(deltaX * preScale / ratio, deltaY * preScale / ratio)

        val deltaScale = (1 + value * (ratio - 1)) / preScale
        preScale *= deltaScale

        imageMatrix.postScale(deltaScale, deltaScale, centerX, centerY)

        invalidate()
      }
      animator.duration = 100
      animator.doOnEnd {
        mIsAnimating = false
      }
      animator.start()
    }
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

    // If rotation exceeds 45 or -45 degree, the origin right will be less than left,
    // we should find the valid rect to wrap all these two points
    val left = Math.min(mBitmapPoints[0], mBitmapPoints[2])
    val right = Math.max(mBitmapPoints[0], mBitmapPoints[2])
    val top = Math.min(mBitmapPoints[1], mBitmapPoints[3])
    val bottom = Math.max(mBitmapPoints[1], mBitmapPoints[3])
    contentRect.set(left, top, right, bottom)
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
    mShowGridLines = true
    invalidate()
  }

  override fun onRotationEnd() {
    startGridLineAnimation {
      mRotating = false
    }
  }

  private fun resetStartRotateScale() {
    mRotateStartScale = 0.0f
  }

  fun reset(resetRotate: Boolean = true) {
    if (resetRotate) {
      mRotateTimes = 0
      resetCropState(mBitmap)
    }
    mCropRect.set(mInitContentRect)
    imageMatrix.set(mInitMatrix)
    mPreRotation = 0.0f
    mPreScale = 1.0f
    resetStartRotateScale()
    invalidate()
  }

  fun rotate() {
    mRotateTimes++
    resetCropState(mBitmap, (mRotateTimes * 90) % 360)
    reset(false)
  }

  private fun startGridLineAnimation(onAnimationEnd: ((Animator) -> Unit)? = null): ObjectAnimator {
    var animator = mGridLineAnimator
    if (animator != null && animator.isRunning) {
      animator.end()
      animator.cancel()
    }
    animator = ObjectAnimator.ofFloat(this, "animateGridRatio", 1.0f, 0.0f).setDuration(200)
    animator.doOnEnd {
      mShowGridLines = false
      if (onAnimationEnd != null) {
        onAnimationEnd(it)
      }
      invalidate()
    }
    mGridLineAnimator = animator
    animator.start()
    return animator
  }
}