package com.withparadox2.simpleocr.baselib.template

import android.app.Activity
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import java.io.Closeable


/**
 * Created by withparadox2 on 2018/11/17.
 */
const val APK_PATH = "apk_path"

fun createAssetManager(dexPath: String): AssetManager? {
    try {
        val assetManager = AssetManager::class.java.newInstance()
        assetManager::class.java.getMethod("addAssetPath", String::class.java).invoke(assetManager, dexPath)
        return assetManager
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
    return null
}

fun createResource(context: Context?, assetManager: AssetManager?): Resources? {
    if (context == null || assetManager == null) return null
    return Resources(assetManager, context.resources.displayMetrics, context.resources.configuration)
}


fun closeQuietly(close: Closeable?) {
    try {
        close?.close()
    } catch (e: Exception) {
    }
}

fun dp2px(dip: Int, context: Context): Int {
    val scale = context.resources.displayMetrics.density
    return (dip * scale + 0.5f).toInt()
}

fun dp2px(dip: Float, context: Context): Float {
    val scale = context.resources.displayMetrics.density
    return (dip * scale + 0.5f)
}

var sSaveClassloader: ClassLoader? = null
fun changeClassLoader(context: Context, classLoader: ClassLoader?) {
    if (classLoader != null) {
        sSaveClassloader = context.classLoader
        setClassLoader(context, classLoader)
    }
}

fun restoreClassLoader(context: Context) {
    val loader = sSaveClassloader
    if (loader != null) {
        setClassLoader(context, loader)
    }
    sSaveClassloader = null
}

fun setClassLoader(context: Context, classLoader: ClassLoader) {
    try {
        val baseContext = (context as Activity).baseContext
        val field = baseContext.javaClass.getDeclaredField("mClassLoader")
        field.isAccessible = true
        field.set(baseContext, classLoader)
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}