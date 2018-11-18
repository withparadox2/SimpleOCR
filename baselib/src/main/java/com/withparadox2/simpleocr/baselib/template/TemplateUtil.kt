package com.withparadox2.simpleocr.baselib.template

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import dalvik.system.DexClassLoader
import android.content.res.AssetManager
import android.content.res.Resources


/**
 * Created by withparadox2 on 2018/11/17.
 */
const val APK_PATH = "apk_path"
const val FRAGMENT_NAME = "com.withparadox2.template.TemplateFragment"

fun loadFragmentFromApk(context: Context, apkPath: String, args: Bundle, fragmentName: String = FRAGMENT_NAME): Fragment? {
    try {
        val fragment = createDexLoader(context, apkPath).loadClass(fragmentName).newInstance() as Fragment
        fragment.arguments = args.apply {
            this.putString(APK_PATH, apkPath)
        }
        return fragment
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

fun createDexLoader(context: Context, apkPath: String): DexClassLoader {
    return DexClassLoader(apkPath, context.getDir("template_cache", 0).absolutePath, null, context.classLoader)
}

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