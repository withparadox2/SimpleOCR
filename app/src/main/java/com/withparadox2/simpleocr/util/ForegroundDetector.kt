package com.withparadox2.simpleocr.util


import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle

import java.util.concurrent.CopyOnWriteArrayList

@SuppressLint("NewApi")
class ForegroundDetector(application: Application) : Application.ActivityLifecycleCallbacks {

  private var refs: Int = 0
  private var wasInBackground = true
  private var enterBackgroundTime: Long = 0
  private val listeners = CopyOnWriteArrayList<Listener>()

  val isForeground: Boolean
    get() = refs > 0

  val isBackground: Boolean
    get() = refs == 0

  interface Listener {
    fun onBecameForeground()
    fun onBecameBackground()
  }

  init {
    instance = this
    application.registerActivityLifecycleCallbacks(this)
  }

  fun addListener(listener: Listener) {
    listeners.add(listener)
  }

  fun removeListener(listener: Listener) {
    listeners.remove(listener)
  }

  override fun onActivityStarted(activity: Activity) {
    if (++refs == 1) {
      if (System.currentTimeMillis() - enterBackgroundTime < 200) {
        wasInBackground = false
      }
      for (listener in listeners) {
        try {
          listener.onBecameForeground()
        } catch (e: Exception) {
        }

      }
    }
  }

  fun isWasInBackground(reset: Boolean): Boolean {
    if (reset && Build.VERSION.SDK_INT >= 21 && System.currentTimeMillis() - enterBackgroundTime < 200) {
      wasInBackground = false
    }
    return wasInBackground
  }

  fun resetBackgroundVar() {
    wasInBackground = false
  }

  override fun onActivityStopped(activity: Activity) {
    if (--refs == 0) {
      enterBackgroundTime = System.currentTimeMillis()
      wasInBackground = true
      for (listener in listeners) {
        try {
          listener.onBecameBackground()
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
    }
  }

  override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

  override fun onActivityResumed(activity: Activity) {}

  override fun onActivityPaused(activity: Activity) {}

  override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {}

  override fun onActivityDestroyed(activity: Activity) {}

  companion object {
    lateinit var instance: ForegroundDetector
  }
}
