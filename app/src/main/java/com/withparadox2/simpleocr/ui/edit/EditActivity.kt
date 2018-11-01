package com.withparadox2.simpleocr.ui.edit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import com.withparadox2.simpleocr.App
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.ui.BaseActivity
import com.withparadox2.simpleocr.util.closeQuietly
import com.withparadox2.simpleocr.util.getBasePath
import com.withparadox2.simpleocr.util.toast
import java.io.FileOutputStream

class EditActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        App.postDelayed(Runnable {
            val view = findViewById(R.id.layout_container)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            val outputStream = FileOutputStream(getBasePath() + "temp2.jpg")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            closeQuietly(outputStream)
            toast("finish")
        }, 1000)
    }


}