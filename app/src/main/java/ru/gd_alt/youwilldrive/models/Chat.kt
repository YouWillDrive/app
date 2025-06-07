package ru.gd_alt.youwilldrive.models

import ru.gd_alt.youwilldrive.data.client.Connection

class Chat(override val id: String) : Identifiable {
    companion object : ModelCompanion<Chat> {
        override val tableName: String = "chats"

        override fun fromDictionary(dictionary: Map<*, *>): Chat {
            return Chat(dictionary["id"]!!.toString())
        }

        suspend fun sendMessage(chat: Chat, message: Message) {
            Connection.cl.run(
                "RELATE ${message.id}->belongs_to->${chat.id}"
            )
        }

        suspend fun create(user1: User, user2: User): Chat {
            val chat = (Connection.cl.insert("chats", mapOf())!![0] as Map <*, *>)["id"]!!.toString()

            Connection.cl.run(
                "RELATE ${chat}->participates->${user1}"
            )
            Connection.cl.run(
                "RELATE ${chat}->participates->${user2}"
            )

            return Chat(chat)
        }

        suspend fun byParticipants(user1: User, user2: User): Chat? {
            val result = Connection.cl.run(
                "SELECT * FROM (SELECT (SELECT * FROM ->users)[0] as user1, (SELECT * FROM ->users)[1] as user2, (SELECT * FROM <-chats)[0] as chat FROM participates) WHERE (user1.id = '${user1.id}' AND user2.id = '${user2.id}') OR (user1.id = '${user2.id}' AND user2.id = '${user1.id}')"
            )

            if ((result as List<*>).isEmpty()) {
                return null
            }

            return fromDictionary((result[0] as Map<*, *>)["chat"] as Map<*, *>)
        }
    }

    suspend fun messages(): List<Message> {
        return fetchRelatedList(
            "belongs_to",
            Message::fromId,
            true
        )
    }
}