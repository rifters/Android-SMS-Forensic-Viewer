package com.example.timelineexporter

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Shared utility functions used across the project.
 */
object Utils {

    const val SELF_ADDRESS = "self"
    const val SELF_NAME    = "Me"

    private val DATE_FORMAT_DATE = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private val DATE_FORMAT_TIME = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val DATE_FORMAT_FULL = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

    /** Format a Unix timestamp (ms) as a human-readable date string, e.g. "May 5, 2024". */
    fun formatDate(timestampMs: Long): String =
        DATE_FORMAT_DATE.format(Date(timestampMs))

    /** Format a Unix timestamp (ms) as a human-readable time string, e.g. "2:14 PM". */
    fun formatTime(timestampMs: Long): String =
        DATE_FORMAT_TIME.format(Date(timestampMs))

    /** Format a Unix timestamp (ms) as a full date+time string, e.g. "May 5, 2024 2:14 PM". */
    fun formatFull(timestampMs: Long): String =
        DATE_FORMAT_FULL.format(Date(timestampMs))

    /**
     * Escape characters that have special meaning in HTML.
     * Prevents XSS-style issues when inserting message bodies into HTML.
     */
    fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
        .replace("\n", "<br>")

    /**
     * Return true if two timestamps are within [windowMs] of each other.
     * Used to group nearby messages under the same time separator.
     */
    fun withinWindow(a: Long, b: Long, windowMs: Long = TimeUnit.SECONDS.toMillis(30)): Boolean =
        Math.abs(a - b) <= windowMs

    /**
     * Normalise a phone number to a canonical form for de-duplication.
     * Strips non-digit characters and leading country-code prefixes where safe.
     */
    fun normalisePhone(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        // Strip leading "1" from 11-digit US numbers
        return if (digits.length == 11 && digits.startsWith("1")) digits.substring(1) else digits
    }

    /**
     * Build a ContentResolver-based contact lookup map: phone → display name.
     * Callers should cache the result; do not call on the main thread.
     */
    fun buildContactMap(contentResolver: ContentResolver): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI, ""
        )

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ),
            null, null, null
        )

        cursor?.use {
            val numIdx  = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val nameIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            while (it.moveToNext()) {
                val number = it.getString(numIdx) ?: continue
                val name   = it.getString(nameIdx) ?: continue
                map[normalisePhone(number)] = name
            }
        }

        return map
    }

    /**
     * Look up a display name for [phone] using a pre-built [contactMap].
     */
    fun lookupName(phone: String, contactMap: Map<String, String>): String? =
        contactMap[normalisePhone(phone)]

    /**
     * Group a flat list of messages into [Thread] objects keyed by threadId.
     * Also populates thread-level metadata (participant list, display name).
     */
    fun groupIntoThreads(
        messages: List<Message>,
        contactMap: Map<String, String>
    ): Map<Long, Thread> {
        val threads = mutableMapOf<Long, Thread>()

        for (msg in messages) {
            val thread = threads.getOrPut(msg.threadId) {
                val allAddresses = buildList {
                    if (msg.groupParticipants != null) addAll(msg.groupParticipants)
                    else add(msg.sender.takeIf { it != SELF_ADDRESS } ?: "")
                }.filter { it.isNotEmpty() }.distinct()

                val names = allAddresses.map { addr ->
                    lookupName(addr, contactMap) ?: addr
                }

                val displayName = when {
                    names.isEmpty() -> "Unknown"
                    names.size == 1 -> names[0]
                    else            -> names.joinToString(", ")
                }

                Thread(
                    threadId         = msg.threadId,
                    participants     = allAddresses,
                    participantNames = names,
                    displayName      = displayName
                )
            }
            thread.messages.add(msg)
        }

        // Sort messages within each thread by timestamp
        threads.values.forEach { it.messages.sortBy { m -> m.timestamp } }

        return threads
    }
}
