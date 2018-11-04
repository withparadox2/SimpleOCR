package com.withparadox2.simpleocr.support.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.hardware.Camera
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.support.camera.CameraController
import com.withparadox2.simpleocr.util.dp2px

class FlashSwitch(context: Context, attr: AttributeSet) : FrameLayout(context, attr) {
    private val mViewList: Array<ImageView> = Array(2) {
        ImageView(context)
    }
    private val mImageSize = dp2px(48)
    private var mAnimating: Boolean = false

    private val mFlashModeList = ArrayList<String>()
    private var mCurrentFlashMode : String? = null

    init {
        mViewList.forEach { view ->
            view.scaleType = ImageView.ScaleType.CENTER
            view.visibility = View.INVISIBLE
            addView(view, FrameLayout.LayoutParams(mImageSize, mImageSize, Gravity.CENTER_VERTICAL))
            view.setOnClickListener { currentImage ->
                if (CameraController.instance.getCamera() == null || mAnimating) {
                    return@setOnClickListener
                }

                val nextImage = if (currentImage == mViewList[0]) {
                    mViewList[1]
                } else {
                    mViewList[0]
                }


                val nextMode = getNextFlashMode()
                if (mCurrentFlashMode == nextMode || nextMode == null) {
                    return@setOnClickListener
                }
                setFlashMode(nextMode, nextImage)

                val animatorSet = AnimatorSet()
                animatorSet.playTogether(
                        ObjectAnimator.ofFloat(currentImage, "translationY", 0.0f, mImageSize.toFloat()),
                        ObjectAnimator.ofFloat(nextImage, "translationY", -mImageSize.toFloat(), 0.0f),
                        ObjectAnimator.ofFloat(currentImage, "alpha", 1.0f, 0.0f),
                        ObjectAnimator.ofFloat(nextImage, "alpha", 0.0f, 1.0f))
                animatorSet.duration = 200
                animatorSet.doOnEnd {
                    currentImage.visibility = View.INVISIBLE
                    mAnimating = false
                }
                animatorSet.start()
                mAnimating = true
            }
        }
    }

    fun setFlashModeList(flashModes: List<String>) {
        if (mFlashModeList.size == 0) {
            mFlashModeList.addAll(flashModes)
            if (mFlashModeList.size > 0) {
                setFlashMode(mFlashModeList[0], mViewList[0])
            }
        } else {
            setFlashMode(mCurrentFlashMode!!, mViewList[0])
        }
    }

    private fun setFlashMode(mode : String, imageView: ImageView) {
        imageView.setImageResource(getImageRes(mode))
        imageView.visibility = View.VISIBLE
        CameraController.instance.setFlashMode(mode)
        mCurrentFlashMode = mode
    }

    private fun getNextFlashMode() : String? {
        if (mFlashModeList.size == 0) return null
        // we have set mCurrentFlashMode once after getting flash mode list
        // @see setFlashModeList
        assert(mCurrentFlashMode != null)

        val index = mFlashModeList.indexOf(mCurrentFlashMode)
        val nextIndex = if (index == mFlashModeList.size - 1) 0 else index + 1
        return mFlashModeList[nextIndex]
    }

    private fun getImageRes(mode: String): Int {
        return when (mode) {
            Camera.Parameters.FLASH_MODE_OFF -> R.drawable.flash_off
            Camera.Parameters.FLASH_MODE_ON -> R.drawable.flash_on
            else -> R.drawable.flash_auto
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(mImageSize, mImageSize * 3)
    }
}