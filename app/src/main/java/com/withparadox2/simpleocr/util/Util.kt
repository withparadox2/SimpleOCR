package com.withparadox2.simpleocr.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.withparadox2.simpleocr.App
import java.io.File

/**
 * Created by withparadox2 on 2018/3/15.
 */
fun getBasePath(): String {
    val basePath = Environment.getExternalStorageDirectory().absolutePath + "/simpleocr/"
    File(basePath).mkdirs()
    return basePath
}

fun getTemplateBasePath(): String {
    return getBasePath() + "template/"
}

fun getTempBitmapPath(): String {
    return getBasePath() + "temp.jpg"
}

fun buildUri(context: Context, file: File, intent: Intent?): Uri {
    val uri: Uri
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N/*android 7.0*/) {
        uri = FileProvider.getUriForFile(context, "com.withparadox2.simpleocr.provider", file)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    } else {
        uri = Uri.fromFile(file)
    }
    return uri
}

fun toast(text: String?) = Toast.makeText(App.instance, text, Toast.LENGTH_SHORT).show()

fun dp2px(dip: Int, context: Context = App.instance): Int {
    val scale = context.resources.displayMetrics.density
    return (dip * scale + 0.5f).toInt()
}

fun dp2px(dip: Float, context: Context = App.instance): Float {
    val scale = context.resources.displayMetrics.density
    return (dip * scale + 0.5f)
}

fun executeAsync(action: Runnable) {
    AsyncTask.THREAD_POOL_EXECUTOR.execute(action)
}

fun getSp(): SharedPreferences {
    return App.instance.getSharedPreferences(App.instance.packageName, Context.MODE_PRIVATE)
}

fun saveSpString(key: String, value: String) {
    getSp().edit().putString(key, value).apply()
}

fun getSpString(key: String, defaultValue: String): String {
    return getSp().getString(key, defaultValue)!!
}

fun getVersionCode(): Int {
    val pInfo = App.instance.getPackageManager().getPackageInfo(App.instance.getPackageName(), 0)
    return pInfo.versionCode
}

fun hideKeyboard(activity: Activity, view: View?) {
    try {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken ?: activity.currentFocus?.windowToken, 0)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun showKeyboard(activity: Activity, view: View?) {
    try {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_FORCED)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}