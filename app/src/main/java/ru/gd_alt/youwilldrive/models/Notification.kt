package ru.gd_alt.youwilldrive.models

import ru.gd_alt.youwilldrive.data.client.Connection
import java.time.Instant

class Notification(override val id: String, var body: Map<String, Any?>, var dateSent: Instant, var received: Boolean, var read: Boolean) : Identifiable {
    companion object : ModelCompanion<Notification> {
        override val tableName: String = "notifications"

        override fun fromDictionary(dictionary: Map<*, *>): Notification {
            return Notification(
                dictionary["id"]!!.toString(),
                dictionary["body"] as Map<String, Any?>,
                dictionary["date_sent"]!! as Instant,
                dictionary["received"]!!.toString().toBoolean(),
                dictionary["read"]!!.toString().toBoolean()
            )
        }

        /**
         * Post a notification to the server.
         * @param body The body of the notification.
         * @return Nothing.
         */
        suspend fun postNotification(body: Map<String, Any?>, receiver: String) {
            val result = Connection.cl.insert(
                tableName,
                mapOf(
                    "body" to body,
                    "date_sent" to Instant.now(),
                    "received" to false,
                    "read" to false
                )
            )
        }
    }

    suspend fun receiver(): User? {
        return fetchRelatedSingle<User>("is_for", User::fromId)
    }
}