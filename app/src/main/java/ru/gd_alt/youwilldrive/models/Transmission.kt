package ru.gd_alt.youwilldrive.models

import kotlinx.coroutines.runBlocking
import ru.gd_alt.youwilldrive.data.client.Connection

class Transmission(override val id: String, val name: String): Identifiable {
    companion object: ModelCompanion<Transmission> {
        override val tableName: String = "transmissions"

        override fun fromDictionary(dictionary: Map<*, *>): Transmission {
            return Transmission(
                dictionary["id"]!!.toString(),
                dictionary["name"]!!.toString()
            )
        }

        suspend fun fromName(name: String): Transmission? {
            val transmissions: List<Transmission> = all()
            for (transmission in transmissions) {
                if (transmission.name == name) {
                    return transmission
                }
            }
            runBlocking {
                Connection.cl.insert("transmissions", mapOf("name" to name))
            }
            val transmissionsNew: List<Transmission> = all()
            for (transmission in transmissionsNew) {
                if (transmission.name == name) {
                    return transmission
                }
            }
            return Transmission("0", "none")
        }
    }
}