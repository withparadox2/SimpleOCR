package com.withparadox2.simpleocr.support.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.support.camera.CameraController
import com.withparadox2.simpleocr.util.dp2px

class FlashSwitch(context: Context, attr: AttributeSet) : FrameLayout(context, attr) {
    private val mViewList: Array<ImageView> = Array(2) { _ ->
        ImageView(context)
    }
    private val mImageSize = dp2px(48)
    private var mAnimating : Boolean = false

    init {
        mViewList[0].setImageResource(R.drawable.flash_auto)
        mViewList[1].setImageResource(R.drawable.flash_off)
        mViewList.forEach { view ->
            view.scaleType = ImageView.ScaleType.CENTER
            view.visibility = View.INVISIBLE
            addView(view, FrameLayout.LayoutParams(mImageSize, mImageSize, Gravity.CENTER_VERTICAL))
            view.setOnClickListener { currentImage ->
                if (CameraController.instance.getCamera() == null || mAnimating) {
                    //TODO check grammar
                    return@setOnClickListener
                }

                val nextImage = if (currentImage == mViewList[0]) {
                    mViewList[1]
                } else {
                    mViewList[0]
                }

                nextImage.visibility = View.VISIBLE
                val animatorSet = AnimatorSet()
                animatorSet.playTogether(
                        ObjectAnimator.ofFloat(currentImage, "translationY", 0.0f, mImageSize.toFloat()),
                        ObjectAnimator.ofFloat(nextImage, "translationY", -mImageSize.toFloat(), 0.0f),
                        ObjectAnimator.ofFloat(currentImage, "alpha", 1.0f, 0.0f),
                        ObjectAnimator.ofFloat(nextImage, "alpha", 0.0f, 1.0f))
                animatorSet.duration = 200
                animatorSet.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animator: Animator) {
                        currentImage.visibility = View.INVISIBLE
                        mAnimating = false
                    }
                })
                animatorSet.start()
                mAnimating = true
            }
        }
        mViewList[0].visibility = View.VISIBLE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(mImageSize, mImageSize * 3)
    }
}