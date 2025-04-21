package ru.gd_alt.youwilldrive.models

import kotlinx.coroutines.runBlocking
import ru.gd_alt.youwilldrive.data.client.Connection


class CarPhoto(override val id: String, var photo: String) : Identifiable {
    companion object: ModelCompanion<CarPhoto> {
        override val tableName: String = "car_photos"

        override fun fromDictionary(dictionary: Map<*, *>): CarPhoto {
            return CarPhoto(
                dictionary["id"]!!.toString(),
                dictionary["photo"]!!.toString()
            )
        }
    }
}