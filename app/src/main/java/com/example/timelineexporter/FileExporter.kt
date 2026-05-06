package com.example.timelineexporter

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.IOException

/**
 * Writes all generated HTML files and static assets to the device's external storage.
 *
 * Output directory: <External Documents>/TimelineExport/
 */
class FileExporter(private val context: Context) {

    companion object {
        const val EXPORT_DIR_NAME = "TimelineExport"

        /** Relative paths of bundled asset files that must be copied alongside the HTML. */
        private val ASSET_FILES = listOf(
            "assets/style.css",
            "assets/timeline.css",
            "assets/thread.css",
            "assets/script.js",
            "assets/thread.js"
        )
    }

    /**
     * Returns (and creates if necessary) the export root directory.
     * Uses [Context.getExternalFilesDir] so no WRITE_EXTERNAL_STORAGE permission is needed
     * on API 29+ devices.
     */
    fun getExportDir(): File {
        val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: throw IOException("External storage is not available")
        return File(docsDir, EXPORT_DIR_NAME).also { it.mkdirs() }
    }

    /**
     * Write a single HTML file to the export directory.
     *
     * @param fileName  File name, e.g. "timeline.html" or "thread_12.html".
     * @param content   Complete HTML string.
     * @return The [File] that was written.
     */
    fun writeHtml(fileName: String, content: String): File {
        val exportDir = getExportDir()
        val file = File(exportDir, fileName)
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    /**
     * Copy all bundled CSS/JS assets from the app's assets folder to the export directory.
     * Creates subdirectories as needed.
     */
    fun copyAssets() {
        val exportDir = getExportDir()
        for (relativePath in ASSET_FILES) {
            val destFile = File(exportDir, relativePath)
            destFile.parentFile?.mkdirs()
            try {
                context.assets.open(relativePath).use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: IOException) {
                // Asset may not exist in all build variants; skip silently
            }
        }
    }

    /**
     * Delete all previously exported files in the export directory.
     */
    fun clearExportDir() {
        getExportDir().listFiles()?.forEach { it.deleteRecursively() }
    }

    /**
     * Full export: clear old files, write timeline + all thread pages, copy assets.
     *
     * @param timelineHtml  Content for timeline.html.
     * @param threadPages   Map of threadId → HTML content.
     * @return The export root [File].
     */
    fun exportAll(
        timelineHtml: String,
        threadPages: Map<Long, String>
    ): File {
        clearExportDir()
        writeHtml("timeline.html", timelineHtml)
        for ((threadId, html) in threadPages) {
            writeHtml("thread_$threadId.html", html)
        }
        copyAssets()
        return getExportDir()
    }
}
