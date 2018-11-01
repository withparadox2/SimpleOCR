package com.withparadox2.simpleocr.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.withparadox2.simpleocr.App
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.support.net.OcrResult
import com.withparadox2.simpleocr.support.net.OcrService
import com.withparadox2.simpleocr.support.view.CropImageView
import com.withparadox2.simpleocr.ui.edit.EditActivity
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
    private lateinit var ivPhoto: CropImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnOcr: View

    private lateinit var layoutBottom: View
    private lateinit var tvText: TextView

    private var mOcrText: String? = null
    private var mOcrTextTemp: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_image)

        btnOcr = findViewById(R.id.btn_ocr)
        btnOcr.setOnClickListener(this)

        findViewById(R.id.btn_cancel).setOnClickListener(this)

        ivPhoto = findViewById(R.id.iv_photo) as CropImageView
        progressBar = findViewById(R.id.progressbar) as ProgressBar

        layoutBottom = findViewById(R.id.layout_text)
        layoutBottom.setOnTouchListener { _, _ -> true }
        tvText = findViewById(R.id.tv_result) as TextView

        findViewById(R.id.btn_reset).setOnClickListener(this)
        findViewById(R.id.btn_join).setOnClickListener(this)
        findViewById(R.id.btn_copy).setOnClickListener(this)
        findViewById(R.id.btn_convert).setOnClickListener(this)

        mFilePath = getTempBitmapPath()
        mOcrPath = "${getBasePath()}$PHOTO_OCR_NAME"
        showPhotoIfExist()
    }

    override fun onClick(v: View?) {
        if (v == null) return
        when (v.id) {
            R.id.btn_ocr -> {
//                val bitmap = ivPhoto.getCropBitmap()
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, FileOutputStream(mOcrPath))
//                startOcr()
                startActivity(Intent(this, EditActivity::class.java))
            }
            R.id.btn_join -> {
                mOcrTextTemp = mOcrTextTemp!!.split("\n").joinToString("")
                tvText.text = mOcrTextTemp
            }
            R.id.btn_reset -> {
                mOcrTextTemp = mOcrText!!.substring(0)
                tvText.text = mOcrTextTemp
            }
            R.id.btn_copy -> {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("simpleocr", mOcrTextTemp)
                clipboard.primaryClip = clip
                toast("copy success!")
            }
            R.id.btn_convert -> {
                mOcrTextTemp = mOcrTextTemp!!.replace(",", "，")
                tvText.text = mOcrTextTemp
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
                    showBottom()
                } else {
                    toast("fail")
                }
            }
        })
    }

    private fun showBottom() {
        btnOcr.visibility = View.GONE
        layoutBottom.visibility = View.VISIBLE
        mOcrTextTemp = mOcrText!!.substring(0)
        tvText.text = mOcrTextTemp

        if (layoutBottom.measuredHeight == 0) {
            layoutBottom.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                    layoutBottom.removeOnLayoutChangeListener(this)
                    animateBottom()
                }
            })
        } else {
            animateBottom()
        }
    }

    private fun animateBottom() {
        layoutBottom.translationY = layoutBottom.measuredHeight.toFloat()
        layoutBottom.animate().translationY(0f).setDuration(200).start()
    }

    private fun hideBottom() {
        layoutBottom.animate().translationY(layoutBottom.measuredHeight.toFloat()).setDuration(150).withEndAction {
            layoutBottom.visibility = View.GONE
            btnOcr.visibility = View.VISIBLE
        }.start()
    }

    override fun onBackPressed() {
        if (layoutBottom.visibility == View.VISIBLE) {
            hideBottom()
        } else {
            super.onBackPressed()
        }
    }
}
