package com.withparadox2.simpleocr.ui

import android.app.Activity
import android.content.Intent
import android.hardware.Camera
import android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
import android.hardware.Camera.getCameraInfo
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.util.getTempBitmapPath
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.OutputStream

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
        mBtnShutter.setOnClickListener(this)
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
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(mCameraId, info)

        var targetRatio: Float = getScreenRatio()
        if (info.orientation == 90 || info.orientation == 270) {
            targetRatio = 1 / targetRatio
        }

        val parameters = mCamera.parameters
        val finalSize = getOptimizeSize(parameters.supportedPreviewSizes, targetRatio)

        if (finalSize != null) {
            parameters.setPreviewSize(finalSize.width, finalSize.height)
            parameters.setPictureSize(finalSize.width, finalSize.height)
        }
        parameters.focusMode = FOCUS_MODE_CONTINUOUS_PICTURE
        //TODO detect the real rotation
        parameters.setRotation(90)

        mCamera.parameters = parameters
    }

    override fun onPause() {
        super.onPause()
        mCamera.release()
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.btn_shutter -> {
                mCamera.takePicture(null, null, Camera.PictureCallback { data, camera ->
                    var output: OutputStream? = null
                    try {
                        output = BufferedOutputStream(FileOutputStream(getTempBitmapPath()))
                        output.write(data)
                        output.flush()
                        startActivity(Intent(this@CameraActivity, MainActivity::class.java))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        output?.close()
                    }
                })
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
            result = (360 - result) % 360  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360
        }
        camera.setDisplayOrientation(result)
    }

    private fun getScreenRatio(): Float {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        return metrics.heightPixels.toFloat() / metrics.widthPixels
    }

    private fun getOptimizeSize(sizeList: List<Camera.Size>, targetRatio: Float): Camera.Size? {
        var finalSize: Camera.Size? = null
        var minDiff: Float = Int.MAX_VALUE.toFloat()

        for (size in sizeList) {
            val ratio = size.height.toFloat() / size.width
            val diff = Math.abs(ratio - targetRatio)
            if (Math.abs(diff - minDiff) < 0.1) {
                continue
            }
            if (diff < minDiff) {
                minDiff = diff
                finalSize = size
            }
        }
        return finalSize
    }
}