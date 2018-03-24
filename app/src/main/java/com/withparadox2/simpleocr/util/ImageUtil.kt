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

const val MAX_SIZE = 1800

fun compress(srcPath: String?) {
    if (!isFileExists(srcPath)) return

    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    decodeFile(srcPath, options)

    if (options.outWidth * options.outHeight < MAX_SIZE * MAX_SIZE) {
        return
    }

    options.inJustDecodeBounds = false
    options.inSampleSize = Math.max(options.outHeight, options.outWidth) / MAX_SIZE

    var bitmap = decodeFile(srcPath, options)
    val scale : Float = Math.max(options.outHeight, options.outWidth) / MAX_SIZE.toFloat()

    val rotation = getRotationOfPhoto(srcPath)

    bitmap = scaleAndRotate(bitmap, scale > 1, 1 / scale, rotation != 0, rotation)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 60, FileOutputStream(srcPath))
}

fun scaleAndRotate(bitmap: Bitmap, needScale: Boolean = false, scale: Float = 0f, needRotate: Boolean = false, angle: Int = 0, recycleSrc: Boolean = true): Bitmap {
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