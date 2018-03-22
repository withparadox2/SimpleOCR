package com.withparadox2.simpleocr.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.decodeFile
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

/**
 * Created by withparadox2 on 2018/3/22.
 */
fun compress(srcPath: String?) {
    if (!isFileExists(srcPath)) return

    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    decodeFile(srcPath, options)

    val maxSize = 2000
    if (options.outWidth * options.outHeight < maxSize * maxSize) {
        return
    }

    options.inJustDecodeBounds = false
    options.inSampleSize = Math.max(options.outHeight, options.outWidth) / 2000

    var bitmap = decodeFile(srcPath, options)
    val scale = Math.max(options.outHeight, options.outWidth) / 2000f

    bitmap = scaleAndRotate(bitmap, scale > 1, 1 / scale, false, 0, true)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, FileOutputStream(srcPath))
}

fun scaleAndRotate(bitmap: Bitmap, needScale: Boolean, scale: Float, needRotate: Boolean, angle: Int, recycleSrc: Boolean): Bitmap {
    val matrix = Matrix()
    if (needScale) {
        matrix.setScale(scale, scale)
    }
    if (needRotate) {
        matrix.setRotate(angle.toFloat())
    }
    val temp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (recycleSrc) {
        bitmap.recycle()
    }
    return temp
}

fun getRotationOfPhoto(filePath: String?): Int {
    if (!isFileExists(filePath)) return 0
    val type = ExifInterface(filePath)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
    return when (type) {
        ExifInterface.ORIENTATION_NORMAL -> 0
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        else -> 270
    }
}

fun isFileExists(filePath: String?): Boolean = filePath != null && File(filePath).exists()