package com.example.timelineexporter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the core data model and utility functions.
 * These tests run on the JVM (no Android emulator required).
 */
class MessageModelTest {

    // ── MessageType ────────────────────────────────────────────────────────

    @Test
    fun `MessageType values are SMS and MMS`() {
        assertEquals(2, MessageType.values().size)
        assertTrue(MessageType.values().contains(MessageType.SMS))
        assertTrue(MessageType.values().contains(MessageType.MMS))
    }

    // ── Thread.isGroup ─────────────────────────────────────────────────────

    @Test
    fun `Thread isGroup is false for single participant`() {
        val thread = Thread(
            threadId         = 1L,
            participants     = listOf("+15551234567"),
            participantNames = listOf("Alice"),
            displayName      = "Alice"
        )
        assertFalse(thread.isGroup)
    }

    @Test
    fun `Thread isGroup is true for multiple participants`() {
        val thread = Thread(
            threadId         = 2L,
            participants     = listOf("+15551234567", "+15559876543"),
            participantNames = listOf("Alice", "Bob"),
            displayName      = "Alice, Bob"
        )
        assertTrue(thread.isGroup)
    }

    // ── Utils.escapeHtml ───────────────────────────────────────────────────

    @Test
    fun `escapeHtml converts ampersand`() {
        assertEquals("a &amp; b", Utils.escapeHtml("a & b"))
    }

    @Test
    fun `escapeHtml converts angle brackets`() {
        assertEquals("&lt;b&gt;bold&lt;/b&gt;", Utils.escapeHtml("<b>bold</b>"))
    }

