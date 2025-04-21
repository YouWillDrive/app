package ru.gd_alt.youwilldrive.models

class EventType(override val id: String, var name: String) : Identifiable {
    companion object : ModelCompanion<EventType> {
        override val tableName: String = "event_type"

        override fun fromDictionary(dictionary: Map<*, *>): EventType {
            return EventType(dictionary["id"]!!.toString(), dictionary["name"]!!.toString())
        }
    }

    override fun equals(other: Any?): Boolean {
        return this.id == (other as EventType).id
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}