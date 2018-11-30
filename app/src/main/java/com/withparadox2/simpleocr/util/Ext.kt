package com.withparadox2.simpleocr.util

import android.app.Activity
import android.graphics.RectF
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream

/**
 * Created by withparadox2 on 2018/11/7.
 */
fun CoroutineScope.launchUI(blockParam: suspend CoroutineScope.() -> Unit) = GlobalScope.launch(Dispatchers.Main, block = blockParam)

fun <T> CoroutineScope.asyncIO(blockParam: suspend CoroutineScope.() -> T): Deferred<T> = this.async(Dispatchers.IO) {
    blockParam()
}

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

fun <T : View> Activity.inflate(id: Int, parent: ViewGroup? = null): T {
    @Suppress("UNCHECKED_CAST")
    return LayoutInflater.from(this).inflate(id, parent, false) as T
}

fun RectF.halfWidth(): Float {
    return this.width() / 2f
}

fun RectF.halfHeight(): Float {
    return this.height() / 2f
}