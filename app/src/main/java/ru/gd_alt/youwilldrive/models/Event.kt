package ru.gd_alt.youwilldrive.models

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime

class Event(override val id: String, var date: LocalDateTime) : Identifiable {
    companion object : ModelCompanion<Event> {
        override val tableName: String = "event"

        override fun fromDictionary(dictionary: Map<*, *>): Event {
            return Event(dictionary["id"]!!.toString(), dictionary["date_time"]!! as LocalDateTime)
        }
    }

    suspend fun eventType(): EventType? {
        return fetchRelatedSingle<EventType>("of_type", EventType::fromId)
    }

    suspend fun ofCadet(): Cadet? {
        return fetchRelatedSingle<Cadet>("event_of_cadet", Cadet::fromId)
    }

    suspend fun ofInstructor(): Instructor? {
        return fetchRelatedSingle<Instructor>("event_of_instructor", Instructor::fromId)
    }
}

fun main() {
    runBlocking {
        println(User.fromId("users:kcri1xildstepqjkxmhv")?.isCadet()?.events())
    }
}