package com.example.timelineexporter.plugins.line

import android.content.Context
import com.example.timelineexporter.Message
import com.example.timelineexporter.plugins.MessageSourcePlugin
import java.io.File

/**
 * Plugin that loads LINE messages.
 *
 * Access strategy (tried in order):
 *   1. Shizuku — if the Shizuku service is bound and permission is granted.
 *   2. Root — if the device is rooted and su is accessible.
 *   3. User-imported backup — if the user has placed a LINE backup file in the
 *      app's files directory.
 *   4. Unavailable — none of the above applies; [isAvailable] returns false.
 */
class LinePlugin : MessageSourcePlugin {

    // Cached availability — set during isAvailable() to avoid re-evaluating in loadMessages()
    private var shizukuAvailable = false
    private var rootAvailable    = false

    override fun isAvailable(context: Context): Boolean {
        shizukuAvailable = ShizukuHelper.isAvailable()
        rootAvailable    = RootHelper.isAvailable()
        return shizukuAvailable || rootAvailable || importFileExists(context)
    }

    override fun loadMessages(
        context: Context,
        contactLookup: ((String) -> String?)?
    ): List<Message> {
        val reader = LineReader()
        return when {
            shizukuAvailable          -> reader.readViaShizuku()
            rootAvailable             -> reader.readViaRoot()
            importFileExists(context) -> reader.readFromImport(importFile(context))
            else                      -> emptyList()
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun importFile(context: Context): File =
        File(context.filesDir, "line_backup.db")

    private fun importFileExists(context: Context): Boolean =
        importFile(context).exists()
}

// ── Thin wrappers so Shizuku / root checks are easy to stub in tests ────────

private object ShizukuHelper {
    /** Returns true when Shizuku is running and permission has been granted. */
    fun isAvailable(): Boolean {
        // TODO: replace with real Shizuku availability check
        //   e.g. Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PERMISSION_GRANTED
        return false
    }
}

private object RootHelper {
    /** Returns true when a root binary (su) is accessible on the device. */
    fun isAvailable(): Boolean {
        // TODO: replace with real root check, e.g. via libsu or RootBeer
        return false
    }
}
