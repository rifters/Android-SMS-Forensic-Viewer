package com.example.timelineexporter.plugins

import android.content.Context
import com.example.timelineexporter.Message
import com.example.timelineexporter.plugins.email.EmailPlugin
import com.example.timelineexporter.plugins.line.LinePlugin
import com.example.timelineexporter.plugins.sms.SmsPlugin

/**
 * Central registry for all [MessageSourcePlugin] implementations.
 *
 * Call [loadAllMessages] to collect messages from every available plugin,
 * merge them into a single list, and sort them chronologically.
 */
object PluginManager {

    /** Ordered list of all registered plugins. */
    private val plugins: List<MessageSourcePlugin> = listOf(
        SmsPlugin(),
        LinePlugin(),
        EmailPlugin()
    )

    /**
     * Iterates every registered plugin, skips unavailable ones, collects their
     * messages, merges the results, and returns them sorted by timestamp.
     *
     * @param context       Android context forwarded to each plugin.
     * @param contactLookup Optional contact-name resolver forwarded to each plugin.
     */
    fun loadAllMessages(
        context: Context,
        contactLookup: ((String) -> String?)? = null
    ): List<Message> {
        return plugins
            .filter { it.isAvailable(context) }
            .flatMap { it.loadMessages(context, contactLookup) }
            .sortedBy { it.timestamp }
    }
}
