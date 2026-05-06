package com.example.timelineexporter

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri

/**
 * Reads MMS messages from the system MMS content provider, including
 * message parts (text/media) and group-chat participants.
 */
class MmsReader(private val contentResolver: ContentResolver) {

    companion object {
        private val MMS_URI          = Uri.parse("content://mms")
        private val MMS_PARTS_URI    = Uri.parse("content://mms/part")
        private const val ADDR_TYPE_FROM = 137   // PduHeaders.FROM
        private const val ADDR_TYPE_TO   = 151   // PduHeaders.TO
        // msg_box=1 → received, msg_box=2 → sent
        private const val MSG_BOX_RECEIVED = 1
    }

    /**
     * Reads all MMS messages and returns them as a list of [Message].
     * @param contactLookup optional lambda to resolve phone numbers to contact names.
     */
    fun readAll(contactLookup: ((String) -> String?)? = null): List<Message> {
        val messages = mutableListOf<Message>()

        val cursor: Cursor? = contentResolver.query(
            MMS_URI,
            arrayOf("_id", "thread_id", "date", "msg_box"),
            null,
            null,
            "date ASC"
        )

        cursor?.use {
            val idIndex      = it.getColumnIndexOrThrow("_id")
            val threadIndex  = it.getColumnIndexOrThrow("thread_id")
            val dateIndex    = it.getColumnIndexOrThrow("date")
            val msgBoxIndex  = it.getColumnIndexOrThrow("msg_box")

            while (it.moveToNext()) {
                val mmsId    = it.getLong(idIndex)
                val threadId = it.getLong(threadIndex)
                // MMS date is in seconds; convert to milliseconds
                val dateMs   = it.getLong(dateIndex) * 1000L
                val msgBox   = it.getInt(msgBoxIndex)
                val isIncoming = (msgBox == MSG_BOX_RECEIVED)

                val participants = getAddresses(mmsId)
                val fromAddress  = participants
                    .firstOrNull { addr -> addr.type == ADDR_TYPE_FROM }
                    ?.address ?: "Unknown"

                val toAddresses = participants
                    .filter { addr -> addr.type == ADDR_TYPE_TO }
                    .map { addr -> addr.address }

                val senderAddress = if (isIncoming) fromAddress else Utils.SELF_ADDRESS
                val senderName    = if (isIncoming) contactLookup?.invoke(fromAddress) else Utils.SELF_NAME

                val groupParticipants: List<String>? =
                    if (toAddresses.size > 1) (listOf(fromAddress) + toAddresses).distinct()
                    else null

                val parts        = getParts(mmsId)
                val textBody     = parts
                    .filter { p -> p.mimeType == "text/plain" }
                    .joinToString("\n") { p -> p.text ?: "" }
                    .trim()
                val attachments  = parts
                    .filter { p -> p.mimeType != "text/plain" && p.mimeType != "application/smil" }
                    .map { p ->
                        MediaAttachment(
                            partId   = p.id,
                            mimeType = p.mimeType,
                            fileName = p.fileName,
                            filePath = null
                        )
                    }

                messages.add(
                    Message(
                        id               = mmsId,
                        threadId         = threadId,
                        timestamp        = dateMs,
                        sender           = senderAddress,
                        senderName       = senderName,
                        body             = textBody,
                        type             = MessageType.MMS,
                        isIncoming       = isIncoming,
                        groupParticipants = groupParticipants,
                        mediaAttachments  = attachments.ifEmpty { null }
                    )
                )
            }
        }

        return messages
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private data class MmsPart(
        val id: Long,
        val mimeType: String,
        val text: String?,
        val fileName: String?
    )

    private data class MmsAddress(val address: String, val type: Int)

    private fun getParts(mmsId: Long): List<MmsPart> {
        val parts = mutableListOf<MmsPart>()
        val cursor = contentResolver.query(
            MMS_PARTS_URI,
            arrayOf("_id", "ct", "text", "name"),
            "mid = ?",
            arrayOf(mmsId.toString()),
            null
        ) ?: return parts

        cursor.use {
            val idIndex       = it.getColumnIndexOrThrow("_id")
            val ctIndex       = it.getColumnIndexOrThrow("ct")
            val textIndex     = it.getColumnIndex("text")
            val nameIndex     = it.getColumnIndex("name")

            while (it.moveToNext()) {
                val partId   = it.getLong(idIndex)
                val mimeType = it.getString(ctIndex) ?: "application/octet-stream"
                val text     = if (textIndex >= 0) it.getString(textIndex) else null
                val name     = if (nameIndex >= 0) it.getString(nameIndex) else null
                parts.add(MmsPart(partId, mimeType, text, name))
            }
        }
        return parts
    }

    private fun getAddresses(mmsId: Long): List<MmsAddress> {
        val addresses = mutableListOf<MmsAddress>()
        val uri = Uri.parse("content://mms/$mmsId/addr")
        val cursor = contentResolver.query(
            uri,
            arrayOf("address", "type"),
            null,
            null,
            null
        ) ?: return addresses

        cursor.use {
            val addrIndex = it.getColumnIndexOrThrow("address")
            val typeIndex = it.getColumnIndexOrThrow("type")
            while (it.moveToNext()) {
                val address = it.getString(addrIndex) ?: continue
                val type    = it.getInt(typeIndex)
                if (address.isNotBlank() && address != "insert-address-token") {
                    addresses.add(MmsAddress(address, type))
                }
            }
        }
        return addresses
    }
}
