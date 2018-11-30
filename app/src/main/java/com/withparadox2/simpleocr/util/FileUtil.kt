package com.withparadox2.simpleocr.util

import java.io.*

fun writeToFile(data: ByteArray, path: String): Boolean {
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

fun closeQuietly(close: Closeable?) {
    try {
        close?.close()
    } catch (e: Exception) {
    }
}

fun writeToFile(input: InputStream, path: String) {
    var output: OutputStream? = null
    try {
        File(path).parentFile.mkdirs()
        output = FileOutputStream(File(path))

        val buffer = ByteArray(1024)
        var len: Int
        while (true) {
            len = input.read(buffer)
            if (len <= 0) {
                break
            }
            output.write(buffer, 0, len)
        }
        output.flush()
    } catch (e: Throwable) {
        e.printStackTrace()
    } finally {
        closeQuietly(input)
        closeQuietly(output)
    }
}