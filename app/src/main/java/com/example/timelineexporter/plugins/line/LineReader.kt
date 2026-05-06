package com.example.timelineexporter.plugins.line

import com.example.timelineexporter.Message
import java.io.File

/**
 * Reader responsible for extracting LINE messages.
 *
 * All three methods are stubs.  Real SQLite / Shizuku / root parsing will be
 * added in a future step — for now each method simply returns an empty list so
 * that the plugin system compiles and runs end-to-end.
 */
class LineReader {

    /**
     * Reads LINE messages by querying the LINE database via the Shizuku API.
     *
     * TODO: implement — open LINE's SQLite DB through a Shizuku-bound
     *       ContentProvider or UserService and parse the message rows.
     */
    fun readViaShizuku(): List<Message> {
        // TODO: implement Shizuku-based LINE DB access
        return emptyList()
    }

    /**
     * Reads LINE messages by accessing the LINE database as root.
     *
     * TODO: implement — use `su` (e.g. via libsu) to copy / read LINE's DB
     *       from its private data directory, then parse the message rows.
     */
    fun readViaRoot(): List<Message> {
        // TODO: implement root-based LINE DB access
        return emptyList()
    }

    /**
     * Reads LINE messages from a user-provided backup file.
     *
     * @param file The backup DB file placed by the user in the app's files dir.
     *
     * TODO: implement — open [file] as a SQLite database and parse message rows.
     */
    fun readFromImport(file: File): List<Message> {
        // TODO: implement import-file parsing
        return emptyList()
    }
}
