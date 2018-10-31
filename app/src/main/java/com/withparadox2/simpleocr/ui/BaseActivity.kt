package com.withparadox2.simpleocr.ui

import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import com.withparadox2.simpleocr.support.permission.PermissionManager

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionManager.instance.handlePermissionResult(requestCode, permissions, grantResults)
    }
}
