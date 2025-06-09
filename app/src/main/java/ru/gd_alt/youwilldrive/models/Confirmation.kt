package ru.gd_alt.youwilldrive.models

import kotlinx.datetime.LocalDateTime
import ru.gd_alt.youwilldrive.data.client.Connection
import java.time.Instant

class Confirmation(override val id: String, var value: Boolean, var date: LocalDateTime) : Identifiable {
    companion object : ModelCompanion<Confirmation> {
        override val tableName: String = "confirmations"

        override fun fromDictionary(dictionary: Map<*, *>): Confirmation {
            return Confirmation(
                dictionary["id"]!!.toString(),
                dictionary["confirmation_value"] as Boolean,
                dictionary["date_time"] as LocalDateTime
            )
        }

        suspend fun create(value: Int): Confirmation {
            val rawObj = Connection.cl.insert(
                tableName,
                mapOf("value" to value)
            )!![0] as Map<*, *>

            return fromDictionary(rawObj)
        }
    }
}