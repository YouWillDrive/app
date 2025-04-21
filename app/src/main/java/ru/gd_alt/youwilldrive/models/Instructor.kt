package ru.gd_alt.youwilldrive.models
import kotlinx.coroutines.runBlocking
import ru.gd_alt.youwilldrive.data.client.Connection

class Instructor(override val id: String) : Identifiable {
    companion object: ModelCompanion<Instructor> {
        override val tableName: String = "instructor"

        override fun fromDictionary(dictionary: Map<*, *>): Instructor {
            return Instructor(dictionary["id"]!!.toString())
        }
    }

    suspend fun cars(): MutableList<Car> {
        return fetchRelatedList<Car>("has_car", Car::fromId)
    }
}