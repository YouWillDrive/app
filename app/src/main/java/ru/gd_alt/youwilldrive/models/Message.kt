package ru.gd_alt.youwilldrive.models

import android.util.Log
import kotlinx.datetime.LocalDateTime
import ru.gd_alt.youwilldrive.data.client.Connection
import ru.gd_alt.youwilldrive.data.models.RecordID
import kotlin.collections.mapOf
import ru.gd_alt.youwilldrive.data.client.SurrealDBClient

class Message(override val  id: String, val text: String, val delivered: Boolean, var isRead: Boolean, val dateSent: LocalDateTime, val expansions: Map<*, *> = mapOf<String, Any?>()) : Identifiable {
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

        /**
         * Counts unread messages for a specific user in a given chat.
         * A message is considered unread if it was sent by someone other than `forUserId`
         * and its 'read' status is false.
         * @param chatId The ID of the chat.
         * @param forUserId The ID of the user for whom to count unread messages (the "receiver" in this context).
         * @return The number of unread messages.
         */
        suspend fun countUnreadInChat(chatId: String, forUserId: String): Int {
            // Query messages that belong to this chat AND are not read
            // AND were sent by someone other than forUserId
            val objects = allWithChatAndSender(chatId)

            var unreadCount = 0
            for (message in objects) {
                val senderId = message.expansions["sender"]?.toString()

                Log.d("Message", "Checking message ${message.id} sent by $senderId in chat $chatId for user $forUserId")

                // A message is unread for `forUserId` if:
                // 1. It was NOT sent by `forUserId`
                // 2. Its `isRead` status is false (already filtered by query, but double check)
                if (senderId != forUserId && !message.isRead) {
                    unreadCount++
                }
            }
            Log.i("Message", "Found $unreadCount unread messages in chat $chatId for user $forUserId")
            return unreadCount
        }
    }

    /**
     * Marks this message as read in the database.
     * @param client The SurrealDBClient instance to use. Defaults to Connection.cl.
     */
    suspend fun markAsRead(client: SurrealDBClient = Connection.cl) {
        if (!this.isRead) {
            try {
                val result = client.query(
                    "UPDATE \$id SET read = true",
                    mapOf("id" to RecordID(id.split(":")[0], id.split(":")[1]))
                )
                Log.d("Message", "Marked message $id as read in DB: $result")
                this.isRead = true // Update local object state
            } catch (e: Exception) {
                Log.e("Message", "Error marking message $id as read: ${e.message}", e)
            }
        }
    }
}