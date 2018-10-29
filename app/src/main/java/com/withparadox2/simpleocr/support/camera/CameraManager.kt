package com.withparadox2.simpleocr.support.camera

import android.app.Activity
import android.content.Context
import android.hardware.Camera
import android.util.DisplayMetrics
import android.view.Surface
import android.view.SurfaceView
import com.withparadox2.simpleocr.App
import kotlin.concurrent.thread


class CameraManager private constructor() {
    private var mCamera: Camera? = null

    fun openCamera(surfaceView: SurfaceView, callback: Callback) {
        val cameraId = getCameraId()
        if (cameraId < 0) {
            callback.onOpenFailed()
            return
        }

        closeCamera()

        thread(start = true) {
            var camera: Camera? = null
            try {
                camera = Camera.open(cameraId)
                configCamera(camera, cameraId, surfaceView.context)
                //TODO make creation of camera and surface in parallel
                camera.setPreviewDisplay(surfaceView.holder)
                camera.startPreview()
            } catch (e: Exception) {
                e.printStackTrace()
                camera?.release()
                camera = null
            } finally {
                if (camera != null) {
                    synchronized(this) {
                        mCamera = camera
                    }

                    App.post(Runnable {
                        callback.onOpenSuccess(camera)
                    })
                } else {
                    App.post(Runnable {
                        callback.onOpenFailed()
                    })
                }
            }
        }
    }

    private fun configCamera(camera: Camera, cameraId : Int, context : Context) {
        setCameraDisplayOrientation(context as Activity, cameraId, camera)

        val info = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, info)

        var targetRatio: Float = getScreenRatio(context as Activity)
        if (info.orientation == 90 || info.orientation == 270) {
            targetRatio = 1 / targetRatio
        }

        val parameters = camera.parameters
        val finalSize = getOptimizeSize(parameters.supportedPreviewSizes, targetRatio)

        if (finalSize != null) {
            parameters.setPreviewSize(finalSize.width, finalSize.height)
            parameters.setPictureSize(finalSize.width, finalSize.height)
        }
        parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        //TODO detect the real rotation
        parameters.setRotation(90)

        try {
            camera.parameters = parameters
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }

    fun getCamera() : Camera? {
        return mCamera
    }

    @Synchronized
    fun closeCamera() {
        val camera = mCamera
        if (camera != null) {
            try {
                camera.stopPreview()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            camera.release()
            mCamera = null
        }
    }

    private fun getCameraId(): Int {
        val numberOfCameras = Camera.getNumberOfCameras()
        val cameraInfo = Camera.CameraInfo()
        for (i in 0 until numberOfCameras) {
            Camera.getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i
            }
        }
        return -1
    }

    private fun getScreenRatio(activity : Activity): Float {
        val metrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(metrics)
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

    companion object {
        val instance = CameraManager()
    }

    interface Callback {
        fun onOpenSuccess(camera: Camera)
        fun onOpenFailed()
    }
}