    @Test
    fun `escapeHtml converts double quotes`() {
        assertEquals("say &quot;hello&quot;", Utils.escapeHtml("""say "hello""""))
    }

    @Test
    fun `escapeHtml converts single quotes`() {
        assertEquals("it&#39;s", Utils.escapeHtml("it's"))
    }

    @Test
    fun `escapeHtml converts newlines to br`() {
        assertEquals("line1<br>line2", Utils.escapeHtml("line1\nline2"))
    }

    @Test
    fun `escapeHtml leaves plain text unchanged`() {
        assertEquals("Hello world", Utils.escapeHtml("Hello world"))
    }

    // ── Utils.normalisePhone ──────────────────────────────────────────────

    @Test
    fun `normalisePhone strips dashes and spaces`() {
        assertEquals("5551234567", Utils.normalisePhone("555-123-4567"))
    }

    @Test
    fun `normalisePhone strips US country code from 11-digit number`() {
        assertEquals("5551234567", Utils.normalisePhone("+15551234567"))
    }

    @Test
    fun `normalisePhone strips parentheses`() {
        assertEquals("5551234567", Utils.normalisePhone("(555) 123-4567"))
    }

    @Test
    fun `normalisePhone leaves 10-digit number unchanged`() {
        assertEquals("5551234567", Utils.normalisePhone("5551234567"))
    }

    // ── Utils.withinWindow ────────────────────────────────────────────────

    @Test
    fun `withinWindow returns true for identical timestamps`() {
        assertTrue(Utils.withinWindow(1000L, 1000L))
    }

    @Test
    fun `withinWindow returns true when within default 30s window`() {
        assertTrue(Utils.withinWindow(0L, 29_000L))
    }

    @Test
    fun `withinWindow returns false when outside default 30s window`() {
        assertFalse(Utils.withinWindow(0L, 31_000L))
    }

    @Test
    fun `withinWindow respects custom window`() {
        assertTrue(Utils.withinWindow(0L, 5_000L, windowMs = 10_000L))
        assertFalse(Utils.withinWindow(0L, 15_000L, windowMs = 10_000L))
    }

    // ── Utils.formatTime ──────────────────────────────────────────────────

    @Test
    fun `formatTime returns non-empty string for valid timestamp`() {
        val result = Utils.formatTime(1_715_000_000_000L)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatDate returns non-empty string for valid timestamp`() {
        val result = Utils.formatDate(1_715_000_000_000L)
        assertTrue(result.isNotEmpty())
    }

    // ── Utils.groupIntoThreads ─────────────────────────────────────────────

    @Test
    fun `groupIntoThreads groups messages by threadId`() {
        val messages = listOf(
            makeMessage(id = 1L, threadId = 10L, timestamp = 1000L),
            makeMessage(id = 2L, threadId = 20L, timestamp = 2000L),
            makeMessage(id = 3L, threadId = 10L, timestamp = 3000L)
        )
        val threads = Utils.groupIntoThreads(messages, emptyMap())
        assertEquals(2, threads.size)
        assertEquals(2, threads[10L]?.messages?.size)
        assertEquals(1, threads[20L]?.messages?.size)
    }

    @Test
    fun `groupIntoThreads sorts messages within thread by timestamp`() {
        val messages = listOf(
            makeMessage(id = 2L, threadId = 10L, timestamp = 3000L),
            makeMessage(id = 1L, threadId = 10L, timestamp = 1000L)
        )
        val threads = Utils.groupIntoThreads(messages, emptyMap())
        val sorted = threads[10L]?.messages!!
        assertTrue(sorted[0].timestamp < sorted[1].timestamp)
    }

    @Test
    fun `groupIntoThreads resolves display name from contactMap`() {
        val messages = listOf(
            makeMessage(id = 1L, threadId = 10L, sender = "+15551234567", isIncoming = true)
        )
        val contactMap = mapOf("5551234567" to "Alice")
        val threads = Utils.groupIntoThreads(messages, contactMap)
        assertEquals("Alice", threads[10L]?.displayName)
    }

    // ── HtmlGenerator ─────────────────────────────────────────────────────

    @Test
    fun `buildTimeline HTML contains timeline div`() {
        val generator = HtmlGenerator()
        val messages = listOf(makeMessage(id = 1L, threadId = 1L))
        val threads = Utils.groupIntoThreads(messages, emptyMap())
        val html = generator.buildTimeline(messages, threads)
        assertTrue(html.contains("""id="timeline""""))
    }

    @Test
    fun `buildTimeline HTML contains message onclick`() {
        val generator = HtmlGenerator()
        val msg = makeMessage(id = 42L, threadId = 7L)
        val messages = listOf(msg)
        val threads = Utils.groupIntoThreads(messages, emptyMap())
        val html = generator.buildTimeline(messages, threads)
        assertTrue(html.contains("openThread(7, 42)"))
    }

    @Test
    fun `buildThreadPage HTML contains message anchor`() {
        val generator = HtmlGenerator()
        val msg = makeMessage(id = 55L, threadId = 3L)
        val thread = Thread(
            threadId         = 3L,
            participants     = listOf("+15551234567"),
            participantNames = listOf("Bob"),
            displayName      = "Bob",
            messages         = mutableListOf(msg)
        )
        val html = generator.buildThreadPage(thread)
        assertTrue(html.contains("""id="msg_55""""))
    }

    @Test
    fun `buildThreadPage HTML contains back link to timeline`() {
        val generator = HtmlGenerator()
        val thread = Thread(
            threadId         = 3L,
            participants     = listOf("+15551234567"),
            participantNames = listOf("Bob"),
            displayName      = "Bob",
            messages         = mutableListOf(makeMessage(id = 1L, threadId = 3L))
        )
        val html = generator.buildThreadPage(thread)
        assertTrue(html.contains("timeline.html"))
    }

    @Test
    fun `buildTimeline escapes HTML in message body`() {
        val generator = HtmlGenerator()
        val msg = makeMessage(id = 1L, threadId = 1L, body = "<script>alert('xss')</script>")
        val messages = listOf(msg)
        val threads = Utils.groupIntoThreads(messages, emptyMap())
        val html = generator.buildTimeline(messages, threads)
        assertFalse(html.contains("<script>"))
        assertTrue(html.contains("&lt;script&gt;"))
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun makeMessage(
        id: Long = 1L,
        threadId: Long = 1L,
        timestamp: Long = 1_715_000_000_000L,
        sender: String = "+15551234567",
        senderName: String? = null,
        body: String = "Hello",
        type: MessageType = MessageType.SMS,
        isIncoming: Boolean = true
    ) = Message(
        id          = id,
        threadId    = threadId,
        timestamp   = timestamp,
        sender      = sender,
        senderName  = senderName,
        body        = body,
        type        = type,
        isIncoming  = isIncoming
    )
}
