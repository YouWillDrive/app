package ru.gd_alt.youwilldrive.models

import ru.gd_alt.youwilldrive.data.client.Connection
import android.util.Log
import ru.gd_alt.youwilldrive.data.models.RecordID

class Chat(override val id: String) : Identifiable {
    companion object : ModelCompanion<Chat> {
        override val tableName: String = "chats"

        override fun fromDictionary(dictionary: Map<*, *>): Chat {
            return Chat(dictionary["id"]!!.toString())
        }

        suspend fun sendMessage(chat: Chat, message: Message, fromUser: User) {
            Connection.cl.query(
                "RELATE ${message.id}->sent_by->${fromUser.id}"
            )
            Connection.cl.query(
                "RELATE ${message.id}->belongs_to->${chat.id}"
            )
        }

        suspend fun create(user1: User, user2: User): Chat {
            val chat = (Connection.cl.insert("chats", mapOf())!![0] as Map <*, *>)["id"]!!.toString()

            Connection.cl.query(
                "RELATE ${chat}->participates->${user1.id}"
            )
            Connection.cl.query(
                "RELATE ${chat}->participates->${user2.id}"
            )

            return Chat(chat)
        }

        suspend fun byParticipants(user1: User, user2: User): Chat? {
            val result = Connection.cl.query(
                "SELECT * FROM (SELECT *, (SELECT * FROM ->participates->users)[0] as user1, (SELECT * FROM ->participates->users)[1] as user2 FROM chats) WHERE (user1.id = \$user1 OR user2.id = \$user2) OR (user1.id = \$user2 OR user2.id = \$user1)",
                mapOf(
                    "user1" to RecordID(user1.id.split(":")[0], user1.id.split(":")[1]),
                    "user2" to RecordID(user2.id.split(":")[0], user2.id.split(":")[1])
                )
            )

            Log.d("Chat", "byParticipants: Result of query: $result")

            if ((((result as List<*>)[0] as Map<*, *>)["result"] as List<*>).isEmpty()) {
                return create(user1, user2)
            }

            Log.d("Chat", "byParticipants: Found existing chat with $result")

            Log.d("Chat", "byParticipants: Found chat with $result")
            return fromId((((result[0] as Map<*, *>)["result"] as List<*>)[0] as Map<*, *>)["id"].toString())
        }
    }

    suspend fun messages(): List<Message> {
        Log.d("Chat", "Fetching messages for chat $id")
        return fetchRelatedList("belongs_to", Message::fromId, isChild = true)
    }
}