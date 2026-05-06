package com.example.timelineexporter

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony

/**
 * Reads SMS messages from the system SMS content provider.
 */
class SmsReader(private val contentResolver: ContentResolver) {

    companion object {
        private val SMS_URI = Telephony.Sms.CONTENT_URI
        private val PROJECTION = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE,
            Telephony.Sms.BODY,
            Telephony.Sms.TYPE
        )
        // Incoming message type
        private const val TYPE_INBOX = 1
    }

    /**
     * Reads all SMS messages and returns them as a list of [Message].
     * @param contactLookup optional lambda to resolve phone numbers to contact names.
     */
    fun readAll(contactLookup: ((String) -> String?)? = null): List<Message> {
        val messages = mutableListOf<Message>()

        val cursor: Cursor? = contentResolver.query(
            SMS_URI,
            PROJECTION,
            null,
            null,
            "${Telephony.Sms.DATE} ASC"
        )

        cursor?.use {
            val idIndex       = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val threadIdIndex = it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addressIndex  = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val dateIndex     = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val bodyIndex     = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val typeIndex     = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)

            while (it.moveToNext()) {
                val id        = it.getLong(idIndex)
                val threadId  = it.getLong(threadIdIndex)
                val address   = it.getString(addressIndex) ?: "Unknown"
                val date      = it.getLong(dateIndex)
                val body      = it.getString(bodyIndex) ?: ""
                val msgType   = it.getInt(typeIndex)
                val isIncoming = (msgType == TYPE_INBOX)

                val senderAddress = if (isIncoming) address else Utils.SELF_ADDRESS
                val senderName = if (isIncoming) contactLookup?.invoke(address) else Utils.SELF_NAME

                messages.add(
                    Message(
                        id = id,
                        threadId = threadId,
                        timestamp = date,
                        sender = senderAddress,
                        senderName = senderName,
                        body = body,
                        type = MessageType.SMS,
                        isIncoming = isIncoming
                    )
                )
            }
        }

        return messages
    }
}
