package com.withparadox2.simpleocr

import android.app.*
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.withparadox2.simpleocr.ui.edit.EditActivity

class MonitorService : Service() {
    private var mLastText: CharSequence? = null
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private val mPrimaryChangeListener = ClipboardManager.OnPrimaryClipChangedListener {
        val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        if (clipData!!.itemCount > 0) {
            val text = clipData.getItemAt(0).text
            if (text == mLastText) {
                return@OnPrimaryClipChangedListener
            }

            for (i in text.indices) {
                if (text[i].toInt() !in 0..255) {
                    mLastText = clipData.getItemAt(0).text
                    startActivity(Intent(this, EditActivity::class.java).apply {
                        this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    break
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.addPrimaryClipChangedListener(mPrimaryChangeListener)

        val resultIntent = Intent(this, EditActivity::class.java)
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        resultIntent.action = Intent.ACTION_MAIN
        val resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val channelId =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel("my_service", "My Background Service")
                } else {
                    // If earlier version channel ID is not used
                    // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                    ""
                }

        val notification = NotificationCompat.Builder(this, channelId).setContentTitle("SimpleDict")
                .setLargeIcon((resources.getDrawable(R.mipmap.icon_launcher) as BitmapDrawable).bitmap)
                .setSmallIcon(R.mipmap.icon_launcher)
                .setContentIntent(resultPendingIntent)
                .build()
        startForeground(1, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }
}