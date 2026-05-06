package com.example.timelineexporter.plugins.email

import android.content.Context
import com.example.timelineexporter.Message
import com.example.timelineexporter.plugins.MessageSourcePlugin

/**
 * Plugin that loads email messages (Gmail / IMAP).
 *
 * No Shizuku or root access is required — email is fetched via standard APIs.
 * The plugin always reports itself as available so that stub messages are
 * included in the merged output.  When real fetch logic is added, [isAvailable]
 * should check for a configured account / valid credentials instead.
 */
class EmailPlugin : MessageSourcePlugin {

    override fun isAvailable(context: Context): Boolean {
        // TODO: check that at least one email account is configured
        return true
    }

    override fun loadMessages(
        context: Context,
        contactLookup: ((String) -> String?)?
    ): List<Message> {
        val reader = EmailReader()
        // TODO: decide at runtime which source to use (Gmail or IMAP), based
        //       on the configured account type.  For now both return empty lists
        //       so calling both is a no-op.
        return reader.readViaGmail() + reader.readViaImap()
    }
}
