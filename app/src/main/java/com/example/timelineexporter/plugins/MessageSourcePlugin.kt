package com.example.timelineexporter.plugins

import android.content.Context
import com.example.timelineexporter.Message

/**
 * Contract that every message-source plugin must implement.
 *
 * Plugins are responsible for reporting their own availability and for loading
 * their messages.  Actual DB / network access is delegated to dedicated Reader
 * classes so that this interface stays minimal.
 */
interface MessageSourcePlugin {

    /**
     * Returns true if this plugin can supply messages on the current device
     * (e.g. required permissions granted, app installed, backup file present).
     */
    fun isAvailable(context: Context): Boolean

    /**
     * Loads and returns all messages this plugin can provide.
     *
     * @param context       Android context used for content-resolver / file access.
     * @param contactLookup Optional lambda that resolves a raw address/phone to a
     *                      human-readable contact name.
     */
    fun loadMessages(
        context: Context,
        contactLookup: ((String) -> String?)? = null
    ): List<Message>
}
