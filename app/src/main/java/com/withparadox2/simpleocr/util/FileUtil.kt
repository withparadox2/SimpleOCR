package com.withparadox2.simpleocr.util

import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.FileOutputStream
import java.io.OutputStream

fun writeToFile(data : ByteArray, path : String) : Boolean {
    var output: OutputStream? = null
    try {
        output = BufferedOutputStream(FileOutputStream(path))
        output.write(data)
        output.flush()
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    } finally {
        closeQuietly(output)
    }
    return true
}

fun closeQuietly(close : Closeable?) {
    try {
        close?.close()
    } catch (e : Exception) {
    }
}