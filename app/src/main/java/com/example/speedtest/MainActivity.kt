package com.example.speedtest

import android.os.Bundle
import android.os.StrictMode
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {

    private lateinit var speedTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speedTextView = TextView(this)
        speedTextView.textSize = 18f
        setContentView(speedTextView)

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().permitAll().build()
        )

        testDownloadSpeed()
    }

    private fun testDownloadSpeed() {
        CoroutineScope(Dispatchers.IO).launch {
            val url = URL("https://speed.hetzner.de/10MB.bin")
            val connection = url.openConnection()
            val input = connection.getInputStream()
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
        }
    }
}