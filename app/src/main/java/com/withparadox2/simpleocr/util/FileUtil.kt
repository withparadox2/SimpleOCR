package com.withparadox2.simpleocr.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.FileOutputStream
import java.io.OutputStream
import android.provider.MediaStore

suspend fun writeToFile(data: ByteArray, path: String): Boolean {
    return GlobalScope.async(Dispatchers.IO) {
        var output: OutputStream? = null
        try {
            output = BufferedOutputStream(FileOutputStream(path))
            output.write(data)
            output.flush()
        } catch (e: Exception) {
            e.printStackTrace()
            return@async false
        } finally {
            closeQuietly(output)
        }
        true
    }.await()
}

fun closeQuietly(close: Closeable?) {
    try {
        close?.close()
    } catch (e: Exception) {
    }
}