package ru.gd_alt.youwilldrive.models

class Plan(override val id: String, var name: String, var theoryHours: Int, var practiceHours: Int, var price: Float) : Identifiable {
    companion object: ModelCompanion<Plan> {
        override val tableName: String = "plan"

        override fun fromDictionary(dictionary: Map<*, *>): Plan {
            return Plan(
                dictionary["id"]!!.toString(),
                dictionary["name"]!!.toString(),
                dictionary["theory_hours"]!!.toString().toInt(),
                dictionary["practice_hours"]!!.toString().toInt(),
                dictionary["price"]!!.toString().toFloat()
            )
        }
    }
}