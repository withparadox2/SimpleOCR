package com.withparadox2.simpleocr.util

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