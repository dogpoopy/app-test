package com.example.speedtest

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.InputStream
import java.net.URL
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {
    private lateinit var speedTextView: TextView
    private lateinit var pingTextView: TextView
    private lateinit var downloadButton: Button
    private lateinit var pingButton: Button
    private lateinit var unitSpinner: Spinner
    private lateinit var showPingLogsButton: Button

    private var downloadJob: Job? = null
    private var pingJob: Job? = null
    private var isDownloading = false
    private var isPinging = false
    private var pingLogText = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        speedTextView = findViewById(R.id.speedTextView)
        pingTextView = findViewById(R.id.pingTextView)
        downloadButton = findViewById(R.id.startDownloadButton)
        pingButton = findViewById(R.id.startPingButton)
        unitSpinner = findViewById(R.id.unitSpinner)
        showPingLogsButton = findViewById(R.id.showPingLogsButton)

        downloadButton.setOnClickListener {
            if (isDownloading) stopDownload() else startDownload()
        }

        pingButton.setOnClickListener {
            if (isPinging) stopPing() else startPing()
        }

        showPingLogsButton.setOnClickListener {
            showPingLogsDialog()
        }
    }

    private fun startDownload() {
        isDownloading = true
        downloadButton.text = "Stop Download"
        speedTextView.text = "Starting download..."

        downloadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://github.com/dogpoopy/app-test/releases/download/v1.0/speedtestfile")
                val connection = url.openConnection()
                val input: InputStream = connection.getInputStream()
                val buffer = ByteArray(8192)
                var totalBytes = 0L
                var lastBytes = 0L
                var startTime = System.currentTimeMillis()

                while (isActive) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead == -1) break
                    totalBytes += bytesRead

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - startTime >= 1000) {
                        val bytesThisSecond = totalBytes - lastBytes
                        lastBytes = totalBytes
                        val selectedUnit = unitSpinner.selectedItem.toString()
                        val speed = if (selectedUnit == "MB/s") {
                            bytesThisSecond / 1_000_000.0
                        } else {
                            (bytesThisSecond * 8) / 1_000_000.0
                        }

                        val formattedSpeed = DecimalFormat("#.##").format(speed)
                        withContext(Dispatchers.Main) {
                            speedTextView.text = "Download speed: $formattedSpeed $selectedUnit"
                        }

                        startTime = currentTime
                    }
                }

                input.close()
                withContext(Dispatchers.Main) {
                    speedTextView.append("\nDownload stopped.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    speedTextView.text = "Download error: ${e.localizedMessage}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    downloadButton.text = "Start Download"
                    isDownloading = false
                }
            }
        }
    }

    private fun stopDownload() {
        downloadJob?.cancel()
        downloadButton.text = "Start Download"
        isDownloading = false
        speedTextView.text = "Download stopped."
    }

    private fun startPing() {
        isPinging = true
        pingButton.text = "Stop Ping"
        pingLogText.clear()

        pingJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val process = ProcessBuilder("ping", "google.com")
                    .redirectErrorStream(true)
                    .start()

                val reader = process.inputStream.bufferedReader()

                while (isActive) {
                    val line = reader.readLine() ?: break
                    val match = Regex("time=([0-9.]+) ms").find(line)
                    match?.groupValues?.get(1)?.let { pingTime ->
                        pingLogText.appendLine("Ping: ${pingTime}ms")
                        withContext(Dispatchers.Main) {
                            pingTextView.text = "Ping: ${pingTime}ms"
                        }
                    }
                }

                process.destroy()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pingTextView.text = "Ping error: ${e.localizedMessage}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    pingButton.text = "Start Ping"
                    isPinging = false
                }
            }
        }
    }

    private fun stopPing() {
        pingJob?.cancel()
        pingTextView.text = "Ping: -"
        pingButton.text = "Start Ping"
        isPinging = false
    }

    private fun showPingLogsDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Ping Logs")
            .setMessage(pingLogText.toString().ifBlank { "No logs yet." })
            .setPositiveButton("Close", null)
            .create()
        dialog.show()
    }
}
