package dev.vibe.pixelspot.lock

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dev.vibe.pixelspot.MainActivity
import dev.vibe.pixelspot.R
import dev.vibe.pixelspot.data.Prefs

/**
 * Opens [LockPlayerActivity] over the lock screen while music plays (the same "full-screen
 * notification" trick alarm apps use). That trick wakes the display, so it normally runs when
 * the phone is woken, replacing the lock screen. Only "Always on" also uses it at screen off.
 */
class LockScreenService : Service() {

    companion object {
        private const val CHANNEL_SERVICE = "lock_service"
        private const val CHANNEL_SHOW = "lock_show"
        private const val ID_SERVICE = 1
        const val ID_SHOW = 2

        fun sync(context: Context) {
            val intent = Intent(context, LockScreenService::class.java)
            if (Prefs(context).settings.value.lockScreenPlayer) {
                runCatching { ContextCompat.startForegroundService(context, intent) }
            } else {
                context.stopService(intent)
            }
        }
    }

    private val screenEvents = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val music = getSystemService(AudioManager::class.java).isMusicActive
            // Already open: leave it. Reopening it here is what would wake a screen you just turned off.
            val canOpen = music && !LockPlayerActivity.isOpen
            when (intent.action) {
                Intent.ACTION_SCREEN_ON ->
                    if (canOpen && getSystemService(KeyguardManager::class.java).isKeyguardLocked) showOverLockScreen()
                Intent.ACTION_SCREEN_OFF ->
                    if (canOpen && Prefs(context).settings.value.lockScreenKeepOn) showOverLockScreen()
                Intent.ACTION_USER_PRESENT ->
                    getSystemService(NotificationManager::class.java).cancel(ID_SHOW)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SERVICE, "Lock screen player (background)", NotificationManager.IMPORTANCE_MIN)
                .apply { description = "Keeps the lock screen player ready. You can hide this notification." }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SHOW, "Lock screen player", NotificationManager.IMPORTANCE_HIGH)
                .apply { setSound(null, null); enableVibration(false) }
        )
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Lock screen player is on")
            .setContentText("Shows what's playing on your lock screen")
            .setContentIntent(open)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        ServiceCompat.startForeground(this, ID_SERVICE, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenEvents, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun showOverLockScreen() {
        val intent = Intent(this, LockPlayerActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        val pending = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_SHOW)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Now playing")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setFullScreenIntent(pending, true)
            .setTimeoutAfter(10_000)
            .build()
        getSystemService(NotificationManager::class.java).notify(ID_SHOW, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenEvents) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
