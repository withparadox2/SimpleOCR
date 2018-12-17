package com.withparadox2.simpleocr.support.template

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.withparadox2.simpleocr.util.APK_PATH
import java.io.File

class Template(private val apkPath: String, private val clazz: Class<Fragment>) {
    var name: String = File(apkPath).run {
        name.substring(8, name.indexOf("."))
    }

    val key: String
        get() = apkPath

    fun newFragment(args: Bundle? = null): Fragment? {
        try {
            val fragment = clazz.newInstance()
            fragment.arguments = (args ?: Bundle()).apply {
                this.putString(APK_PATH, apkPath)
            }
            return fragment
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}