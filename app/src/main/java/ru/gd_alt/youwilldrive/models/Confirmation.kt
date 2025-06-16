package ru.gd_alt.youwilldrive.models

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.gd_alt.youwilldrive.data.client.Connection

class Confirmation(override val id: String, var value: Long, var date: LocalDateTime) : Identifiable {
    companion object : ModelCompanion<Confirmation> {
        override val tableName: String = "confirmations"

        override fun fromDictionary(dictionary: Map<*, *>): Confirmation {
            return Confirmation(
                dictionary["id"]!!.toString(),
                dictionary["confirmation_value"] as Long,
                dictionary["date_time"] as LocalDateTime
            )
        }

        suspend fun create(value: Long): Confirmation {
            val rawObj = Connection.cl.insert(
                tableName,
                mapOf(
                    "confirmation_value" to value,
                    "date_time" to Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                )
            )!![0] as Map<*, *>

            return fromDictionary(rawObj)
        }
    }

    suspend fun confirmationType(): ConfirmationType? {
        return fetchRelatedSingle<ConfirmationType>("of_confirmation_type", ConfirmationType::fromId)
    }

    suspend fun confirmator(): User? {
        return fetchRelatedSingle<User>("confirmator", User::fromId)
    }
}