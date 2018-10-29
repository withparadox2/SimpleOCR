package com.withparadox2.simpleocr.ui

import android.content.Intent
import android.hardware.Camera
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.support.camera.CameraManager
import com.withparadox2.simpleocr.util.getTempBitmapPath
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Created by withparadox2 on 2018/5/20.
 */
class CameraActivity : BaseActivity(), View.OnClickListener {

    private lateinit var mCameraView: SurfaceView
    private lateinit var mBtnShutter: Button
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
                CameraManager.instance.openCamera(mCameraView, object : CameraManager.Callback {
                    override fun onOpenSuccess(camera: Camera) {
                    }

                    override fun onOpenFailed() {
                    }
                })
            }
        })
        mCameraView.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        mBtnShutter.setOnClickListener(this)
    }


    override fun onPause() {
        super.onPause()
        CameraManager.instance.closeCamera()
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.btn_shutter -> {
                CameraManager.instance.getCamera()?.takePicture(null, null, Camera.PictureCallback { data, _ ->
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
}