package com.withparadox2.simpleocr.support.camera

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.hardware.Camera
import android.util.DisplayMetrics
import android.view.OrientationEventListener
import android.view.Surface
import android.view.TextureView
import com.withparadox2.simpleocr.App
import kotlin.concurrent.thread


class CameraController private constructor() {
  private var mCamera: Camera? = null
  private var mCameraInfo: Camera.CameraInfo? = null
  private var mOrientationListener: OrientationEventListener? = null

  fun openCamera(textureView: TextureView, callback: Callback) {
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
        configCamera(camera, cameraId, textureView.context)
        camera.setPreviewTexture(textureView.surfaceTexture)
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
            synchronized(this) {
              mCamera?.apply { callback.onOpenSuccess(camera) }
            }
          })
        } else {
          App.post(Runnable {
            callback.onOpenFailed()
          })
        }
      }
    }
  }

  private fun configCamera(camera: Camera, cameraId: Int, context: Context) {

    mOrientationListener?.disable()
    mOrientationListener = OrientationEventListenerImpl(context)
    mOrientationListener?.enable()

    setCameraDisplayOrientation(context as Activity, cameraId, camera)

    val info = Camera.CameraInfo()
    Camera.getCameraInfo(cameraId, info)
    mCameraInfo = info
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
    parameters.setRotation(90)

    try {
      camera.parameters = parameters
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun getCamera(): Camera? {
    return mCamera
  }

  fun getCameraInfo(): Camera.CameraInfo? {
    return mCameraInfo
  }

  fun onResume() {
    mOrientationListener?.enable()
  }

  fun onPause() {
    mOrientationListener?.disable()
  }

  @Synchronized
  fun closeCamera() {
    val camera = mCamera
    if (camera != null) {
      try {
        camera.setPreviewCallbackWithBuffer(null)
        camera.stopPreview()
      } catch (e: Exception) {
        e.printStackTrace()
      }
      camera.release()
      mCamera = null
    }
  }

  fun focusToRect(focusRect: Rect, meteringRect: Rect) {
    try {
      val camera = mCamera
      if (camera != null) {
        camera.cancelAutoFocus()
        var parameters: Camera.Parameters? = null
        try {
          parameters = camera.parameters
        } catch (e: Exception) {
          e.printStackTrace()
        }

        if (parameters != null) {
          parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
          var meteringAreas = ArrayList<Camera.Area>()
          meteringAreas.add(Camera.Area(focusRect, 1000))
          parameters.focusAreas = meteringAreas

          if (parameters.maxNumMeteringAreas > 0) {
            meteringAreas = ArrayList()
            meteringAreas.add(Camera.Area(meteringRect, 1000))
            parameters.meteringAreas = meteringAreas
          }

          try {
            camera.parameters = parameters
            camera.autoFocus { success, _ ->
              if (success) {
              } else {
              }
            }
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

  }

  fun getFlashModes(): List<String> {
    val rawFlashModes = mCamera?.parameters?.supportedFlashModes
    if (rawFlashModes != null) {
      return rawFlashModes.filter {
        it == Camera.Parameters.FLASH_MODE_AUTO || it == Camera.Parameters.FLASH_MODE_ON || it == Camera.Parameters.FLASH_MODE_OFF
      }
    }
    return ArrayList()
  }

  fun setFlashMode(flashMode: String) {
    setParameters(mCamera, mCamera?.parameters?.apply { this.flashMode = flashMode })
  }

  private fun setParameters(camera: Camera?, parameters: Camera.Parameters?) {
    try {
      if (parameters != null) camera?.parameters = parameters
    } catch (e: Exception) {
      e.printStackTrace()
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

  private fun getScreenRatio(activity: Activity): Float {
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
    val instance = CameraController()
  }

  interface Callback {
    fun onOpenSuccess(camera: Camera)
    fun onOpenFailed()
  }


  private var mLastRotation = Int.MAX_VALUE

  private inner class OrientationEventListenerImpl(context: Context) : OrientationEventListener(context) {
    override fun onOrientationChanged(orientationP: Int) {
      if (OrientationEventListener.ORIENTATION_UNKNOWN == orientationP) {
        return
      }
      val info = getCameraInfo() ?: return

      var orientation = (orientationP + 45) / 90 * 90
      val rotation = if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
        (info.orientation - orientation + 360) % 360
      } else {
        (info.orientation + orientation) % 360
      }

      if (mLastRotation != rotation && (rotation == 0 || rotation == 90 || rotation == 180
              || rotation == 270)) {
        setParameters(getCamera(), getCamera()?.parameters?.apply {
          this.setRotation(rotation)
        })
      }
      mLastRotation = rotation
    }
  }
}