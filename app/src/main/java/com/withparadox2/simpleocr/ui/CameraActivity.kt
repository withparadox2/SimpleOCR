package com.withparadox2.simpleocr.ui

import android.content.Intent
import android.hardware.Camera
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.support.camera.CameraController
import com.withparadox2.simpleocr.support.camera.CameraView
import com.withparadox2.simpleocr.support.view.FlashSwitch
import com.withparadox2.simpleocr.support.view.ShutterButton
import com.withparadox2.simpleocr.util.getTempBitmapPath
import com.withparadox2.simpleocr.util.writeToFile

/**
 * Created by withparadox2 on 2018/5/20.
 */
class CameraActivity : BaseActivity(), View.OnClickListener {

    private lateinit var mBtnShutter: ShutterButton
    private lateinit var mCameraView: CameraView
    private lateinit var mBtnFlash : FlashSwitch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        mBtnFlash = findViewById(R.id.btn_flash_switch) as FlashSwitch
        mBtnShutter = findViewById(R.id.btn_shutter) as ShutterButton

        mCameraView = CameraView(this)
        mCameraView.setCameraCallback(object : CameraController.Callback {
            override fun onOpenSuccess(camera: Camera) {
                mBtnFlash.setFlashModeList(CameraController.instance.getFlashModes())
            }

            override fun onOpenFailed() {
            }
        })

        val container = findViewById(R.id.layout_container) as ViewGroup
        container.addView(mCameraView, 0)
        mBtnShutter.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.btn_shutter -> {
                CameraController.instance.getCamera()?.takePicture(null, null, Camera.PictureCallback { data, _ ->
                    if (writeToFile(data, getTempBitmapPath())) {
                        startActivity(Intent(this@CameraActivity, MainActivity::class.java))
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