package com.example.smartlearningeducationalgame2

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder

class MusicService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var currentResId: Int = -1

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val EXTRA_SONG_RES_ID = "EXTRA_SONG_RES_ID"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val songResId = intent?.getIntExtra(EXTRA_SONG_RES_ID, R.raw.main_backgroundsong) ?: R.raw.main_backgroundsong

        val sharedPref = getSharedPreferences("game_settings", Context.MODE_PRIVATE)
        val isMusicEnabled = sharedPref.getBoolean("music_enabled", true)

        if (!isMusicEnabled && (action == ACTION_START || action == ACTION_RESUME)) {
            // If music is disabled, we should still update currentResId if it's ACTION_START
            // so that if they turn it on later, the correct song plays.
            if (action == ACTION_START) currentResId = songResId
            stopAndRelease()
            return START_STICKY
        }

        when (action) {
            ACTION_START -> {
                playSong(songResId)
            }
            ACTION_RESUME -> {
                if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                    mediaPlayer?.start()
                } else if (mediaPlayer == null) {
                    playSong(songResId)
                }
            }
            ACTION_PAUSE -> {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
            }
        }
        return START_STICKY
    }

    private fun playSong(resId: Int) {
        if (currentResId == resId && mediaPlayer?.isPlaying == true) {
            return // Already playing this song
        }

        stopAndRelease()

        mediaPlayer = MediaPlayer.create(this, resId)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(0.75f, 0.75f)
        mediaPlayer?.start()
        currentResId = resId
    }

    private fun stopAndRelease() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAndRelease()
    }
}
