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

    suspend fun cadets() : MutableList<Cadet> {
        val assignedPlanPointsMaps = fetchRelatedList<PlanHistoryPoint>("assigned_instructor", PlanHistoryPoint::fromId, true)

        // Filter to get the newest plan history point for each cadet
        val assignedPlanPoints = assignedPlanPointsMaps.groupBy { it.ofCadet() }.map { (_, planPoints) -> planPoints.maxByOrNull { it.date } }

        // Get the cadets from the plan history points
        val cadets = mutableListOf<Cadet>()
        for (planPoint in assignedPlanPoints) {
            val cadet = planPoint?.ofCadet()
            if (cadet != null) {
                cadets.add(cadet)
            }
        }

        return cadets
    }
}