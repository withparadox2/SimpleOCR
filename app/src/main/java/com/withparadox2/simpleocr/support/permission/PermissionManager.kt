package com.withparadox2.simpleocr.support.permission;

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.SparseArray
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by withparadox2 on 2018/3/13.
 */
class PermissionManager private constructor() {
  private val sIdToAction = SparseArray<PermissionCallback>()
  private val sIdGenerator = AtomicInteger(0)

  companion object Single {
    val instance = PermissionManager()
  }

  fun hasPermission(o: Any, vararg permissions: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    return !permissions.any {
      ContextCompat.checkSelfPermission(getContext(o), it) != PackageManager.PERMISSION_GRANTED
    }
  }

  private fun getContext(o: Any): Context = when (o) {
    is Activity -> o
    is Fragment -> o.activity!!
    is android.app.Fragment -> o.activity
    else -> throw IllegalArgumentException("Can not get context from object o.")
  }

  fun requestPermission(o: Any, action: PermissionCallback?, vararg permissions: String) {
    val deniedArr = permissions.filter { !hasPermission(o, it) }
    if (deniedArr.isEmpty()) {
      action?.onGranted()
    } else {
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

  private fun getAndRemoveAction(requestCode: Int): PermissionCallback? {
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

  fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    val allGranted = permissions.indices.none { grantResults[it] != PackageManager.PERMISSION_GRANTED }
    getAndRemoveAction(requestCode)?.apply {
      if (allGranted) this.onGranted() else this.onDenied()
    }
  }

  interface PermissionCallback {
    fun onGranted()
    fun onDenied()
  }
}
