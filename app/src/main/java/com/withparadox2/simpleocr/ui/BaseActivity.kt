package com.withparadox2.simpleocr.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.withparadox2.simpleocr.support.permission.PermissionManager
import com.withparadox2.simpleocr.util.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var mJob: Job
    var mGetPermissions = false

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionManager.instance.handlePermissionResult(requestCode, permissions, grantResults)
    }

    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        mJob = Job()
        super.onCreate(savedInstanceState)
        checkPermission()
    }

    override fun onDestroy() {
        mJob.cancel()
        super.onDestroy()
    }

    open fun checkPermission() {
        if (!PermissionManager.instance.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)) {
            PermissionDialog(DialogInterface.OnClickListener { _, _ ->
                requestPermission()
            }).show(supportFragmentManager, "permission")
        } else {
            onGetPermission()
        }
    }

    private fun requestPermission() {
        PermissionManager.instance.requestPermission(this, object : PermissionManager.PermissionCallback {
            override fun onDenied() {
                toast("SimpleOCR can not work without necessary permissions")
            }

            override fun onGranted() {
                onGetPermission()
            }

        }, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
    }

    open fun onGetPermission() {
        mGetPermissions = true
    }
}
