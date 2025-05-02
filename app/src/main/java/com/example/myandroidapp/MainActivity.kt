package com.example.myandroidapp

import android.content.ContentProviderClient
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var calculator: Button
    private lateinit var mp3: Button
    private lateinit var gps: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        calculator = findViewById(R.id.openCalculator)
        calculator.setOnClickListener {
            val intentcalculator = Intent(this, Calculator::class.java)
            startActivity(intentcalculator)
        }
        mp3 = findViewById(R.id.openMP3)
        mp3.setOnClickListener{
            val  intentmp3 = Intent(this, mp3player::class.java)
            startActivity(intentmp3)
        }
        gps = findViewById(R.id.openGPS)
        gps.setOnClickListener{
            val intentgps = Intent(this, GpsActivity::class.java)
            startActivity(intentgps)
        }
    }
}