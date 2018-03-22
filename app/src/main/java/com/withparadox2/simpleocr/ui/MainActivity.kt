package com.withparadox2.simpleocr.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import com.withparadox2.simpleocr.App
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.support.permission.PermissionManager
import com.withparadox2.simpleocr.util.buildUri
import com.withparadox2.simpleocr.util.compress
import com.withparadox2.simpleocr.util.getBasePath
import java.io.File

const val REQUEST_TAKE_PIC = 1
const val PHOTO_NAME = "prepare_decode.jpg"

class MainActivity : BaseActivity(), View.OnClickListener {
    var mFilePath: String? = null
    lateinit var ivPhoto: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btn = findViewById(R.id.btn_take_pic)
        btn.setOnClickListener(this)
        ivPhoto = findViewById(R.id.iv_photo) as ImageView
        mFilePath = savedInstanceState?.getString("key_path")
        showPhotoIfExist()
    }

    override fun onClick(v: View?) {
        if (v == null) return
        when (v.id) {
            R.id.btn_take_pic -> {
                PermissionManager.getInstance().requestPermission(this, Runnable {
                    mFilePath = "${getBasePath()}$PHOTO_NAME"
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, buildUri(this, File(mFilePath), intent))
                    startActivityForResult(intent, REQUEST_TAKE_PIC)
                }, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putString("key_path", mFilePath)
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        when (requestCode) {
            REQUEST_TAKE_PIC -> {
                showPhotoIfExist()
            }
        }
    }

    private fun showPhotoIfExist() {
        if (mFilePath == null) {
            return
        }
        if (File(mFilePath).exists()) {
            if (ivPhoto.width == 0 || ivPhoto.height == 0) {
                ivPhoto.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                        showPhoto()
                        ivPhoto.removeOnLayoutChangeListener(this)
                    }
                })
            } else {
                showPhoto()
            }
        }
    }

    private fun showPhoto() = Thread {
        compress(mFilePath)
        val bitmap = BitmapFactory.decodeFile(mFilePath)
        App.post(Runnable {
            ivPhoto.setImageBitmap(bitmap)

            val matrix = Matrix()
            val scale: Float
            var dx = 0f
            var dy = 0f

            if ((bitmap.width / bitmap.height.toFloat() > ivPhoto.width / ivPhoto.height.toFloat())) {
                scale = ivPhoto.width.toFloat() / bitmap.width
                dy = (ivPhoto.height - bitmap.height * scale) / 2
            } else {
                scale = ivPhoto.height.toFloat() / bitmap.height
                dx = (ivPhoto.width - bitmap.width * scale) / 2
            }

            matrix.setScale(scale, scale)
            matrix.postTranslate(dx, dy)

            ivPhoto.imageMatrix = matrix
        })
    }.run()
}
