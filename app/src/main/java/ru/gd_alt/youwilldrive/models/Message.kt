package ru.gd_alt.youwilldrive.models

import android.util.Log
import kotlinx.datetime.LocalDateTime
import ru.gd_alt.youwilldrive.data.client.Connection
import ru.gd_alt.youwilldrive.data.models.RecordID
import kotlin.collections.mapOf

class Message(override val  id: String, val text: String, val delivered: Boolean, val sent: Boolean, val dateSent: LocalDateTime, val expansions: Map<*, *> = mapOf<String, Any?>()) : Identifiable {
    companion object : ModelCompanion<Message> {
        override val tableName: String = "messages"

        override fun fromDictionary(dictionary: Map<*, *>): Message {
            var additions: MutableMap<String, Any?> = mutableMapOf<String, Any?>()

            if (dictionary["sender"] != null) {
                additions["sender"] = dictionary["sender"]!!.toString()
            } else {
                additions["sender"] = null
            }
            if (dictionary["chat"] != null) {
                additions["chat"] = dictionary["chat"]!!.toString()
            } else {
                additions["chat"] = null
            }

            return Message(
                dictionary["id"]!!.toString(),
                dictionary["text"]!!.toString(),
                dictionary["delivered"] as Boolean,
                dictionary["read"] as Boolean,
                dictionary["date_sent"] as LocalDateTime,
                additions
            )
        }

        /** Fetches all messages from the database with sender ID and chat ID from chat ID.
         * @return List of all messages.
         */
        suspend fun allWithChatAndSender(chatId: String): MutableList<Message> {
            val rawObj = (Connection.cl.query(
                "SELECT * FROM (SELECT *, (->sent_by->users)[0] AS sender, (->belongs_to->chats)[0] AS chat FROM messages) WHERE chat = \$chatId ORDER BY date_sent ASC;",
                mapOf("chatId" to RecordID(chatId.split(":")[0], chatId.split(":")[1]))
            ) as List<Map<*, *>>)[0]["result"] as List<Map<*, *>>

            Log.d("Message", "Fetched messages with chat and sender: $rawObj")

            return rawObj.map { fromDictionary(it) }.toMutableList()
        }

        suspend fun create(text: String): Message {
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