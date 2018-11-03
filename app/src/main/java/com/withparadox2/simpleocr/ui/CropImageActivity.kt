package com.withparadox2.simpleocr.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ProgressBar
import com.withparadox2.simpleocr.App
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.support.net.OcrResult
import com.withparadox2.simpleocr.support.net.OcrService
import com.withparadox2.simpleocr.support.view.CropImageView
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
    private lateinit var ivPhoto: CropImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnOcr: View

    private var mOcrText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_image)

        btnOcr = findViewById(R.id.btn_ocr)
        btnOcr.setOnClickListener(this)

        findViewById(R.id.btn_cancel).setOnClickListener(this)

        ivPhoto = findViewById(R.id.iv_photo) as CropImageView
        progressBar = findViewById(R.id.progressbar) as ProgressBar

        mFilePath = getTempBitmapPath()
        mOcrPath = "${getBasePath()}$PHOTO_OCR_NAME"
        showPhotoIfExist()
    }

    override fun onClick(v: View?) {
        if (v == null) return
        when (v.id) {
            R.id.btn_ocr -> {
                val bitmap = ivPhoto.getCropBitmap()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, FileOutputStream(mOcrPath))
                startOcr()
            }
            R.id.btn_cancel-> finish()
        }
    }

    private fun showPhotoIfExist() {
        if (mFilePath == null || !File(mFilePath).exists()) {
            return
        }
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

    private fun showPhoto() = Thread {
        val bitmap: Bitmap = getBitmap(mFilePath!!)
        App.post(Runnable {
            ivPhoto.setImageBitmap(bitmap)
        })
    }.run()

    private fun startOcr() {
        progressBar.visibility = View.VISIBLE
        val image = Base64.encodeToString(File(mOcrPath).readBytes(), Base64.DEFAULT)
        OcrService.requestOcr(image, object : Callback<OcrResult> {
            override fun onFailure(call: Call<OcrResult>?, t: Throwable?) {
                progressBar.visibility = View.GONE
            }

            override fun onResponse(call: Call<OcrResult>?, response: Response<OcrResult>?) {
                progressBar.visibility = View.GONE
                val text: String? = parseText(response?.body()?.resultList)
                if (text != null) {
                    mOcrText = text
                    startActivity(com.withparadox2.simpleocr.ui.edit.getIntent(this@CropImageActivity, text))
                } else {
                    toast("fail")
                }
            }
        })
    }
}