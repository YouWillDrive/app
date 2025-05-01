package ru.gd_alt.youwilldrive.models


class Cadet(override val id: String, var hoursAlready: Int) : Participant {
    companion object: ModelCompanion<Cadet> {
        override val tableName: String = "cadet"

        override fun fromDictionary(dictionary: Map<*, *>): Cadet {
            return Cadet(
                dictionary["id"]!!.toString(),
                dictionary["hours_already"]!!.toString().toInt()
            )
        }
    }

    override suspend fun me() : User? {
        return fetchRelatedSingle<User>("is_cadet", User::fromId, true)
    }

    suspend fun planHistoryPoints() : MutableList<PlanHistoryPoint> {
        return fetchRelatedList<PlanHistoryPoint>("has_plan_points", PlanHistoryPoint::fromId, true)
    }

    suspend fun actualPlanPoint() : PlanHistoryPoint? {
        val points = planHistoryPoints()
        if (points.isEmpty()) {
            return null
        }
        points.sortWith { o1, o2 ->
            o1.date.compareTo(o2.date)
        }
        points.reverse()
        return points[0]
    }
}