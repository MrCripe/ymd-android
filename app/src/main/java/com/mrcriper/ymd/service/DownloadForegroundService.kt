package com.mrcriper.ymd.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import com.mrcriper.ymd.R
import com.mrcriper.ymd.presentation.MainActivity

/**
 * Foreground service that keeps the download manager alive while tasks are running.
 * MediaStyle notification with progress actions.
 */
class DownloadForegroundService : LifecycleService() {

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
        startForeground(NOTIFICATION_ID, buildNotification("Preparing…", 0, false))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> Unit
        }
        return START_STICKY
    }

    fun updateProgress(title: String, progress: Int, indeterminate: Boolean) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(title, progress, indeterminate))
    }

    private fun buildNotification(title: String, progress: Int, indeterminate: Boolean): Notification {
        val pending = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stopPi = PendingIntent.getService(
            this, 1,
            Intent(this, DownloadForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentIntent(pending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, indeterminate)
            .addAction(0, "Stop", stopPi)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "ymd.download"
        const val NOTIFICATION_ID = 42
        const val ACTION_STOP = "com.mrcriper.ymd.action.STOP"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(NotificationManager::class.java)
                if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                    nm.createNotificationChannel(
                        NotificationChannel(
                            CHANNEL_ID,
                            "Downloads",
                            NotificationManager.IMPORTANCE_LOW,
                        ),
                    )
                }
            }
        }
    }
}
