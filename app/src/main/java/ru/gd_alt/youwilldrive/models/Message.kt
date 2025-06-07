package ru.gd_alt.youwilldrive.models

import android.util.Log
import kotlinx.datetime.LocalDateTime
import ru.gd_alt.youwilldrive.data.client.Connection

class Message(override val  id: String, val text: String, val delivered: Boolean, val sent: Boolean, val dateSent: LocalDateTime) : Identifiable {
    companion object : ModelCompanion<Message> {
        override val tableName: String = "messages"

        override fun fromDictionary(dictionary: Map<*, *>): Message {
            return Message(dictionary["id"]!!.toString(), dictionary["text"]!!.toString(), dictionary["delivered"] as Boolean, dictionary["read"] as Boolean, dictionary["date_sent"] as LocalDateTime)
        }

        suspend fun create(text: String) : Message {
            val rawObj = Connection.cl.insert(
                tableName,
                mapOf(
                    "text" to text
                )
            )!![0] as Map<*, *>

            Log.d("Message", "Created message: $rawObj")

            val obj = fromDictionary(
                rawObj
            )

            return obj
        }
    }
}