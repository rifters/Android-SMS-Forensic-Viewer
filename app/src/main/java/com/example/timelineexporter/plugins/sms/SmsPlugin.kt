package com.example.timelineexporter.plugins.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.timelineexporter.Message
import com.example.timelineexporter.MmsReader
import com.example.timelineexporter.SmsReader
import com.example.timelineexporter.plugins.MessageSourcePlugin

/**
 * Plugin that wraps the existing [SmsReader] and [MmsReader].
 *
 * No SMS/MMS reading logic is duplicated here — this class is purely a thin
 * adapter so that the rest of the app can treat SMS/MMS as just another plugin.
 *
 * Availability: requires the READ_SMS permission to be granted at runtime.
 */
class SmsPlugin : MessageSourcePlugin {

    override fun isAvailable(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED

    override fun loadMessages(
        context: Context,
        contactLookup: ((String) -> String?)?
    ): List<Message> {
        val resolver = context.contentResolver
        val sms = SmsReader(resolver).readAll(contactLookup)
        val mms = MmsReader(resolver).readAll(contactLookup)
        return sms + mms
    }
}
