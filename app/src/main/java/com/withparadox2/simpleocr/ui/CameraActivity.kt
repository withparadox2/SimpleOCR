@file:Suppress("DEPRECATION")

package com.withparadox2.simpleocr.ui

import android.Manifest
import android.content.Intent
import android.hardware.Camera
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.support.camera.CameraController
import com.withparadox2.simpleocr.support.camera.CameraView
import com.withparadox2.simpleocr.support.permission.PermissionManager
import com.withparadox2.simpleocr.support.view.FlashSwitch
import com.withparadox2.simpleocr.support.view.ShutterButton
import com.withparadox2.simpleocr.util.*
import kotlinx.coroutines.GlobalScope

/**
 * Created by withparadox2 on 2018/5/20.
 */
class CameraActivity : BaseActivity(), View.OnClickListener {
    private val mBtnShutter: ShutterButton by bind(R.id.btn_shutter)
    private val mBtnFlash : FlashSwitch by bind(R.id.btn_flash_switch)

    private lateinit var mCameraView: CameraView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        mCameraView = CameraView(this)
        mCameraView.setCameraCallback(object : CameraController.Callback {
            override fun onOpenSuccess(camera: Camera) {
                mBtnFlash.setFlashModeList(CameraController.instance.getFlashModes())
            }

            override fun onOpenFailed() {
                toast("Failed to open camera")
            }
        })

        val container = findViewById<ViewGroup>(R.id.layout_container)
        container.addView(mCameraView, 0)
        mBtnShutter.setOnClickListener(this)

        PermissionManager.instance.requestPermission(this, object : PermissionManager.PermissionCallback{
            override fun onDenied() {
                toast("SimpleOCR can not work without relevant permissions")
            }

            override fun onGranted() {
                mCameraView.onPermissionGranted()
            }

        }, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.btn_shutter -> {
                CameraController.instance.getCamera()?.takePicture(null, null, Camera.PictureCallback { data, _ ->
                    GlobalScope.launchUI {
                        if (writeToFile(data, getTempBitmapPath())) {
                            startActivity(Intent(this@CameraActivity, CropImageActivity::class.java))
                        }
                    }
                })
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mCameraView.onPause()
    }

    override fun onResume() {
        super.onResume()
        mCameraView.onResume()
    }
}