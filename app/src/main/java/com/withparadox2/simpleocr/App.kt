package com.withparadox2.simpleocr

import android.app.Application

/**
 * Created by withparadox2 on 2018/3/21.
 */

class App : Application() {
    init {
        instance = this
    }

    companion object {
        lateinit var instance : Application
    }
}