package ru.gd_alt.youwilldrive.models

import kotlinx.coroutines.runBlocking
import ru.gd_alt.youwilldrive.data.client.Connection


class Cadet(override val id: String, var hoursAlready: Int) : Identifiable {
    companion object: ModelCompanion<Cadet> {
        override val tableName: String = "cadet"

        override fun fromDictionary(dictionary: Map<*, *>): Cadet {
            return Cadet(
                dictionary["id"]!!.toString(),
                dictionary["hours_already"]!!.toString().toInt()
            )
        }
    }

    suspend fun planHistoryPoints() : MutableList<PlanHistoryPoint> {
        return fetchRelatedList<PlanHistoryPoint>("has_plan_points", PlanHistoryPoint::fromId, true)
    }

    suspend fun actualPlanPoint() : PlanHistoryPoint? {
        val points = planHistoryPoints()
        if (points.isEmpty()) {
            return null
        }
        points.sortWith(Comparator<PlanHistoryPoint> { o1, o2 ->
            o1.date.compareTo(o2.date)
        })
        points.reverse()
        return points[0]
    }

    suspend fun events() : MutableList<Event> {
        return fetchRelatedList<Event>("event_of_cadet", Event::fromId, true)
    }
}