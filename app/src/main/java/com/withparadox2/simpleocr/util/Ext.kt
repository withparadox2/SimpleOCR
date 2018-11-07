package com.withparadox2.simpleocr.util

import android.app.Activity
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

/**
 * Created by withparadox2 on 2018/11/7.
 */
fun CoroutineScope.launchUI(blockParam: suspend CoroutineScope.() -> Unit) = GlobalScope.launch(Dispatchers.Main, block = blockParam)

fun File.readBytes(): ByteArray {
    val ifs = FileInputStream(this)
    return ifs.readBytes()
}

fun <T : View> Activity.bind(id: Int): Lazy<T> {
    return unsafeLazy { findViewById<T>(id) }
}

fun <T : View> View.bind(id: Int): Lazy<T> {
    return unsafeLazy { findViewById<T>(id) }
}

fun <T> unsafeLazy(init: () -> T): Lazy<T> {
    return lazy(LazyThreadSafetyMode.NONE, init)
}