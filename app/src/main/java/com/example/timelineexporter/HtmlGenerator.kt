package com.example.timelineexporter

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates all HTML output:
 *  - timeline.html   — merged, chronological view across all conversations
 *  - thread_<id>.html — per-conversation view with anchor-based highlighting
 */
class HtmlGenerator {

    /**
     * Build the main timeline HTML page from a list of all messages (already sorted by timestamp).
     *
     * @param allMessages  All messages, sorted by ascending timestamp.
     * @param threads      Thread metadata map (threadId → Thread).
     * @return The complete HTML string for timeline.html.
     */
    fun buildTimeline(
        allMessages: List<Message>,
        threads: Map<Long, Thread>
    ): String {
        val sb = StringBuilder()
        sb.appendLine(htmlHeader("SMS / MMS Timeline", extraCss = "timeline"))

        sb.appendLine("""
            <div class="page-header">
                <h1>📱 Message Timeline</h1>
                <p class="subtitle">${allMessages.size} messages across ${threads.size} conversations</p>
                <div class="toolbar">
                    <input type="text" id="search" placeholder="🔍 Search messages…" oninput="filterMessages(this.value)">
                    <label for="dateJump">Jump to date:</label>
                    <input type="date" id="dateJump" onchange="jumpToDate(this.value)">
                </div>
            </div>
            <div id="timeline">
        """.trimIndent())

        var lastDateLabel = ""
        var lastTimestamp = 0L

        for (msg in allMessages) {
            val thread = threads[msg.threadId]
            val dateLabel = Utils.formatDate(msg.timestamp)

            // Date separator
            if (dateLabel != lastDateLabel) {
                sb.appendLine("""<div class="date-separator" data-date="${isoDate(msg.timestamp)}"><span>$dateLabel</span></div>""")
                lastDateLabel = dateLabel
            }

            val timeLabel   = Utils.formatTime(msg.timestamp)
            val senderLabel = msg.senderName ?: msg.sender
            val threadName  = thread?.displayName ?: "Thread ${msg.threadId}"
            val bodyHtml    = Utils.escapeHtml(msg.body)
            val typeClass   = if (msg.isIncoming) "incoming" else "outgoing"
            val msgTypeTag  = msg.type.name.lowercase()
            val groupTag    = if (thread?.isGroup == true) """<span class="group-tag">Group</span>""" else ""
            val attachTag   = buildAttachmentTag(msg.mediaAttachments)

            sb.appendLine("""
                <div class="msg $typeClass" data-ts="${msg.timestamp}" data-thread="${msg.threadId}"
                     onclick="openThread(${msg.threadId}, ${msg.id})"
                     title="$threadName — $timeLabel">
                    <div class="msg-meta">
                        <span class="time">$timeLabel</span>
                        <span class="thread-name">$threadName</span>
                        $groupTag
                        <span class="type-badge $msgTypeTag">$msgTypeTag</span>
                    </div>
                    <div class="sender">$senderLabel</div>
                    <div class="text">$bodyHtml$attachTag</div>
                </div>
            """.trimIndent())

            lastTimestamp = msg.timestamp
        }

        sb.appendLine("</div>") // #timeline
        sb.appendLine(htmlFooter(includeTimelineScript = true))
        return sb.toString()
    }

    /**
     * Build a thread page for a single conversation.
     *
     * @param thread  The thread metadata + messages.
     * @return The complete HTML string for thread_<id>.html.
     */
    fun buildThreadPage(thread: Thread): String {
        val sb = StringBuilder()
        sb.appendLine(htmlHeader(thread.displayName, extraCss = "thread"))

        val participantsHtml = thread.participantNames.joinToString(", ") { Utils.escapeHtml(it) }
        val groupClass = if (thread.isGroup) "group" else "direct"

        sb.appendLine("""
            <div class="thread-header $groupClass">
                <a href="timeline.html" class="back-btn">← Timeline</a>
                <h1>${Utils.escapeHtml(thread.displayName)}</h1>
                <p class="participants">$participantsHtml</p>
            </div>
            <div id="messages">
        """.trimIndent())

        var lastDateLabel = ""

        for (msg in thread.messages) {
            val dateLabel = Utils.formatDate(msg.timestamp)

            if (dateLabel != lastDateLabel) {
                sb.appendLine("""<div class="date-separator"><span>$dateLabel</span></div>""")
                lastDateLabel = dateLabel
            }

            val timeLabel   = Utils.formatTime(msg.timestamp)
            val senderLabel = msg.senderName ?: msg.sender
            val bodyHtml    = Utils.escapeHtml(msg.body)
            val typeClass   = if (msg.isIncoming) "incoming" else "outgoing"
            val attachTag   = buildAttachmentTag(msg.mediaAttachments)

            sb.appendLine("""
                <div id="msg_${msg.id}" class="message $typeClass" data-ts="${msg.timestamp}">
                    <div class="bubble">
                        <div class="sender">$senderLabel</div>
                        <div class="text">$bodyHtml$attachTag</div>
                        <div class="time">$timeLabel</div>
                    </div>
                </div>
            """.trimIndent())
        }

        sb.appendLine("</div>") // #messages
        sb.appendLine(htmlFooter(includeTimelineScript = false))
        return sb.toString()
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private fun htmlHeader(title: String, extraCss: String): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>${Utils.escapeHtml(title)}</title>
            <link rel="stylesheet" href="assets/style.css">
            <link rel="stylesheet" href="assets/$extraCss.css">
        </head>
        <body class="$extraCss-page">
    """.trimIndent()

    private fun htmlFooter(includeTimelineScript: Boolean): String {
        val scriptTag = if (includeTimelineScript)
            """<script src="assets/script.js"></script>"""
        else
            """<script src="assets/thread.js"></script>"""
        return """
            $scriptTag
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildAttachmentTag(attachments: List<MediaAttachment>?): String {
        if (attachments.isNullOrEmpty()) return ""
        val sb = StringBuilder("""<div class="attachments">""")
        for (att in attachments) {
            val safeType = Utils.escapeHtml(att.mimeType)
            val safeName = Utils.escapeHtml(att.fileName ?: "attachment")
            when {
                att.mimeType.startsWith("image/") ->
                    sb.append("""<span class="attachment image-att">🖼 $safeName</span>""")
                att.mimeType.startsWith("video/") ->
                    sb.append("""<span class="attachment video-att">🎥 $safeName</span>""")
                att.mimeType.startsWith("audio/") ->
                    sb.append("""<span class="attachment audio-att">🎵 $safeName</span>""")
                else ->
                    sb.append("""<span class="attachment file-att">📎 $safeName ($safeType)</span>""")
            }
        }
        sb.append("</div>")
        return sb.toString()
    }

    private fun isoDate(timestampMs: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestampMs))
}
