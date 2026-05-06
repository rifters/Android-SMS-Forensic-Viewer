package com.example.timelineexporter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main activity — requests permissions, drives extraction and export,
 * and provides a button to open the generated output folder.
 */
class MainActivity : AppCompatActivity() {

    // ── Views ──────────────────────────────────────────────────────────────
    private lateinit var btnGenerate: Button
    private lateinit var btnExport: Button
    private lateinit var btnOpen: Button
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar

    // ── State ──────────────────────────────────────────────────────────────
    private var cachedMessages: List<Message>? = null
    private var cachedThreads:  Map<Long, Thread>? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ── Permission launcher ────────────────────────────────────────────────
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) {
                onPermissionsGranted()
            } else {
                showStatus("⚠️ Permissions denied. Cannot read messages.")
            }
        }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnGenerate = findViewById(R.id.btnGenerate)
        btnExport   = findViewById(R.id.btnExport)
        btnOpen     = findViewById(R.id.btnOpen)
        statusText  = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)

        btnExport.isEnabled = false
        btnOpen.isEnabled   = false

        btnGenerate.setOnClickListener { requestPermissionsAndGenerate() }
        btnExport.setOnClickListener   { exportTimeline() }
        btnOpen.setOnClickListener     { openExportFolder() }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    // ── Permission handling ────────────────────────────────────────────────

    private fun requiredPermissions(): Array<String> = buildList {
        add(Manifest.permission.READ_SMS)
        add(Manifest.permission.READ_CONTACTS)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    private fun hasPermissions(): Boolean =
        requiredPermissions().all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }

    private fun requestPermissionsAndGenerate() {
        if (hasPermissions()) {
            onPermissionsGranted()
        } else {
            permissionLauncher.launch(requiredPermissions())
        }
    }

    private fun onPermissionsGranted() {
        generateTimeline()
    }

    // ── Generation ────────────────────────────────────────────────────────

    private fun generateTimeline() {
        showProgress(true)
        showStatus("📖 Reading messages…")
        btnGenerate.isEnabled = false

        scope.launch {
            try {
                val (messages, threads) = withContext(Dispatchers.IO) {
                    val contactMap = Utils.buildContactMap(contentResolver)
                    val lookup: (String) -> String? = { phone ->
                        Utils.lookupName(phone, contactMap)
                    }

                    val smsMessages = SmsReader(contentResolver).readAll(lookup)
                    val mmsMessages = MmsReader(contentResolver).readAll(lookup)

                    val allMessages = (smsMessages + mmsMessages)
                        .sortedBy { it.timestamp }
                    val threads = Utils.groupIntoThreads(allMessages, contactMap)

                    Pair(allMessages, threads)
                }

                cachedMessages = messages
                cachedThreads  = threads

                showStatus(
                    "✅ Found ${messages.size} messages in ${threads.size} threads.\n" +
                    "Tap 'Export HTML' to write the files."
                )
                btnExport.isEnabled = true

            } catch (e: Exception) {
                showStatus("❌ Error reading messages: ${e.message}")
            } finally {
                showProgress(false)
                btnGenerate.isEnabled = true
            }
        }
    }

    // ── Export ────────────────────────────────────────────────────────────

    private fun exportTimeline() {
        val messages = cachedMessages ?: return
        val threads  = cachedThreads  ?: return

        showProgress(true)
        showStatus("⚙️ Generating HTML…")
        btnExport.isEnabled = false

        scope.launch {
            try {
                val exportDir = withContext(Dispatchers.IO) {
                    val generator = HtmlGenerator()
                    val exporter  = FileExporter(this@MainActivity)

                    val timelineHtml = generator.buildTimeline(messages, threads)
                    val threadPages  = threads.mapValues { (_, thread) ->
                        generator.buildThreadPage(thread)
                    }

                    exporter.exportAll(timelineHtml, threadPages)
                }

                showStatus("✅ Export complete!\n${exportDir.absolutePath}")
                btnOpen.isEnabled = true

            } catch (e: Exception) {
                showStatus("❌ Export failed: ${e.message}")
            } finally {
                showProgress(false)
                btnExport.isEnabled = true
            }
        }
    }

    // ── Open folder ───────────────────────────────────────────────────────

    private fun openExportFolder() {
        try {
            val exporter  = FileExporter(this)
            val exportDir = exporter.getExportDir()
            val uri: Uri  = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                exportDir
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "resource/folder")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Fall back: open timeline.html directly in a browser
                openTimelineHtml()
            }
        } catch (e: Exception) {
            openTimelineHtml()
        }
    }

    private fun openTimelineHtml() {
        try {
            val exporter     = FileExporter(this)
            val timelineFile = java.io.File(exporter.getExportDir(), "timeline.html")
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                timelineFile
            )
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────

    private fun showStatus(msg: String) {
        statusText.text = msg
    }

    private fun showProgress(visible: Boolean) {
        progressBar.visibility = if (visible) View.VISIBLE else View.GONE
    }
}
