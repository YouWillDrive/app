package ru.gd_alt.youwilldrive.models

import kotlinx.datetime.LocalDateTime
import ru.gd_alt.youwilldrive.data.client.Connection

class Message(override val  id: String, val text: String, val delivered: Boolean, val sent: Boolean, val dateSent: LocalDateTime) : Identifiable {
    companion object : ModelCompanion<Message> {
        override val tableName: String = "messages"

        override fun fromDictionary(dictionary: Map<*, *>): Message {
            return Message(dictionary["id"]!!.toString(), dictionary["text"]!!.toString(), dictionary["delivered"] as Boolean, dictionary["sent"] as Boolean, dictionary["date_sent"] as LocalDateTime)
        }

        suspend fun create(text: String, sender: User) : Message {
            val obj = fromDictionary(
                Connection.cl.insert(
                    tableName,
                    mapOf(
                        "text" to text
                    )
                )!![0] as Map<*, *>
            )

            Connection.cl.run(
                "RELATE ${obj.id}->sent_by->${sender.id}"
            )

            return obj
        }
    }
}