package com.empire_mammoth.audiocraft

import android.R
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.empire_mammoth.audiocraft.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private var mediaController: MediaControllerCompat? = null
    private var mediaBrowser: MediaBrowserCompat? = null

    private var binding: ActivityMainBinding? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        startService(Intent(this, MediaPlaybackService::class.java))

        //  Создание MediaBrowser
        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(
                this,
                MediaPlaybackService::class.java
            ),  //  передаем Class сервиса для взаимодействия с ним
            connectionCallbacks, null
        )
        mediaBrowser!!.connect()

        binding?.apply {
            playButton?.setOnClickListener {
                if (mediaController != null) {
                    mediaController!!.transportControls.play()
                }
            }
            pauseButton?.setOnClickListener {
                if (mediaController != null) {
                    mediaController!!.transportControls.pause()
                }
            }
            nextButton?.setOnClickListener {
                if (mediaController != null) {
                    mediaController!!.transportControls.skipToNext()
                }
            }
            previousButton?.setOnClickListener {
                if (mediaController != null) {
                    mediaController!!.transportControls.skipToPrevious()
                }
            }
        }
    }

    // Callbacks для MediaBrowser
    private val connectionCallbacks: MediaBrowserCompat.ConnectionCallback =
        object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                super.onConnected()
                try {
                    // Получение MediaControllerCompat
                    mediaController =
                        MediaControllerCompat(this@MainActivity, mediaBrowser!!.sessionToken)
                    //  Установка обратного вызова для отслеживания изменения состояния медиасессии
                    mediaController!!.registerCallback(mediaControllerCallback)
                    setMediaController(mediaController)
                    Log.d("MainActivity", "Connected to MediaService")
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }

            override fun onConnectionFailed() {
                Log.d("MainActivity", "ConnectionFailed to MediaService")
                super.onConnectionFailed()
            }

            override fun onConnectionSuspended() {
                Log.d("MainActivity", "ConnectionSuspended to MediaService")
                super.onConnectionSuspended()
                mediaController = null
                setMediaController(mediaController)
            }
        }

    //  Callback для MediaControllerCompat
    private val mediaControllerCallback: MediaControllerCompat.Callback =
        object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
                super.onPlaybackStateChanged(state)
                if (state != null) {
                    Log.d("MainActivity", "state = " + state.state)
                }
                //обновить ui
            }

            override fun onMetadataChanged(metadata: MediaMetadataCompat) {
                super.onMetadataChanged(metadata)
                //обновить ui
                if (metadata != null) {
                    Log.d(
                        "MainActivity",
                        "METADATA = " + metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                    )
                }
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaBrowser != null) {
            mediaBrowser!!.disconnect() // отключаемся от сервиса
        }
        if (mediaController != null) {
            mediaController!!.unregisterCallback(mediaControllerCallback)
        }
    }

    private fun setMediaController(controller: MediaControllerCompat?) {
        mediaController = controller
    }
}