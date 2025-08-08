package com.truedlspeed

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.truedlspeed.ui.SpeedometerView
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.*
import java.io.InputStream
import java.net.URL
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var speedTextView: TextView
    private lateinit var pingTextView: TextView
    private lateinit var downloadButton: Button
    private lateinit var pingButton: Button
    private lateinit var unitSpinner: Spinner
    private lateinit var showPingLogsButton: Button
    private lateinit var speedometerView: SpeedometerView

    private var downloadJob: Job? = null
    private var pingJob: Job? = null
    private var animateJob: Job? = null
    private var isDownloading = false
    private var isPinging = false
    private var pingLogText = StringBuilder()

    private var displayedSpeed = 0f

    companion object {
        private const val BYTES_PER_MB = 1_000_000.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivitiesIfAvailable(application)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        speedTextView = findViewById(R.id.speedTextView)
        pingTextView = findViewById(R.id.pingTextView)
        downloadButton = findViewById(R.id.startDownloadButton)
        pingButton = findViewById(R.id.startPingButton)
        unitSpinner = findViewById(R.id.unitSpinner)
        showPingLogsButton = findViewById(R.id.showPingLogsButton)
        speedometerView = findViewById(R.id.speedometerView)

        speedTextView.text = getString(R.string.default_download_speed)
        pingTextView.text = getString(R.string.default_ping)

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
        downloadButton.text = getString(R.string.stop_download)
        speedTextView.text = getString(R.string.starting_download)

        downloadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://github.com/dogpoopy/speed-test-app/releases/download/v1.0/speedtestfile")
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
                        val rawSpeed = if (selectedUnit == "MB/s") {
                            bytesThisSecond / BYTES_PER_MB
                        } else {
                            (bytesThisSecond * 8) / BYTES_PER_MB
                        }

                        val displaySpeed = String.format("%.2f", rawSpeed)
                        val speedMbps = if (selectedUnit == "Mbps") rawSpeed else rawSpeed * 8

                        withContext(Dispatchers.Main) {
                            speedTextView.text = getString(R.string.download_speed, displaySpeed, selectedUnit)
                            animateSpeedChange(speedMbps.toFloat())
                        }

                        startTime = currentTime
                    }
                }

                input.close()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    speedTextView.text = "Download error: ${e.localizedMessage}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    downloadButton.text = getString(R.string.start_download)
                    isDownloading = false
                    animateSpeedChange(0f)
                    if (speedTextView.text.startsWith(getString(R.string.starting_download))) {
                        speedTextView.text = getString(R.string.default_download_speed)
                    }
                }
            }
        }
    }

    private fun animateSpeedChange(targetSpeed: Float) {
        animateJob?.cancel()
        animateJob = CoroutineScope(Dispatchers.Main).launch {
            while (abs(displayedSpeed - targetSpeed) > 0.1f) {
                displayedSpeed += (targetSpeed - displayedSpeed) * 0.2f
                speedometerView.setSpeed(displayedSpeed)
                delay(16)
            }
            displayedSpeed = targetSpeed
            speedometerView.setSpeed(displayedSpeed)
        }
    }

    private fun stopDownload() {
        downloadJob?.cancel()
        downloadButton.text = getString(R.string.start_download)
        isDownloading = false
        speedTextView.text = getString(R.string.default_download_speed)
        animateSpeedChange(0f)
    }

    private fun startPing() {
        isPinging = true
        pingButton.text = getString(R.string.stop_ping)
        pingLogText.clear()

        pingJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val process = ProcessBuilder("ping", "8.8.8.8")
                    .redirectErrorStream(true)
                    .start()

                val reader = process.inputStream.bufferedReader()

                while (isActive) {
                    val line = reader.readLine() ?: break
                    val match = Regex("time=([0-9.]+) ms").find(line)
                    match?.groupValues?.get(1)?.let { pingTime ->
                        pingLogText.appendLine("Ping: ${pingTime}ms")
                        withContext(Dispatchers.Main) {
                            pingTextView.text = getString(R.string.ping_value, pingTime)
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
                    pingButton.text = getString(R.string.start_ping)
                    isPinging = false
                    if (!pingTextView.text.contains("ms")) {
                        pingTextView.text = getString(R.string.default_ping)
                    }
                }
            }
        }
    }

    private fun stopPing() {
        pingJob?.cancel()
        pingTextView.text = getString(R.string.default_ping)
        pingButton.text = getString(R.string.start_ping)
        isPinging = false
    }

    private fun showPingLogsDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.ping_logs_title)
            .setMessage(pingLogText.toString().ifBlank { getString(R.string.no_logs) })
            .setPositiveButton("Close", null)
            .show()
    }
}
