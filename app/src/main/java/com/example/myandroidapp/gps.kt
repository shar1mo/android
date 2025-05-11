package com.example.myandroidapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GpsActivity : AppCompatActivity() {

    private val LOG_TAG = "GPS_ACTIVITY"
    private lateinit var tvLongitude: TextView
    private lateinit var tvLatitude: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvHistory: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var handler: Handler
    private val updateInterval = 3 * 60 * 1000L
    private val gpsHistoryFile = "gps_history.json"

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gps)

        tvTime = findViewById(R.id.gpsTime)
        tvLongitude = findViewById(R.id.gpsLongitude)
        tvLatitude = findViewById(R.id.gpsLatitude)
        tvHistory = findViewById(R.id.gpsHistory)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        handler = Handler(Looper.getMainLooper())

        updateTime()
        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        getCurrentLocation()
        handler.postDelayed(locationUpdateRunnable, updateInterval)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(locationUpdateRunnable)
    }

    private val locationUpdateRunnable = object : Runnable {
        override fun run() {
            getCurrentLocation()
            handler.postDelayed(this, updateInterval)
        }
    }

    private fun updateTime() {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        val currentDate = dateFormat.format(Date())
        tvTime.text = "time: $currentTime\ndate: $currentDate"
    }

    private fun getCurrentLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                fetchLocation()
            } else {
                Log.w(LOG_TAG, "location services disabled")
                Toast.makeText(applicationContext, "please enable location services", Toast.LENGTH_SHORT).show()
                openLocationSettings()
            }
        } else {
            Log.w(LOG_TAG, "location permission not granted")
            requestPermissions()
        }
    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
            val location: Location? = task.result
            if (location == null) {
                Toast.makeText(applicationContext, "unable to get current location", Toast.LENGTH_SHORT).show()
            } else {
                updateTime()
                val latitude = location.latitude
                val longitude = location.longitude
                tvLatitude.text = "latitude: %.6f".format(latitude)
                tvLongitude.text = "longitude: %.6f".format(longitude)

                saveLocationToHistory(latitude, longitude)
                loadHistory()
            }
        }
    }

    private fun saveLocationToHistory(latitude: Double, longitude: Double) {
        try {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val currentTime = timeFormat.format(Date())
            val currentDate = dateFormat.format(Date())

            val locationData = JSONObject().apply {
                put("date", currentDate)
                put("time", currentTime)
                put("latitude", latitude)
                put("longitude", longitude)
            }

            val file = File(filesDir, gpsHistoryFile)
            val jsonArray = if (file.exists()) {
                JSONArray(file.readText())
            } else {
                JSONArray()
            }

            jsonArray.put(locationData)
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            Log.e(LOG_TAG, "error saving location history: ${e.localizedMessage}")
        }
    }

    private fun loadHistory() {
        try {
            val file = File(filesDir, gpsHistoryFile)
            if (file.exists()) {
                val jsonArray = JSONArray(file.readText())
                val historyString = StringBuilder("gps.json:\n\n")

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    historyString.append("date: ${item.getString("date")}\n")
                    historyString.append("time: ${item.getString("time")}\n")
                    historyString.append("latitude: ${item.getDouble("latitude")}\n")
                    historyString.append("longitude: ${item.getDouble("longitude")}\n\n")
                }

                tvHistory.text = historyString.toString()
            } else {
                tvHistory.text = "no location history yet"
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "error loading location history: ${e.localizedMessage}")
            tvHistory.text = "error loading history"
        }
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun openLocationSettings() {
        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(applicationContext, "location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}