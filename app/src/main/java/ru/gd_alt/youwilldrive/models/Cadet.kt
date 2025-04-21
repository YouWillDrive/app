package ru.gd_alt.youwilldrive.models

import kotlinx.coroutines.runBlocking
import ru.gd_alt.youwilldrive.data.client.Connection


class Cadet(override val id: String, var hoursAlready: Int) : Identifiable {
    companion object: ModelCompanion<Cadet> {
        override val tableName: String = "cadet"

        override fun fromDictionary(dictionary: Map<*, *>): Cadet {
            return Cadet(
                dictionary["id"]!!.toString(),
                dictionary["hours_already"]!!.toString().toInt()
            )
        }
    }
}