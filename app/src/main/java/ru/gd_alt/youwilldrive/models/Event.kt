package ru.gd_alt.youwilldrive.models

import android.util.Log
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import ru.gd_alt.youwilldrive.data.client.Connection
import ru.gd_alt.youwilldrive.data.models.RecordID

class Event(override val id: String, var date: LocalDateTime) : Identifiable {
    companion object : ModelCompanion<Event> {
        override val tableName: String = "event"

        override fun fromDictionary(dictionary: Map<*, *>): Event {
            return Event(dictionary["id"]!!.toString(), dictionary["date_time"]!! as LocalDateTime)
        }

        suspend fun create(dateTime: LocalDateTime): String {
            return ((Connection.cl.insert(
                "event",
                mapOf(
                    "date_time" to dateTime
                )
            )!![0] as Map<String, Any?>)["id"] as RecordID).toString()
        }
    }
    val confirmed: Boolean
        get() = runBlocking { actualConfirmationValue("confirmation_types:to_happen") == 1L }

    val declined: Boolean
        get() = runBlocking { actualConfirmationValue("confirmation_types:to_happen") == 0L }

    val durationAsked: Boolean
        get() = runBlocking { actualConfirmationValue("confirmation_types:duration") != -1L }

    val durationAccepted: Boolean
        get() = runBlocking { actualConfirmationValue("confirmation_types:duration_accept") == 1L }


    suspend fun eventType(): EventType? {
        return fetchRelatedSingle<EventType>("of_type", EventType::fromId)
    }

    suspend fun ofCadet(): Cadet? {
        return fetchRelatedSingle<Cadet>("event_of_cadet", Cadet::fromId)
    }

    suspend fun ofInstructor(): Instructor? {
        return fetchRelatedSingle<Instructor>("event_of_instructor", Instructor::fromId)
    }

    suspend fun confirmations(): MutableList<Confirmation> {
        return fetchRelatedList<Confirmation>("of_event", Confirmation::fromId, isChild = true)
    }

    suspend fun addParticipants(instructorId: String, cadetId: String) {
        Connection.cl.query(
            "RELATE ${id}->event_of_instructor->${instructorId}"
        )
        Connection.cl.query(
            "RELATE ${id}->event_of_cadet->${cadetId}"
        )
    }

    suspend fun setType(typeId: String) {
        Connection.cl.query(
            "RELATE ${id}->of_type->${typeId}"
        )
    }

    suspend fun confirm(userId: String, typeId: String, value: Long) {
        val newId = Confirmation.create(value).id
        Connection.cl.query(
            "RELATE ${newId}->of_confirmation_type->${typeId}"
        )
        Connection.cl.query(
            "RELATE ${newId}->of_event->${id}"
        )
        Connection.cl.query(
            "RELATE ${newId}->confirmator->${userId}"
        )
    }

    suspend fun confirmToHappen(userId: String, accept: Boolean = true) {
        confirm(userId,"confirmation_types:to_happen", if (accept) 1 else 0)
    }

    suspend fun confirmDuration(userId: String, value: Long) {
        confirm(userId, "confirmation_types:duration", value)
    }

    suspend fun acceptDuration(userId: String, accept: Boolean = true) {
        confirm(userId,"confirmation_types:duration_accept", if (accept) 1 else 0)
    }

    suspend fun postpone(userId: String, epochMillis: Long) {
        confirm(userId, "confirmation_types:postpone",
            date.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds())
        this.date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())

        Log.d("postpone", "${this.date} ${Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())}")
        val result = Connection.cl.query(
            "UPDATE $id SET date_time = \$dt;",
            mapOf(
                "dt" to Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
            )
        )
        Log.d("postpone", "$result")
        confirm(userId, "confirmation_types:to_happen", 0)
    }

    suspend fun actualConfirmationValue(key: String): Long {
        if (key.isEmpty() || !key.contains(':')) {
            return -1L
        }
        val (tableName, recordId) = key.split(':')
        return try {
            val result = (
                Connection.cl.query(
                    "SELECT confirmation_value, date_time\n" +
                    "FROM confirmations\n" +
                    "WHERE (->of_confirmation_type->confirmation_types)[0] = \$type_id\n" +
                    "    AND (->of_event->event)[0] = \$event_id\n" +
                    "ORDER BY date_time DESC\n" +
                    "LIMIT 1;",
                    mapOf(
                        "type_id" to RecordID(tableName, recordId),
                        "event_id" to RecordID(Event.tableName, this.id.split(':').last())
                    )
                ) as List<Map<String, List<Map<String, Long>>>>
            )[0]["result"]

            if (result?.isEmpty() == true /* in case result is null */) {
                Log.d("actualConfirmationValue", "No $key confirmations for $id")
                return -1
            }

            val value = result?.get(0)?.get("confirmation_value")

            return value ?: -1L
        } catch (e: Exception) {
            Log.e("actualConfirmationValue", "error", e)
            -1
        }
    }

    suspend fun getConfirmationStatus(): Map<String, Long> {
        return ConfirmationType.all().associate {
            it.id to actualConfirmationValue(it.id)
        }
    }
}