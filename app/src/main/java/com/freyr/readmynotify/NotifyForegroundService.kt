package com.freyr.readmynotify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.freyr.readmynotify.common.Constants

class NotifyForegroundService : Service() {
    private var isNeedSpeak = true
//    private lateinit var contentView: RemoteViews

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        Log.d("NotifyForegroundService", "onStartCommand")
        Log.d("NotifyForegroundService", "getStringExtra：${intent?.getStringExtra(Constants.SERVICE_EXTRA)}")

        when (intent?.getStringExtra(Constants.SERVICE_EXTRA)) {
            Constants.EXTRA_SPEAKER_PLAY -> {
                Log.d("NotifyForegroundService", "extra：speaker")
                isNeedSpeak = true
            }
            Constants.EXTRA_SPEAKER_STOP -> {
                isNeedSpeak = false
            }
            Constants.EXTRA_SPEAKER_CLOSE -> {
                stopForeground(Constants.NOTIFY_FOREGROUND_SERVICE_ID)
            }
        }

        startForeground()

        return START_STICKY
    }

    private fun startForeground() {
        val channelId = createNotificationChannel("my_service", "My Background Service")
        val contentView = createNotificationView()

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification =
            notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//            .setContentTitle("ReadNotify")
//            .setContentText("啟動中")
                .setCustomContentView(contentView)
                .setCustomBigContentView(contentView)
                .setCategory(Notification.CATEGORY_SERVICE)
//            .setAutoCancel(true)
//            .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()
        startForeground(Constants.NOTIFY_FOREGROUND_SERVICE_ID, notification)
    }

    private fun createNotificationView(): RemoteViews {
        val views = RemoteViews(packageName, R.layout.layout_notify)

        views.setTextViewText(R.id.tv_title, getString(R.string.app_name))

        if (isNeedSpeak) {
            views.setTextViewText(R.id.btn_stop_or_play, "暫停")
        } else {
            views.setTextViewText(R.id.btn_stop_or_play, "繼續")
        }

        views.setOnClickPendingIntent(R.id.btn_stop_or_play, setSpeakerStatus())
        views.setOnClickPendingIntent(R.id.btn_close, setForegroundServiceClose())

        return views
    }

//    private fun updateNotificationView(): RemoteViews {
//        val views = contentView
//
// //        views.setTextViewText(R.id.tv_title, getString(R.string.app_name))
//
//        if (isNeedSpeak) {
//            views.setTextViewText(R.id.btn_stop_or_play, "暫停")
//        }else{
//            views.setTextViewText(R.id.btn_stop_or_play, "繼續")
//        }
//
//        views.setOnClickPendingIntent(R.id.btn_stop_or_play, setSpeakerStatus())
//        views.setOnClickPendingIntent(R.id.btn_close, setForegroundServiceClose())
//
//        return views
//    }

    private fun setSpeakerStatus(): PendingIntent {
        val intent = Intent(this, NotifyForegroundService::class.java)
        return if (isNeedSpeak) {
            intent.putExtra(Constants.SERVICE_EXTRA, Constants.EXTRA_SPEAKER_STOP)
            PendingIntent.getService(
                this,
                Constants.REQUEST_SPEAKER_STOP,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT,
            )
        } else {
            intent.putExtra(Constants.SERVICE_EXTRA, Constants.EXTRA_SPEAKER_PLAY)
            PendingIntent.getService(
                this,
                Constants.REQUEST_SPEAKER_PLAY,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
//
//        return PendingIntent.getService(
//            this,
//            Constants.REQUEST_SPEAKER_PLAY,
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT
//        )
    }

    private fun setForegroundServiceClose(): PendingIntent {
        val intent = Intent(this, NotifyForegroundService::class.java)
        intent.putExtra(Constants.SERVICE_EXTRA, Constants.EXTRA_SPEAKER_CLOSE)

        return PendingIntent.getService(
            this,
            Constants.REQUEST_SPEAKER_CLOSE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun createNotificationChannel(
        channelId: String,
        channelName: String,
    ): String {
        val chan =
            NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_NONE,
            )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }
}
