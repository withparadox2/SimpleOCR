package com.withparadox2.simpleocr.ui

import android.app.Activity
import android.graphics.Point
import android.hardware.Camera
import android.hardware.Camera.getCameraInfo
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import com.withparadox2.simpleocr.R

@Suppress("DEPRECATION")
/**
 * Created by withparadox2 on 2018/5/20.
 */
class CameraActivity : BaseActivity(), View.OnClickListener {

    private lateinit var mCameraView: SurfaceView
    private lateinit var mBtnShutter: Button
    private lateinit var mCamera: Camera
    private var mCameraId: Int = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        mCameraView = findViewById(R.id.surface_view) as SurfaceView
        mBtnShutter = findViewById(R.id.btn_shutter) as Button

        mCameraView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
            }

            override fun surfaceCreated(holder: SurfaceHolder?) {
                setupCamera()
            }
        })
        mCameraView.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    private fun setupCamera() {
        mCameraId = getCameraId()
        if (mCameraId >= 0) {
            mCamera = Camera.open()
            setCameraDisplayOrientation(this, mCameraId, mCamera)
            configCamera()
            mCamera.setPreviewDisplay(mCameraView.holder)
            mCamera.startPreview()
        }
    }

    private fun configCamera() {
        var info = Camera.CameraInfo()
        Camera.getCameraInfo(mCameraId, info)
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val targetRatio: Float = metrics.heightPixels.toFloat() / metrics.widthPixels
        var minDiff: Float = Int.MAX_VALUE.toFloat()

        val finalSize = Point()
        val needReverse = info.orientation == 90 || info.orientation == 270

        val parameters = mCamera.parameters
        parameters.supportedPreviewSizes.forEach {
            val ratio = if (!needReverse) it.height.toFloat() / it.width else it.width.toFloat() / it.height
            val diff = Math.abs(ratio - targetRatio)
            if (diff < minDiff) {
                minDiff = diff
                finalSize.x = it.width
                finalSize.y = it.height
            }
        }

        parameters.setPreviewSize(finalSize.x, finalSize.y)
        parameters.setPictureSize(finalSize.x, finalSize.y)
        mCamera.parameters = parameters
    }

    override fun onPause() {
        super.onPause()
        mCamera.release()
    }

    override fun onResume() {
        super.onResume()
    }


    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.btn_shutter -> {
            }
        }
    }

    private fun getCameraId(): Int {
        val numberOfCameras = Camera.getNumberOfCameras()
        val cameraInfo = Camera.CameraInfo()
        for (i in 0 until numberOfCameras) {
            getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i
            }
        }
        return -1
    }

    private fun setCameraDisplayOrientation(activity: Activity,
                                            cameraId: Int, camera: Camera) {
        val info: Camera.CameraInfo = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        val rotation = activity.windowManager.defaultDisplay
                .rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }

        var result: Int
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360
        }
        camera.setDisplayOrientation(result)
    }
}