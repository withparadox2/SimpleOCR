package com.withparadox2.simpleocr.util

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import dalvik.system.DexClassLoader

const val APK_PATH = "apk_path"
const val FRAGMENT_NAME = "com.withparadox2.simpleocr.template.TemplateFragment"

fun loadFragmentFromApk(context: Context, apkPath: String, args: Bundle, fragmentName: String = FRAGMENT_NAME): Fragment? {
  try {
    val fragment = loadClassFromApk(context, apkPath, fragmentName)?.newInstance() as Fragment
    fragment.arguments = args.apply {
      this.putString(APK_PATH, apkPath)
    }
    return fragment
  } catch (e: Throwable) {
    e.printStackTrace()
  }
  return null
}

fun loadClassFromApk(context: Context, apkPath: String, fragmentName: String = FRAGMENT_NAME): Class<Fragment>? {
  try {
    @Suppress("UNCHECKED_CAST")
    return createDexLoader(context, apkPath).loadClass(fragmentName) as Class<Fragment>?
  } catch (e: Throwable) {
    e.printStackTrace()
  }
  return null
}

fun createDexLoader(context: Context, apkPath: String): DexClassLoader {
  return DexClassLoader(apkPath, context.getDir("template_cache", 0).absolutePath, null, context.classLoader)
}