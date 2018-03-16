package com.withparadox2.simpleocr.ui

import android.support.v7.app.AppCompatActivity
import com.withparadox2.simpleocr.support.permission.PermissionManager

open class BaseActivity : AppCompatActivity() {
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionManager.getInstance().handlePermissionResult(requestCode, permissions, grantResults)
    }
}
