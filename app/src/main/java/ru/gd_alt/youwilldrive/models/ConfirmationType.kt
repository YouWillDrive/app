package ru.gd_alt.youwilldrive.models

class ConfirmationType(override val id: String, var name: String) : Identifiable {
    companion object : ModelCompanion<ConfirmationType> {
        override val tableName: String = "confirmation_types"

        override fun fromDictionary(dictionary: Map<*, *>): ConfirmationType {
            return ConfirmationType(
                dictionary["id"]!!.toString(),
                dictionary["name"]!!.toString()
            )
        }
    }

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConfirmationType

        if (id != other.id) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}