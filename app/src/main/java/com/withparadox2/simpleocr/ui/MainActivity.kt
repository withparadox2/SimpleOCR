package com.withparadox2.simpleocr.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.support.permission.PermissionManager
import com.withparadox2.simpleocr.util.buildUri
import com.withparadox2.simpleocr.util.getBasePath
import java.io.File

const val REQUEST_TAKE_PIC = 1

class MainActivity : BaseActivity(), View.OnClickListener {
    var mFilePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btn = findViewById(R.id.btn_take_pic)
        btn.setOnClickListener(this)
        PermissionManager.getInstance().requestPermission(this, Runnable {}, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    override fun onClick(v: View?) {
        if (v == null) return
        when (v.id) {
            R.id.btn_take_pic -> {
                mFilePath = "${getBasePath()}${System.currentTimeMillis()}.jpg"
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, buildUri(this, File(mFilePath), intent))
                startActivityForResult(intent, REQUEST_TAKE_PIC)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        when (requestCode) {
            REQUEST_TAKE_PIC -> {
            }
        }
    }
}
