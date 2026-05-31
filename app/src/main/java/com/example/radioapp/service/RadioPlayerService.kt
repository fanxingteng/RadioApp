package com.example.radioapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.radioapp.MainActivity
import com.example.radioapp.R

class RadioPlayerService : Service() {
    
    companion object {
        const val CHANNEL_ID = "RadioPlayerChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY = "PLAY"
        const val ACTION_PAUSE = "PAUSE"
        const val ACTION_STOP = "STOP"
        const val EXTRA_STATION_URL = "station_url"
        const val EXTRA_STATION_NAME = "station_name"
        
        var player: ExoPlayer? = null
        var currentStationName: String = ""
        var isPlaying: Boolean = false
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        player = ExoPlayer.Builder(this).build()
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        isPlaying = true
                        updateNotification()
                    }
                    Player.STATE_IDLE -> {
                        isPlaying = false
                        updateNotification()
                    }
                    Player.STATE_ENDED -> {
                        isPlaying = false
                        updateNotification()
                    }
                }
            }
            
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                super.onPlayerError(error)
                isPlaying = false
                updateNotification()
            }
        })
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val url = intent.getStringExtra(EXTRA_STATION_URL)
                val name = intent.getStringExtra(EXTRA_STATION_NAME)
                if (url != null && name != null) {
                    playStation(url, name)
                }
            }
            ACTION_PAUSE -> pauseStation()
            ACTION_STOP -> stopStation()
        }
        return START_STICKY
    }
    
    private fun playStation(url: String, name: String) {
        currentStationName = name
        player?.stop()
        player?.clearMediaItems()
        val mediaItem = MediaItem.fromUri(url)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
        
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    private fun pauseStation() {
        player?.pause()
        isPlaying = false
        updateNotification()
    }
    
    private fun stopStation() {
        player?.stop()
        player?.clearMediaItems()
        isPlaying = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Radio Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Radio playback controls"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action.Builder(
                R.drawable.ic_pause,
                "Pause",
                getServicePendingIntent(ACTION_PAUSE)
            ).build()
        } else {
            NotificationCompat.Action.Builder(
                R.drawable.ic_play,
                "Play",
                getServicePendingIntent(ACTION_PLAY)
            ).build()
        }
        
        val stopAction = NotificationCompat.Action.Builder(
            R.drawable.ic_stop,
            "Stop",
            getServicePendingIntent(ACTION_STOP)
        ).build()
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentStationName.ifEmpty { "Radio Player" })
            .setContentText(if (isPlaying) "Playing" else "Paused")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .addAction(playPauseAction)
            .addAction(stopAction)
            .setOngoing(true)
            .build()
    }
    
    private fun getServicePendingIntent(action: String): PendingIntent {
        val intent = Intent(this, RadioPlayerService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
