package ru.gd_alt.youwilldrive.models

import kotlinx.serialization.Serializable

@Serializable
class EventType(val id: Int, var name: String) {
    override fun equals(other: Any?): Boolean {
        return this.id == (other as EventType).id
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result
        return result
    }
}