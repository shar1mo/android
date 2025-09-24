package com.example.myandroidapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ

class TcpClient : AppCompatActivity() {
    private val logTag = "MY_LOG_TAG"
    private lateinit var tvResponse: TextView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var handler: Handler

    private var context: ZContext? = null
    private var socket: ZMQ.Socket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tcpclient)

        tvResponse = findViewById(R.id.tvResponse)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        handler = Handler(Looper.getMainLooper())

        Thread {
            try {
                context = ZContext()
                socket = context!!.createSocket(SocketType.REQ)

                socket!!.connect("tcp://172.20.10.2:8080")

                handler.post {
                    tvResponse.text = "Connected to server"
                }
            } catch (e: Exception) {
                Log.e(logTag, "Error creating socket: ${e.message}", e)
                handler.post {
                    tvResponse.text = "Error: ${e.message}"
                }
            }
        }.start()

        btnSend.setOnClickListener {
            val msg = etMessage.text.toString()
            if (msg.isNotBlank()) {
                Thread {
                    try {
                        socket?.send(msg)
                        Log.d(logTag, "[CLIENT] Sent: $msg")

                        val reply = socket?.recvStr()
                        Log.d(logTag, "[CLIENT] Received: $reply")

                        handler.post {
                            tvResponse.text = "Server: $reply"
                        }
                    } catch (e: Exception) {
                        Log.e(logTag, "Error: ${e.message}", e)
                        handler.post {
                            tvResponse.text = "Error: ${e.message}"
                        }
                    }
                }.start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socket?.close()
        context?.close()
    }
}