@file:Suppress("DEPRECATION")

package com.withparadox2.simpleocr.ui

import android.Manifest
import android.app.Activity
import android.content.Context
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
import com.withparadox2.simpleocr.ui.edit.getEditIntent
import com.withparadox2.simpleocr.util.*
import kotlinx.coroutines.GlobalScope

/**
 * Created by withparadox2 on 2018/5/20.
 */
private const val REQUEST_CROP = 1
private const val REQUEST_SELECT_PICTURE = 2

class CameraActivity : BaseActivity(), View.OnClickListener {
    private val mBtnShutter: ShutterButton by bind(R.id.btn_shutter)
    private val mBtnFlash: FlashSwitch by bind(R.id.btn_flash_switch)
    private val mBtnPhoto: View by bind(R.id.btn_photo)

    private lateinit var mCameraView: CameraView
    /**
     * We may want to request more text in edit-page, which relies on
     * this flag to determine what will we do next, either show edit-page
     * or just return data back.
     */
    private var mRequestOcr = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        mRequestOcr = intent.getBooleanExtra("requestOcr", false)
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
        mBtnPhoto.setOnClickListener(this)

        PermissionManager.instance.requestPermission(this, object : PermissionManager.PermissionCallback {
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
                            startActivityForResult(getCropIntent(this@CameraActivity), REQUEST_CROP)
                        }
                    }
                })
            }
            R.id.btn_photo -> {
                startActivityForResult(Intent.createChooser(Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                }, "Select Picture"), REQUEST_SELECT_PICTURE)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CROP -> {
                    val text = data?.getStringExtra("data") ?: ""
                    if (mRequestOcr) {
                        setResult(Activity.RESULT_OK, Intent().putExtra("data", text))
                        finish()
                    } else {
                        startActivity(getEditIntent(this, text))
                    }
                }
                REQUEST_SELECT_PICTURE -> {
                    data?.data?.also { it ->
                        decodePathFromUri(this, it)?.also {
                            startActivityForResult(getCropIntent(this, it), REQUEST_CROP)
                        }
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}

fun getCameraIntent(context: Context, requestOcr: Boolean = true) = Intent(context, CameraActivity::class.java)
        .also { it.putExtra("requestOcr", requestOcr) }