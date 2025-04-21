package ru.gd_alt.youwilldrive.models

class EventType(val id: String, var name: String) {
    override fun equals(other: Any?): Boolean {
        return this.id == (other as EventType).id
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}