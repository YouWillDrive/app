package ru.gd_alt.youwilldrive.models

import ru.gd_alt.youwilldrive.data.client.Connection


class Cadet(override val id: String, var hoursAlready: Int, val expansions: Map<String, Any?> = mapOf()) : Participant {
    companion object: ModelCompanion<Cadet> {
        override val tableName: String = "cadet"

        override fun fromDictionary(dictionary: Map<*, *>): Cadet {
            val additions: MutableMap<String, Any?> = mutableMapOf<String, Any?>()

            if (dictionary["avatar"] != null) {
                additions["avatar"] = dictionary["avatar"]!!.toString()
            } else {
                additions["avatar"] = null
            }

            return Cadet(
                dictionary["id"]!!.toString(),
                dictionary["hours_already"]!!.toString().toInt(),
                additions
            )
        }

        suspend fun allWithPhotos(): MutableList<Cadet> {
            val rawObj = (Connection.cl.query(
                "SELECT *, (<-is_cadet<-users)[0].avatar AS avatar FROM cadet"
            ) as List<Map<*, *>>)[0]["result"] as List<Map<*, *>>

            return rawObj.map { fromDictionary(it) }.toMutableList()
        }
    }

    override suspend fun me() : User? {
        return fetchRelatedSingle<User>("is_cadet", User::fromId, true)
    }

    suspend fun planHistoryPoints() : MutableList<PlanHistoryPoint> {
        return fetchRelatedList<PlanHistoryPoint>("of_cadet", PlanHistoryPoint::fromId, true)
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