package com.withparadox2.simpleocr.ui

import android.content.Intent
import android.hardware.Camera
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.support.camera.CameraManager
import com.withparadox2.simpleocr.support.camera.CameraView
import com.withparadox2.simpleocr.util.getTempBitmapPath
import com.withparadox2.simpleocr.util.writeToFile

/**
 * Created by withparadox2 on 2018/5/20.
 */
class CameraActivity : BaseActivity(), View.OnClickListener {

    private lateinit var mBtnShutter: Button
    private lateinit var mCameraView: CameraView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        val container = findViewById(R.id.layout_container) as ViewGroup
        mCameraView = CameraView(this)
        container.addView(mCameraView, 0)

        mBtnShutter = findViewById(R.id.btn_shutter) as Button

        mBtnShutter.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.btn_shutter -> {
                CameraManager.instance.getCamera()?.takePicture(null, null, Camera.PictureCallback { data, _ ->
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