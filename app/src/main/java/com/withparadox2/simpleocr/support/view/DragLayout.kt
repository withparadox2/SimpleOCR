package com.withparadox2.simpleocr.support.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.customview.widget.ViewDragHelper

class DragLayout(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private var dragCallback: ViewDragHelper.Callback

    init {
        dragCallback = object : ViewDragHelper.Callback() {
            override fun tryCaptureView(child: View, pointerId: Int): Boolean {
                return true
            }

            override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
                return when {
                    top < 0 -> 0
                    top > measuredHeight -> measuredHeight
                    else -> top
                }
            }

            override fun getViewVerticalDragRange(child: View): Int {
                return measuredHeight
            }

            override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
                if (yvel > 0) {
                    dragHelper.settleCapturedViewAt(releasedChild.left, measuredHeight)
                } else {
                    dragHelper.settleCapturedViewAt(releasedChild.left, 0)
                }
                invalidate()
            }

            override fun onViewDragStateChanged(state: Int) {
                super.onViewDragStateChanged(state)
            }
        }
    }

    override fun computeScroll() {
        if (dragHelper.continueSettling(true)) {
            invalidate()
        }
    }

    var dragHelper: ViewDragHelper = ViewDragHelper.create(this, 1.0f, this.dragCallback)

    constructor(context: Context) : this(context, null)

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return dragHelper.shouldInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        dragHelper.processTouchEvent(event!!)
        return true
    }

    fun toggleAnimation(): Boolean {
        val child = getChildAt(0)
        var isShow = translationY != 0f || child.top != 0

        if (visibility != View.VISIBLE) {
            visibility = View.VISIBLE
            isShow = true
        }

        if (isShow) {
            // ViewDragHelper will change child's top
            child.top = 0
            translationY = height.toFloat()
            alpha = 0f
            animate().translationY(0f)
                    .alpha(1f)
                    .setDuration(300)
                    .start()
        } else {
            translationY = 0f
            alpha = 1f
            animate().translationY(height.toFloat())
                    .alpha(0f)
                    .setDuration(300)
                    .start()
        }
        return isShow
    }
}