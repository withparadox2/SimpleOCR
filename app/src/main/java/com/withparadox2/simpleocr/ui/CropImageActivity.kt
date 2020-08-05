package com.withparadox2.simpleocr.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.core.view.doOnNextLayout
import com.withparadox2.simpleocr.App
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.support.net.OcrResult
import com.withparadox2.simpleocr.support.net.OcrService
import com.withparadox2.simpleocr.support.view.CropImageView
import com.withparadox2.simpleocr.support.view.CropRotationWheel
import com.withparadox2.simpleocr.support.view.LoadingView
import com.withparadox2.simpleocr.util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream


const val PHOTO_OCR_NAME = "prepare_ocr.jpg"

class CropImageActivity : BaseActivity(), View.OnClickListener {
  private var mFilePath: String? = null
  private var mOcrPath: String? = null
  private val ivPhoto: CropImageView by bind(R.id.iv_photo)
  private val progressBar: LoadingView by bind(R.id.progressbar)
  private val btnOcr: View by bind(R.id.btn_ocr)
  private val rotationWheel: CropRotationWheel by bind(R.id.layout_wheel)
  private var mOcrRequest: Call<*>? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_crop_image)

    btnOcr.setOnClickListener(this)
    findViewById<View>(R.id.btn_cancel).setOnClickListener(this)
    findViewById<View>(R.id.btn_reset).setOnClickListener(this)
    findViewById<View>(R.id.btn_rotate).setOnClickListener(this)

    val requestPath = intent.getStringExtra("path")
    mFilePath = requestPath ?: getTempBitmapPath()
    mOcrPath = "${getBasePath()}$PHOTO_OCR_NAME"
    showPhotoIfExist()
    rotationWheel.setCallback(ivPhoto)
  }

  override fun onClick(v: View?) {
    if (v == null) return
    when (v.id) {
      R.id.btn_ocr -> {
        val bitmap = ivPhoto.getCropBitmap()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, FileOutputStream(mOcrPath))
        startOcr()
      }
      R.id.btn_cancel -> finish()
      R.id.btn_reset -> {
        rotationWheel.reset()
        ivPhoto.reset()
      }
      R.id.btn_rotate -> {
        rotationWheel.reset()
        ivPhoto.rotate()
      }
    }
  }

  private fun showPhotoIfExist() {
    if (mFilePath == null || !File(mFilePath).exists()) {
      return
    }
    if (ivPhoto.width == 0 || ivPhoto.height == 0) {
      ivPhoto.doOnNextLayout { showPhoto() }
    } else {
      showPhoto()
    }
  }

  private fun showPhoto() = Thread {
    val bitmap: Bitmap = getBitmap(mFilePath!!)
    App.post(Runnable {
      ivPhoto.setImageBitmap(bitmap)
    })
  }.run()

  private fun startOcr() {
    progressBar.show(Runnable {
      val image = Base64.encodeToString(File(mOcrPath).readBytes(), Base64.DEFAULT)
      mOcrRequest = OcrService.requestOcr(image, object : Callback<OcrResult> {
        override fun onFailure(call: Call<OcrResult>?, t: Throwable?) {
          if (progressBar.isShow() && !progressBar.isAnimating()) {
            progressBar.hide()
            toast("Ocr error")
          }
        }

        override fun onResponse(call: Call<OcrResult>?, response: Response<OcrResult>?) {
          progressBar.hide(Runnable {
            parseText(response?.body()?.resultList).also {
              setResult(Activity.RESULT_OK, Intent().putExtra("data", it))
              finish()
            }
          })
        }
      })
    })
  }

  override fun onBackPressed() {
    if (progressBar.isShow()) {
      progressBar.hide()
      mOcrRequest?.cancel()
    } else {
      super.onBackPressed()
    }
  }
}

fun getCropIntent(context: Context, path: String? = null): Intent {
  return Intent(context, CropImageActivity::class.java).apply {
    putExtra("path", path)
  }
}