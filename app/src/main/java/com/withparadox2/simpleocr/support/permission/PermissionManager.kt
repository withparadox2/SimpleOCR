package com.withparadox2.simpleocr.support.permission;

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.SparseArray
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by withparadox2 on 2018/3/13.
 */
class PermissionManager private constructor() {
    private val sIdToAction = SparseArray<Runnable>()
    private val sIdGenerator = AtomicInteger(0)

    companion object Single {
        private val mInstance: PermissionManager = PermissionManager()
        fun getInstance(): PermissionManager = mInstance
    }

    fun hasPermission(o: Any, vararg permissions: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        return !permissions.any {
            ContextCompat.checkSelfPermission(getContext(o), it) != PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getContext(o: Any): Context = when (o) {
        is Activity -> o
        is Fragment -> o.activity
        is android.app.Fragment -> o.activity
        else -> throw IllegalArgumentException("Can not get context from object o.")
    }

    fun requestPermission(o: Any, action: Runnable?, vararg permissions: String) {
        val deniedArr = permissions.filter { !hasPermission(o, it) }
        if (deniedArr.isEmpty()) action?.run()
        else {
            val requestCode = newRequestCode()
            if (action != null) {
                sIdToAction.put(requestCode, action)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissionInternal(o, requestCode, *deniedArr.toTypedArray())
            }
        }
    }

    private fun newRequestCode() = sIdGenerator.getAndIncrement()

    private fun getAndRemoveAction(requestCode: Int): Runnable? {
        val action = sIdToAction.get(requestCode)
        if (action != null) sIdToAction.delete(requestCode)
        return action
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestPermissionInternal(o: Any, requestCode: Int, vararg permissions: String) {

        when (o) {
            is Activity -> ActivityCompat.requestPermissions(o, permissions, requestCode)
            is Fragment -> o.requestPermissions(permissions, requestCode)
            is android.app.Fragment -> o.requestPermissions(permissions, requestCode)
        }
    }

    fun handlePermissionResult(requestCode : Int, permissions : Array<out String>, grantResults : IntArray) {
        val allGranted = (0 until permissions.size).none { grantResults[it] != PackageManager.PERMISSION_GRANTED }
        if (allGranted) {
            getAndRemoveAction (requestCode)?.run()
        }
    }
}
