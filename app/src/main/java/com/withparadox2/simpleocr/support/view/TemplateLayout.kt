package com.withparadox2.simpleocr.support.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.customview.widget.ViewDragHelper
import com.withparadox2.simpleocr.R

class TemplateLayout(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
  private var dragCallback: ViewDragHelper.Callback
  private lateinit var targetView: View
  private var clampAnimTop = 0
  private var clampAnimBottom = 0
  private var targetViewIndex = 0
  private var lastVisible = false

  init {
    dragCallback = object : ViewDragHelper.Callback() {

      override fun getOrderedChildIndex(index: Int): Int {
        return targetViewIndex
      }

      override fun tryCaptureView(child: View, pointerId: Int): Boolean {
        return child == targetView
      }

      override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
        return when {
          top < clampAnimTop -> clampAnimTop
          top > clampAnimBottom -> clampAnimBottom
          else -> top
        }
      }

      override fun getViewVerticalDragRange(child: View): Int {
        return child.measuredHeight
      }

      override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
        if (yvel > 0) {
          dragHelper.settleCapturedViewAt(releasedChild.left, clampAnimBottom)
        } else {
          dragHelper.settleCapturedViewAt(releasedChild.left, clampAnimTop)
        }
        invalidate()
      }

      override fun onViewDragStateChanged(state: Int) {
        super.onViewDragStateChanged(state)
        if (state == ViewDragHelper.STATE_IDLE) {
        }
      }
    }
  }

  var dragHelper: ViewDragHelper = ViewDragHelper.create(this, 1.0f, this.dragCallback)

  override fun onFinishInflate() {
    super.onFinishInflate()
    targetView = findViewById(R.id.layout_template_container)
    targetViewIndex = children.indexOf(targetView)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    lastVisible = isVisibleToUser()
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    val isVisible = lastVisible
    super.onLayout(changed, left, top, right, bottom)
    val bottomView = findViewById<View>(R.id.layout_bottom)

    clampAnimTop = measuredHeight - targetView.measuredHeight - bottomView.measuredHeight
    clampAnimBottom = measuredHeight - bottomView.measuredHeight

    val targetTop = if (isVisible) clampAnimTop else clampAnimBottom
    targetView.layout(targetView.left, targetTop, targetView.right, targetTop + targetView.measuredHeight)
  }

  override fun computeScroll() {
    if (dragHelper.continueSettling(true)) {
      invalidate()
    }
  }

  override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
    return dragHelper.shouldInterceptTouchEvent(ev)
  }

  override fun onTouchEvent(event: MotionEvent?): Boolean {
    dragHelper.processTouchEvent(event!!)
    return true
  }

  private fun isVisibleToUser(): Boolean {
    @Suppress("ConvertTwoComparisonsToRangeCheck")
    return targetView.bottom > 0 && targetView.bottom < measuredHeight
  }

  fun toggleAnimation(): Boolean {
    val isShow = !isVisibleToUser()
    dragHelper.smoothSlideViewTo(targetView, targetView.left, if (isShow) clampAnimTop else clampAnimBottom)
    ViewCompat.postInvalidateOnAnimation(this)
    return isShow
  }
}