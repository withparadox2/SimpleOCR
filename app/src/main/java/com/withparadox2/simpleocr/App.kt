package com.withparadox2.simpleocr

import android.app.Application
import android.os.Handler

/**
 * Created by withparadox2 on 2018/3/21.
 */

class App : Application() {
    init {
        instance = this
    }

    companion object {
        lateinit var instance: Application
        private val handler: Handler = Handler()
        fun post(action: Runnable) = handler.post(action)
        fun postDelayed(action: Runnable, millis : Long) = handler.postDelayed(action, millis)
    }
}