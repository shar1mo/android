package com.example.myandroidapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class TcpClient : AppCompatActivity() {

    private lateinit var tvResponse: TextView
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvHistory: TextView
    private lateinit var scrollHistory: ScrollView
    private lateinit var btnConnect: Button
    private lateinit var btnSendLocation: Button
    private lateinit var btnAutoSend: Button
    private lateinit var etServerIp: EditText

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val mainHandler = Handler(Looper.getMainLooper())
    private val netExecutor = Executors.newSingleThreadExecutor()

    private var socket: ZMQ.Socket? = null
    private var context: ZContext? = null
    @Volatile private var isConnected = false
    @Volatile private var isAutoSending = false

    companion object {
        private const val TAG = "TcpClient"
        private const val LOCATION_PERMISSION_CODE = 100
        private const val AUTO_SEND_INTERVAL_MS: Long = 15_000L
    }

    private val timeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tcpclient)

        initViews()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        etServerIp.setText("172.20.10.2")
    }

    private fun initViews() {
        tvResponse = findViewById(R.id.tvResponse)
        tvLatitude = findViewById(R.id.gpsLatitude)
        tvLongitude = findViewById(R.id.gpsLongitude)
        tvTime = findViewById(R.id.gpsTime)
        tvHistory = findViewById(R.id.gpsHistory)
        scrollHistory = findViewById(R.id.scrollHistory)
        btnConnect = findViewById(R.id.btnConnect)
        btnSendLocation = findViewById(R.id.btnGetLocation)
        btnAutoSend = findViewById(R.id.btnStartAuto)
        etServerIp = findViewById(R.id.etServerIp)

        btnConnect.setOnClickListener {
            if (isConnected) disconnectFromServer()
            else connectToServer()
        }

        btnSendLocation.setOnClickListener {
            if (isConnected) getCurrentLocation()
            else showToast("Begin connect to server first")
        }

        btnAutoSend.setOnClickListener {
            if (!isConnected) {
                showToast("Begin connect to server first")
                return@setOnClickListener
            }
            toggleAutoSend()
        }
    }

    private fun connectToServer() {
        netExecutor.execute {
            try {
                socket?.close()
                context?.close()

                context = ZContext()
                socket = context?.createSocket(SocketType.REQ)
                socket?.setLinger(0)
                socket?.setReceiveTimeOut(3000)
                socket?.setSendTimeOut(3000)

                val serverIp = etServerIp.text.toString().trim()
                val connectionString = "tcp://$serverIp:8080"
                Log.d(TAG, "Connecting to: $connectionString")

                socket?.connect(connectionString)
                socket?.send("test")
                val reply = socket?.recvStr()
                if (reply.isNullOrEmpty()) throw Exception("No reply from server")

                isConnected = true
                mainHandler.post {
                    btnConnect.text = "Disconnect"
                    tvResponse.text = "Connected: $reply"
                    showToast("Connected to server")
                }

            } catch (e: Exception) {
                isConnected = false
                mainHandler.post {
                    btnConnect.text = "Connect to Server"
                    tvResponse.text = "Err connect: ${e.message}"
                    showToast("Err connect: ${e.message}")
                }
                Log.e(TAG, "Connection failed", e)
            }
        }
    }

    private fun disconnectFromServer() {
        netExecutor.execute {
            try {
                socket?.send("disconnect")
                socket?.recvStr()
            } catch (_: Exception) {}
            try {
                socket?.close()
                context?.close()
            } catch (_: Exception) {}
            isConnected = false
            mainHandler.post {
                btnConnect.text = "Connect to Server"
                tvResponse.text = "Disconnected"
                showToast("Disconnected from server")
            }
        }
    }

    private fun getCurrentLocation() {
        if (!checkPermissions()) {
            requestPermissions()
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation.addOnCompleteListener { task ->
            val location: Location? = task.result
            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude

                mainHandler.post {
                    tvLatitude.text = "latitude: %.6f".format(lat)
                    tvLongitude.text = "longitude: %.6f".format(lon)
                    tvTime.text = "time: ${timeFormat.format(Date())}"
                }

                sendLocationToServer(lat, lon)
            } else {
                mainHandler.post {
                    tvResponse.text = "Location is null; try enabling GPS."
                }
            }
        }
    }

    private fun sendLocationToServer(latitude: Double, longitude: Double) {
        netExecutor.execute {
            try {
                if (!isConnected || socket == null) throw Exception("Not connected")

                val locationData = JSONObject().apply {
                    put("latitude", latitude)
                    put("longitude", longitude)
                    put("timestamp", System.currentTimeMillis())
                    put("device_time", timeFormat.format(Date()))
                    put("device_id", "Android_${android.os.Build.MODEL}")
                }

                socket?.send(locationData.toString())
                val reply = socket?.recvStr() ?: "no reply"

                mainHandler.post {
                    tvResponse.text = "Data send: $reply"
                    val entry = "${timeFormat.format(Date())} — lat: %.6f, lon: %.6f\n".format(latitude, longitude)
                    tvHistory.append(entry)
                    scrollHistory.post { scrollHistory.fullScroll(ScrollView.FOCUS_DOWN) }
                }
                Log.d(TAG, "Location sent successfully: $reply")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to send location", e)
                mainHandler.post { tvResponse.text = "Err send: ${e.message}" }
                isConnected = false
            }
        }
    }

    private val autoSendRunnable = object : Runnable {
        override fun run() {
            if (!isAutoSending || !isConnected) return
            getCurrentLocation()
            mainHandler.postDelayed(this, AUTO_SEND_INTERVAL_MS)
        }
    }

    private fun toggleAutoSend() {
        isAutoSending = !isAutoSending
        if (isAutoSending) {
            btnAutoSend.text = "Autosend stop"
            tvResponse.text = "Autosend start (every 15s)"
            showToast("Autosend start")
            mainHandler.post(autoSendRunnable)
        } else {
            btnAutoSend.text = "Autosend start"
            tvResponse.text = "Autosend stopped"
            showToast("Autosend stop")
            mainHandler.removeCallbacks(autoSendRunnable)
        }
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_CODE
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        isAutoSending = false
        mainHandler.removeCallbacksAndMessages(null)
        try { netExecutor.shutdownNow() } catch (_: Exception) {}
        try {
            socket?.close()
            context?.close()
        } catch (_: Exception) {}
    }
}
