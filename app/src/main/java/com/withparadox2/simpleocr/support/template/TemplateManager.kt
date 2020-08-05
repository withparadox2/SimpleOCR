package com.withparadox2.simpleocr.support.template

import android.content.Context
import androidx.core.content.edit
import com.withparadox2.simpleocr.App
import com.withparadox2.simpleocr.BuildConfig
import com.withparadox2.simpleocr.util.*
import kotlinx.coroutines.GlobalScope
import java.io.File

var isCopied = false

// We store app-version-code with this key, only if local folder where
// bundles are kept is empty or the app has been updated to a new version
// or in debug mode, we will copy bundles from assets to local folder
private const val KEY_CHECK_BUNDLE_CODE = "key_bundle_code"
private val keyToTemplate = HashMap<String, Template>()
private val templateList = ArrayList<Template>()
private var hasRetried = false

suspend fun preloadTemplates() {
  GlobalScope.asyncIO {
    updateTemplateBundle()
    val bundleArray = File(getTemplateBasePath()).listFiles()?.filter { it.name.endsWith(".apk") }
        ?: ArrayList<File>(0)
    bundleArray.forEach {
      val clazz = loadClassFromApk(App.instance, it.absolutePath)
      if (clazz != null) {
        val template = Template(it.absolutePath, clazz)
        keyToTemplate[template.key] = template
        templateList.add(template)
      }
    }
    getDefaultTemplate()?.apply {
      templateList.remove(this)
      templateList.add(0, this)
    }
    if (!hasRetried && !bundleArray.isEmpty() && templateList.size == 0) {
      hasRetried = true
      File(getTemplateBasePath()).listFiles()?.filter {
        it.isDirectory
      }?.forEach {
        it.deleteRecursively()
      }
      // Delete pre opt file and retry to load template
      preloadTemplates()
    }
  }.await()
}

fun getTemplateList(): List<Template> {
  return templateList
}

fun getDefaultTemplate(): Template? {
  return getTargetTemplate(getTemplateBasePath() + "templatedefault.apk")
}

fun getTargetTemplate(key: String?): Template? {
  return if (key == null) null else keyToTemplate[key]
}

private fun copyApkIfNot(context: Context) {
  if (isCopied) {
    return
  }
  isCopied = true
  val array = context.assets.list("")?.filter { it.endsWith(".apk") }
  array?.forEach {
    val path = getTemplateBasePath() + it
    writeToFile(context.assets.open(it), path)
  }
}

private fun updateTemplateBundle() {
  val nowCode = getVersionCode()
  val oldCode = getSp().getInt(KEY_CHECK_BUNDLE_CODE, -1)
  if (BuildConfig.DEBUG || File(getTemplateBasePath()).list().isEmpty() || nowCode != oldCode) {
    copyApkIfNot(App.instance)
    getSp().edit {
      this.putInt(KEY_CHECK_BUNDLE_CODE, nowCode)
    }
  }
}