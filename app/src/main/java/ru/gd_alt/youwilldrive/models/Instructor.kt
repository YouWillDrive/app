package ru.gd_alt.youwilldrive.models

class Instructor(override val id: String) : Participant {
    companion object: ModelCompanion<Instructor> {
        override val tableName: String = "instructor"

        override fun fromDictionary(dictionary: Map<*, *>): Instructor {
            return Instructor(dictionary["id"]!!.toString())
        }
    }

    override suspend fun me() : User? {
        return fetchRelatedSingle<User>("is_instructor", User::fromId, true)
    }

    suspend fun cars(): MutableList<Car> {
        return fetchRelatedList<Car>("has_car", Car::fromId)
    }
}