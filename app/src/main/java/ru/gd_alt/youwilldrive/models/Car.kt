package ru.gd_alt.youwilldrive.models
import kotlinx.coroutines.runBlocking
import ru.gd_alt.youwilldrive.data.client.Connection


class Car(override val id: String, var model: String, var plateNumber: String, var color: String) : Identifiable {
    companion object: ModelCompanion<Car> {
        override val tableName: String = "cars"

        override fun fromDictionary(dictionary: Map<*, *>): Car {
            return Car(
                dictionary["id"]!!.toString(),
                dictionary["model"]!!.toString(),
                dictionary["car_number"]!!.toString(),
                dictionary["color"]!!.toString()
            )
        }
    }

    suspend fun photos(): MutableList<CarPhoto> {
        return fetchRelatedList<CarPhoto>("has_photos", CarPhoto::fromId)
    }

    suspend fun transmission(): Transmission? {
        return fetchRelatedSingle<Transmission>("of_transmission", Transmission::fromId)
    }
}