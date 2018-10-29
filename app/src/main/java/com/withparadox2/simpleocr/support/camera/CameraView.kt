package com.withparadox2.simpleocr.support.camera

import android.content.Context
import android.graphics.*
import android.hardware.Camera
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.TextureView
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.FrameLayout
import com.withparadox2.simpleocr.util.dp2px

class CameraView(context: Context) : FrameLayout(context), TextureView.SurfaceTextureListener {
    private val textureView: TextureView = TextureView(context)

    // tap circle
    private var focusAreaSize = dp2px(96)
    private var lastDrawTime: Long = System.currentTimeMillis()
    private var focusProgress: Float = 0.0f
    private var innerAlpha: Float = 0.0f
    private var outerAlpha: Float = 0.0f
    private val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val interpolator: Interpolator = DecelerateInterpolator()
    private var cx = 0
    private var cy = 0

    init {
        setWillNotDraw(false)
        textureView.surfaceTextureListener = this
        addView(textureView)

        outerPaint.color = Color.WHITE
        outerPaint.style = Paint.Style.STROKE
        outerPaint.strokeWidth = dp2px(2).toFloat()
        innerPaint.color = 0x7fffffff
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        CameraManager.instance.closeCamera()
        return true
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        val surfaceTexture = textureView.surfaceTexture
        surfaceTexture.setDefaultBufferSize(width, height)

        CameraManager.instance.openCamera(textureView, object : CameraManager.Callback {
            override fun onOpenSuccess(camera: Camera) {
            }

            override fun onOpenFailed() {
            }
        })
    }

    fun onPause() {
        CameraManager.instance.closeCamera()
    }

    fun onResume() {
        if (textureView.isAvailable) {
            onSurfaceTextureAvailable(textureView.surfaceTexture, 0, 0)
        } else {
            textureView.surfaceTextureListener = this
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            try {
                focusToPoint(event.x.toInt(), event.y.toInt())
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        return true
    }

    private fun focusToPoint(x: Int, y: Int) {
        val focusRect = calculateTapArea(x.toFloat(), y.toFloat(), 1f)
        val meteringRect = calculateTapArea(x.toFloat(), y.toFloat(), 1.5f)

//        if (cameraSession != null) {
//            cameraSession.focusToRect(focusRect, meteringRect)
//        }

        focusProgress = 0.0f
        innerAlpha = 1.0f
        outerAlpha = 1.0f
        cx = x
        cy = y
        lastDrawTime = System.currentTimeMillis()
        invalidate()
    }

    private fun calculateTapArea(x: Float, y: Float, coefficient: Float): Rect {
        val areaSize = java.lang.Float.valueOf(focusAreaSize * coefficient).toInt()

        val left = clamp(x.toInt() - areaSize / 2, 0, width - areaSize)
        val top = clamp(y.toInt() - areaSize / 2, 0, height - areaSize)

        val rectF = RectF(left.toFloat(), top.toFloat(), (left + areaSize).toFloat(), (top + areaSize).toFloat())
        matrix.mapRect(rectF)

        return Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom))
    }

    private fun clamp(x: Int, min: Int, max: Int): Int {
        if (x > max) {
            return max
        }
        return if (x < min) {
            min
        } else x
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (focusProgress != 1.0f || innerAlpha != 0.0f || outerAlpha != 0.0f) {
            val baseRad = dp2px(30)
            val newTime = System.currentTimeMillis()
            var dt = newTime - lastDrawTime
            if (dt < 0 || dt > 17) {
                dt = 17
            }
            lastDrawTime = newTime
            outerPaint.alpha = (interpolator.getInterpolation(outerAlpha) * 255).toInt()
            innerPaint.alpha = (interpolator.getInterpolation(innerAlpha) * 127).toInt()
            val interpolated = interpolator.getInterpolation(focusProgress)
            canvas.drawCircle(cx.toFloat(), cy.toFloat(), baseRad + baseRad * (1.0f - interpolated), outerPaint)
            canvas.drawCircle(cx.toFloat(), cy.toFloat(), baseRad * interpolated, innerPaint)

            if (focusProgress < 1) {
                focusProgress += dt / 200.0f
                if (focusProgress > 1) {
                    focusProgress = 1f
                }
                invalidate()
            } else if (innerAlpha != 0f) {
                innerAlpha -= dt / 150.0f
                if (innerAlpha < 0) {
                    innerAlpha = 0f
                }
                invalidate()
            } else if (outerAlpha != 0f) {
                outerAlpha -= dt / 150.0f
                if (outerAlpha < 0) {
                    outerAlpha = 0f
                }
                invalidate()
            }
        }
    }
}