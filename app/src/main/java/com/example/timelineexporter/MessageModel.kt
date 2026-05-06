package com.example.timelineexporter

/**
 * Unified message model representing either an SMS or MMS message.
 */
data class Message(
    val id: Long,
    val threadId: Long,
    val timestamp: Long,           // milliseconds since epoch
    val sender: String,            // phone number or address
    val senderName: String?,       // resolved contact name (if available)
    val body: String,
    val type: MessageType,
    val isIncoming: Boolean,
    val groupParticipants: List<String>? = null,  // non-null for group MMS threads
    val mediaAttachments: List<MediaAttachment>? = null
)

enum class MessageType {
    SMS, MMS
}

/**
 * Represents a media attachment extracted from an MMS message part.
 */
data class MediaAttachment(
    val partId: Long,
    val mimeType: String,
    val fileName: String?,
    val filePath: String?          // local path on device (if accessible)
)

/**
 * Represents a conversation thread with metadata.
 */
data class Thread(
    val threadId: Long,
    val participants: List<String>,      // phone numbers
    val participantNames: List<String>,  // resolved names
    val displayName: String,             // "Mom", "Work Group", etc.
    val messages: MutableList<Message> = mutableListOf()
) {
    val isGroup: Boolean get() = participants.size > 1
}
