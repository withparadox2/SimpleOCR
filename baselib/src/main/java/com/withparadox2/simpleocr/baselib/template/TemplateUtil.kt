package com.withparadox2.simpleocr.baselib.template

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources


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