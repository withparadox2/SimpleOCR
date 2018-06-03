package com.withparadox2.simpleocr.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Base64
import android.view.View
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

class MainActivity : BaseActivity(), View.OnClickListener {
    private var mFilePath: String? = null
    private var mOcrPath: String? = null
    lateinit var ivPhoto: CropImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_image)

        findViewById(R.id.btn_ocr).setOnClickListener(this)

        ivPhoto = findViewById(R.id.iv_photo) as CropImageView

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

            ivPhoto.setBitmapTx(dx)
            ivPhoto.setBitmapTy(dy)
            ivPhoto.setBitmapScale(scale)
        })
    }.run()


    private fun startOcr() {

        val image = Base64.encodeToString(File(mOcrPath).readBytes(), Base64.DEFAULT)
        OcrService.requestOcr(image, object : Callback<OcrResult> {
            override fun onFailure(call: Call<OcrResult>?, t: Throwable?) = Unit

            override fun onResponse(call: Call<OcrResult>?, response: Response<OcrResult>?) {
                val text: String? = parseText(response?.body()?.resultList)
                if (text != null) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("simpleocr", text)
                    clipboard.primaryClip = clip
                    toast(text)
                }  else {
                    toast("fail")
                }
            }
        })
    }
}
