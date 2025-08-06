package com.example.speedtest

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {

    private lateinit var speedTextView: TextView
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        speedTextView = findViewById(R.id.speedTextView)
        startButton = findViewById(R.id.startButton)

        startButton.setOnClickListener {
            testDownloadSpeed()
        }
    }

    private fun testDownloadSpeed() {
        speedTextView.text = "Testing..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://github.com/dogpoopy/app-test/releases/download/v1.0/speedtestfile")
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.instanceFollowRedirects = true
                connection.connect()

                val input = connection.inputStream
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalBytes = 0L

                val timeTakenMillis = measureTimeMillis {
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        totalBytes += bytesRead
                    }
                    input.close()
                }

                val seconds = timeTakenMillis / 1000.0
                val speedMbps = (totalBytes * 8) / (seconds * 1_000_000)

                runOnUiThread {
                    speedTextView.text = "Download speed: %.2f Mbps".format(speedMbps)
                }

            } catch (e: Exception) {
                Log.e("SpeedTest", "Error during speed test", e)
                runOnUiThread {
                    speedTextView.text = "Error: ${e.message ?: "Unknown error"}"
                }
            }
        }
    }
}
