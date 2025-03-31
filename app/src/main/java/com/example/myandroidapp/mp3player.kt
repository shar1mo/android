package com.example.myandroidapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File

class mp3player : AppCompatActivity() {
    private lateinit var runnable: Runnable
    private var handler = Handler()
    private lateinit var seekbar: SeekBar
    private lateinit var songname: TextView
    private lateinit var play: Button
    private lateinit var prev: Button
    private lateinit var next: Button
    private lateinit var cyclebtn: Button
    private lateinit var playlist: Array<MediaPlayer>
    private lateinit var songTitles: Array<String>
    private var indexSound = 0
    private var cycleval = false
    private lateinit var volumeseek: SeekBar

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission denied - using default songs", Toast.LENGTH_LONG).show()
            setupDefaultSongs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mp3player)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        volumeseek = findViewById(R.id.volumeseek)
        songname = findViewById(R.id.songname)
        next = findViewById(R.id.next_button)
        play = findViewById(R.id.play_button)
        prev = findViewById(R.id.prev_button)
        cyclebtn = findViewById(R.id.cyclebtn)
        seekbar = findViewById(R.id.seekbar)
        seekbar.progress = 0

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            loadDeviceSongs()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            setupDefaultSongs()
        }
    }

    private fun setupDefaultSongs() {
        playlist = arrayOf(
            MediaPlayer.create(this@mp3player, R.raw.bmb),
            MediaPlayer.create(this@mp3player, R.raw.boulev)
        )
        songTitles = arrayOf("bmb", "boulevard")
    }

    private fun loadDeviceSongs() {
        try {
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val files = musicDir.listFiles { file -> file.extension.equals("mp3", ignoreCase = true) }

            if (files != null && files.isNotEmpty()) {
                playlist = Array(files.size) { MediaPlayer() }
                songTitles = Array(files.size) { "" }

                for (i in files.indices) {
                    playlist[i] = MediaPlayer().apply {
                        setDataSource(this@mp3player, Uri.fromFile(files[i]))
                        prepare()
                    }
                    songTitles[i] = files[i].nameWithoutExtension
                }
            } else {
                setupDefaultSongs()
                Toast.makeText(this, "No MP3 files found, using default songs", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            setupDefaultSongs()
            Toast.makeText(this, "Error loading songs, using default", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStart() {
        super.onStart()

        fun endsong() {
            playlist[indexSound].setOnCompletionListener {
                if (cycleval) {
                    playlist[indexSound].seekTo(0)
                    playlist[indexSound].start()
                } else {
                    playlist[indexSound].stop()
                    playlist[indexSound].prepare()

                    indexSound = (indexSound + 1) % playlist.size
                    seekbar.max = playlist[indexSound].duration
                    playlist[indexSound].start()
                    songname.text = songTitles[indexSound]
                }
            }
        }

        fun play() {
            songname.text = songTitles[indexSound]
            seekbar.max = playlist[indexSound].duration
            if (!playlist[indexSound].isPlaying) {
                playlist[indexSound].start()
            } else {
                playlist[indexSound].pause()
            }
        }

        fun next() {
            playlist[indexSound].stop()
            playlist[indexSound].prepare()

            indexSound = (indexSound + 1) % playlist.size
            seekbar.max = playlist[indexSound].duration
            playlist[indexSound].start()
            songname.text = songTitles[indexSound]
        }

        fun prev() {
            playlist[indexSound].stop()
            playlist[indexSound].prepare()

            if (indexSound <= 0) {
                indexSound = playlist.size - 1
            } else {
                indexSound = (indexSound - 1) % playlist.size
            }
            seekbar.max = playlist[indexSound].duration
            playlist[indexSound].start()
            songname.text = songTitles[indexSound]
        }

        fun cyclefun() {
            cycleval = !cycleval
            cyclebtn.text = if (cycleval) "cycle on" else "cycle off"
        }

        play.setOnClickListener {
            play()
            endsong()
        }

        next.setOnClickListener {
            next()
            endsong()
        }

        prev.setOnClickListener {
            prev()
            endsong()
        }

        cyclebtn.setOnClickListener {
            cyclefun()
        }

        seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, pos: Int, changed: Boolean) {
                if (changed) {
                    playlist[indexSound].seekTo(pos)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        runnable = Runnable {
            seekbar.progress = playlist[indexSound].currentPosition
            handler.postDelayed(runnable, 100)
        }
        handler.postDelayed(runnable, 100)

        playlist[indexSound].setOnCompletionListener {
            seekbar.progress = 0
        }

        volumeseek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, changed: Boolean) {
                val volumeseekbar = progress / 100f
                playlist[indexSound].setVolume(volumeseekbar, volumeseekbar)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        playlist.forEach { it.release() }
        handler.removeCallbacks(runnable)
    }
}