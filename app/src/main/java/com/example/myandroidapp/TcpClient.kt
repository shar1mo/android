package com.example.myandroidapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.TelephonyManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class TcpClient : AppCompatActivity() {

    private lateinit var tvResponse: TextView
    private lateinit var tvHistory: TextView
    private lateinit var etServerIp: EditText
    private lateinit var btnConnect: Button
    private lateinit var btnAutoSend: Button

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var telephonyManager: TelephonyManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val netExecutor = Executors.newSingleThreadExecutor()

    @Volatile private var isConnected = false
    @Volatile private var isAutoSending = false

    private var socket: ZMQ.Socket? = null
    private var context: ZContext? = null

    private val unsentQueue = mutableListOf<JSONObject>()

    companion object {
        private const val TAG = "TcpClient"
        private const val LOCATION_PERMISSION_CODE = 100
        private const val AUTO_SEND_INTERVAL_MS: Long = 1000
    }

    private val timeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tcpclient)

        tvResponse = findViewById(R.id.tvResponse)
        tvHistory = findViewById(R.id.tvHistory)
        etServerIp = findViewById(R.id.etServerIp)
        btnConnect = findViewById(R.id.btnConnect)
        btnAutoSend = findViewById(R.id.btnAutoSend)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        etServerIp.setText("172.20.10.2")

        btnConnect.setOnClickListener { toggleConnection() }
        btnAutoSend.setOnClickListener { toggleAutoSend() }
    }

    private fun toggleConnection() {
        if (isConnected) disconnectFromServer()
        else connectToServer()
    }

    private fun connectToServer() {
        netExecutor.execute {
            try {
                try { socket?.close() } catch (_: Throwable) {}
                try { context?.close() } catch (_: Throwable) {}
                context = ZContext()
                socket = context?.createSocket(SocketType.REQ)
                socket?.setLinger(0)
                socket?.setReceiveTimeOut(3000)
                socket?.setSendTimeOut(3000)

                val ip = etServerIp.text.toString().trim()
                socket?.connect("tcp://$ip:8080")
                socket?.send("test")
                val reply = socket?.recvStr() ?: throw Exception("No reply")

                isConnected = true
                mainHandler.post {
                    btnConnect.text = "Disconnect"
                    showToast("Connected: $reply")
                    tvResponse.text = "Connected: $reply"
                }

                sendQueue()
            } catch (t: Throwable) {
                isConnected = false
                mainHandler.post {
                    btnConnect.text = "Connect"
                    showToast("Err: ${t.message}")
                    tvResponse.text = "Err: ${t.message}"
                }
            }
        }
    }

    private fun disconnectFromServer() {
        netExecutor.execute {
            try { socket?.send("exit"); socket?.recvStr() } catch (_: Throwable) {}
            try { socket?.close() } catch (_: Throwable) {}
            try { context?.close() } catch (_: Throwable) {}
            isConnected = false
            mainHandler.post {
                btnConnect.text = "Connect"
                showToast("Disconnected")
                tvResponse.text = "Disconnected"
            }
        }
    }

    private val autoSendRunnable = object : Runnable {
        override fun run() {
            if (!isAutoSending) return

            if (checkPermissions()) {
                collectAndSend()
            } else requestPermissions()

            mainHandler.postDelayed(this, AUTO_SEND_INTERVAL_MS)
        }
    }

    private fun toggleAutoSend() {
        isAutoSending = !isAutoSending
        if (isAutoSending) {
            btnAutoSend.text = "Stop Auto"
            mainHandler.post(autoSendRunnable)
            showToast("Autosend started")
        } else {
            btnAutoSend.text = "Start Auto"
            mainHandler.removeCallbacks(autoSendRunnable)
            showToast("Autosend stopped")
        }
    }

    private fun collectAndSend() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            val payload = JSONObject()
            payload.put("device_id", "Android_${android.os.Build.MODEL}")
            payload.put("timestamp", System.currentTimeMillis())

            val locObj = JSONObject().apply {
                put("latitude", location?.latitude ?: JSONObject.NULL)
                put("longitude", location?.longitude ?: JSONObject.NULL)
                put("altitude", location?.altitude ?: JSONObject.NULL)
                put("speed", location?.speed?.toDouble() ?: JSONObject.NULL)
            }
            payload.put("location", locObj)

            val lteArray = JSONArray()
            val allCells = try { telephonyManager.allCellInfo ?: emptyList() } catch (_: Throwable) { emptyList<CellInfo>() }
            allCells.filterIsInstance<CellInfoLte>().forEach { ci ->
                try {
                    val id = ci.cellIdentity
                    val ss = ci.cellSignalStrength
                    val obj = JSONObject()
                    val identity = JSONObject().apply {
                        put("pci", id?.pci ?: JSONObject.NULL)
                        put("mcc", id?.mccString ?: JSONObject.NULL)
                        put("mnc", id?.mncString ?: JSONObject.NULL)
                    }
                    val signal = JSONObject().apply {
                        put("rsrp", ss?.rsrp ?: JSONObject.NULL)
                        put("rsrq", ss?.rsrq ?: JSONObject.NULL)
                        put("rssnr", ss?.rssnr ?: JSONObject.NULL)
                        put("rssi", ss?.dbm ?: JSONObject.NULL)
                    }
                    obj.put("type", "LTE")
                    obj.put("identity", identity)
                    obj.put("signal", signal)
                    lteArray.put(obj)
                } catch (_: Throwable) {}
            }
            payload.put("cell_info", lteArray)

            unsentQueue.add(payload)
            if (isConnected) sendQueue()
        }
    }

    private fun sendQueue() {
        netExecutor.execute {
            val iterator = unsentQueue.iterator()
            while (iterator.hasNext()) {
                val payload = iterator.next()
                try {
                    socket?.send(payload.toString())
                    val reply = socket?.recvStr() ?: "no reply"
                    mainHandler.post {
                        tvHistory.append("${timeFormat.format(Date())} — Sent\n")
                        tvResponse.text = "Sent: $reply"
                    }
                    iterator.remove()
                } catch (_: Throwable) {
                    break
                }
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val phone = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        return fine && coarse && phone
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE),
            LOCATION_PERMISSION_CODE
        )
    }

    private fun showToast(msg: String) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }

    override fun onDestroy() {
        super.onDestroy()
        isAutoSending = false
        mainHandler.removeCallbacksAndMessages(null)
        try { socket?.close(); context?.close() } catch (_: Throwable) {}
    }
}