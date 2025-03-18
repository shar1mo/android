package com.example.myandroidapp

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.media.MediaPlayer
import android.os.Handler
import android.widget.SeekBar
import android.widget.TextView

class mp3player : AppCompatActivity() {
    private lateinit var runnable: Runnable
    private var handler = Handler()
    private lateinit var seekbar: SeekBar
    private lateinit var songname: TextView
    private lateinit var play: Button
    private lateinit var prev: Button
    private lateinit var next: Button
    private lateinit var playlist: Array<MediaPlayer>
    private lateinit var songTitles : Array<String>
    private var indexSound = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mp3player)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        songname = findViewById(R.id.songname)
        next = findViewById(R.id.next_button)
        play = findViewById(R.id.play_button)
        prev = findViewById(R.id.prev_button)
        playlist = arrayOf(
            MediaPlayer.create(this@mp3player,R.raw.bmb),
            MediaPlayer.create(this@mp3player,R.raw.boulev)
        )
        songTitles = arrayOf(
            "bmb",
            "boulevard"
        )
        seekbar = findViewById(R.id.seekbar)
        seekbar.progress = 0
    }

    override fun onResume() {
        super.onResume()
        play.setOnClickListener{
            if(!playlist[indexSound].isPlaying){
            playlist[indexSound].start()
                songname.text = "${songTitles[indexSound]}"
            }else{
                playlist[indexSound].pause()
            }
        }
        next.setOnClickListener{
            playlist[indexSound].stop()
            playlist[indexSound].prepare()

            indexSound = (indexSound + 1) % playlist.size
            playlist[indexSound].start()
            songname.text = "${songTitles[indexSound]}"
        }
        prev.setOnClickListener{
            playlist[indexSound].stop()
            playlist[indexSound].prepare()

            if(indexSound <= 0){
                indexSound = playlist.size - 1
            }else{
                indexSound = (indexSound - 1) % playlist.size
            }
            playlist[indexSound].start()
            songname.text = "${songTitles[indexSound]}"
        }
        seekbar.max = playlist[indexSound].duration
        seekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, pos: Int, changed: Boolean) {
                if(changed){
                    playlist[indexSound].seekTo(pos)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
        runnable = Runnable {
            seekbar.progress = playlist[indexSound].currentPosition
            handler.postDelayed(runnable, 0)
        }
        handler.postDelayed(runnable, 0)
        playlist[indexSound].setOnCompletionListener {
            seekbar.progress = 0
        }
    }
}