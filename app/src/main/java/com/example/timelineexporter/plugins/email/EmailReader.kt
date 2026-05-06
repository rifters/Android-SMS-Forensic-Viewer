package com.example.timelineexporter.plugins.email

import com.example.timelineexporter.Message

/**
 * Reader responsible for fetching email messages.
 *
 * Both methods are stubs.  Real Gmail / IMAP access will be added in a future
 * step — for now each method returns an empty list so that the plugin system
 * compiles and runs end-to-end.
 */
class EmailReader {

    /**
     * Fetches messages using the Gmail REST API.
     *
     * TODO: implement — authenticate with OAuth 2.0, call the Gmail API
     *       (messages.list / messages.get), and convert results to [Message].
     */
    fun readViaGmail(): List<Message> {
        // TODO: implement Gmail API fetch
        return emptyList()
    }

    /**
     * Fetches messages from a generic IMAP server.
     *
     * TODO: implement — connect to the configured IMAP host, authenticate,
     *       iterate INBOX messages, and convert results to [Message].
     */
    fun readViaImap(): List<Message> {
        // TODO: implement IMAP fetch
        return emptyList()
    }
}
