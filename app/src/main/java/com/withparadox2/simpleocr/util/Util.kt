package com.withparadox2.simpleocr.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import android.widget.Toast
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