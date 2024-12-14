package com.empire_mammoth.audiocraft

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.IOException


class MediaPlaybackService : Service() {
    val TAG = "MediaPlaybackService"

    var notificationManager: NotificationManager? = null

    private var mediaSession: MediaSessionCompat? = null

    private lateinit var songs: IntArray
    private var mediaPlayer: MediaPlayer? = null
    private var current_index: Int = 0
    private var isPlaying = false

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        mediaPlaybackService = this

        mediaSession = MediaSessionCompat(this, "MediaSessionTag")

        mediaSession!!.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        mediaSession!!.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                this@MediaPlaybackService.onPlay()
            }

            override fun onPause() {
                this@MediaPlaybackService.onPause()
            }

            override fun onSkipToNext() {
                this@MediaPlaybackService.onSkipToNext()
            }

            override fun onSkipToPrevious() {
                this@MediaPlaybackService.onSkipToPrevious()
            }

            override fun onStop() {
                this@MediaPlaybackService.onStop()
            }
        })

        val appContext = applicationContext

        val activityIntent = Intent(appContext, MainActivity::class.java)
        mediaSession!!.setSessionActivity(
            PendingIntent.getActivity(appContext, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE)
        );

        notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val mChannelName = getString(R.string.app_name)

        val notificationChannel = NotificationChannel(
            /* id = */ "CHANNEL_ID",
            /* name = */ mChannelName,
            /* importance = */ NotificationManager.IMPORTANCE_LOW
        )
        notificationManager!!.createNotificationChannel(notificationChannel)

        initMediaPlayer()
        updateNotification()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action != null) {
            when (intent.action) {
                PlaybackStateCompat.ACTION_PLAY.toString() -> onPlay()
                PlaybackStateCompat.ACTION_PAUSE.toString() -> onPause()
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT.toString() -> onSkipToNext()
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS.toString() -> onSkipToPrevious()
                PlaybackStateCompat.ACTION_STOP.toString() -> onStop()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun onPlay() {
        if (mediaPlayer == null) {
            initMediaPlayer()
        }
        if (!isPlaying) {
            mediaPlayer?.start()
            isPlaying = true
        }
        updateNotification()
    }

    private fun onPause() {
        if (isPlaying && mediaPlayer != null) {
            mediaPlayer!!.pause()
            isPlaying = false
        }
        updateNotification()
    }

    private fun onSkipToNext() {
        play()
        updateNotification()
    }

    private fun onSkipToPrevious() {
        play(-1)
        updateNotification()
    }


    private fun onStop() {
        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = null
            isPlaying = false
        }
    }


    private fun initMediaPlayer() {
        songs = intArrayOf(
            R.raw.radio_tapok_ataka_mertvecov_,
            R.raw.radio_tapok_gvardiya_petra,
            R.raw.radio_tapok_nochnye_vedmy,
            R.raw.radio_tapok_petropavlovsk,
            R.raw.radio_tapok_vysota_776
        )

        mediaPlayer = MediaPlayer.create(this, songs[0])
        mediaPlayer?.setOnCompletionListener {
            play()
        }
    }

    private fun play(dif: Int = 1) {
        current_index = (current_index + dif) % 5
        if (current_index == -1) current_index = 4
        val afd = this.resources.openRawResourceFd(songs[current_index])

        try {
            mediaPlayer!!.reset()
            mediaPlayer!!.setDataSource(afd.fileDescriptor, afd.startOffset, afd.declaredLength)
            mediaPlayer!!.prepare()
            mediaPlayer!!.start()
            afd.close()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Unable to play audio queue do to exception: " + e.message, e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Unable to play audio queue do to exception: " + e.message, e)
        } catch (e: IOException) {
            Log.e(TAG, "Unable to play audio queue do to exception: " + e.message, e)
        }
    }

    private fun isPlaying() = isPlaying

    override fun onDestroy() {
        super.onDestroy()
        onStop()
        mediaSession?.release()
    }

    fun updateNotification() {
        val notificationIntent = Intent(
            this,
            MainActivity::class.java
        )
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val playAction: NotificationCompat.Action = NotificationCompat.Action(
            if (isPlaying()) R.drawable.baseline_pause_circle_24 else R.drawable.baseline_play_circle_24,
            if (isPlaying()) "Pause" else "Play",
            buildMediaPendingIntent(if (isPlaying()) PlaybackStateCompat.ACTION_PAUSE else PlaybackStateCompat.ACTION_PLAY)
        )
        val nextAction: NotificationCompat.Action = NotificationCompat.Action(
            R.drawable.baseline_skip_next_24,
            "Next",
            buildMediaPendingIntent(PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
        )
        val previousAction: NotificationCompat.Action = NotificationCompat.Action(
            R.drawable.baseline_skip_previous_24,
            "Previous",
            buildMediaPendingIntent(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        )
        val stopAction: NotificationCompat.Action = NotificationCompat.Action(
            R.drawable.baseline_stop_24,
            "Stop",
            buildMediaPendingIntent(PlaybackStateCompat.ACTION_STOP)
        )

        val notification: Notification = NotificationCompat.Builder(this, "CHANNEL_ID")
            .setContentTitle(
                "Radio Tapok"
            )
            .setContentText(
                "Soung"
            )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(previousAction)
            .addAction(playAction)
            .addAction(nextAction)
            .addAction(stopAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession?.getSessionToken())
            )
            .build()

        if (!isPlaying() and (mediaPlayer == null)) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun buildMediaPendingIntent(action: Long): PendingIntent {
        val intent = Intent(this, MediaPlaybackService::class.java)
        intent.setAction(action.toString())
        return PendingIntent.getService(
            this,
            action.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        private var mediaPlaybackService: MediaPlaybackService? = null
        fun isPlaying() = mediaPlaybackService?.isPlaying ?: false
    }
}