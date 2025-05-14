package ru.gd_alt.youwilldrive.models

import android.util.Log
import kotlinx.datetime.toJavaLocalDateTime
import ru.gd_alt.youwilldrive.data.client.Connection
import ru.gd_alt.youwilldrive.data.models.RecordID
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class Notification(override val id: String, val title: String, val message: String, val payload: List<Any?>, var dateSent: LocalDateTime, var received: Boolean, var read: Boolean) : Identifiable {
    companion object : ModelCompanion<Notification> {
        override val tableName: String = "notifications"

        override fun fromDictionary(dictionary: Map<*, *>): Notification {
            return Notification(
                dictionary["id"]!!.toString(),
                dictionary["title"]!!.toString(),
                dictionary["message"]!!.toString(),
                dictionary["payload"] as List<Any?>,
                (dictionary["date_sent"]!! as kotlinx.datetime.LocalDateTime).toJavaLocalDateTime(),
                dictionary["received"]!!.toString().toBoolean(),
                dictionary["read"]!!.toString().toBoolean()
            )
        }

        /**
         * Post a notification to the server.
         * @param body The body of the notification.
         * @return Nothing.
         */
        suspend fun postNotification(title: String, message: String, payload: List<Any?>, receiver: String) {
            Log.d("Notification", "Posting notification: $title, $message, $payload, $receiver")
            val result = Connection.cl.insert(
                tableName,
                mutableMapOf(
                    "title" to title,
                    "message" to message,
                    "payload" to payload,
                    "date_sent" to LocalDateTime.now(),
                    "received" to false,
                    "read" to false
                )
            ) as List<Map<String, Any?>>
            Log.d("Notification", "Result: $result")
            val notificationId = result[0]["id"]!!.toString()
            val result2 = Connection.cl.query(
                "RELATE \$notificationId->is_for->\$receiver",
                mapOf(
                    "notificationId" to RecordID(notificationId.split(":")[0], notificationId.split(":")[1]),
                    "receiver" to RecordID(receiver.split(":")[0], receiver.split(":")[1])
                )
            )
            Log.d("Notification", "Result2: $result2")
        }
    }

    suspend fun receiver(): User? {
        return fetchRelatedSingle<User>("is_for", User::fromId)
    }

    /**
     * Applies the changes to the notification.
     * @return Nothing.
     */
    suspend fun update() {
        val result = Connection.cl.query(
            "UPDATE \$id SET title = \$title, message = \$message, payload = \$payload, date_sent = \$dateSent, received = \$received, read = \$read",
            mapOf(
                "id" to RecordID(id.split(":")[0], id.split(":")[1]),
                "title" to title,
                "message" to message,
                "payload" to payload,
                "dateSent" to dateSent,
                "received" to received,
                "read" to read
            )
        )
        Log.d("Notification", "Update result: $result")
    }
}