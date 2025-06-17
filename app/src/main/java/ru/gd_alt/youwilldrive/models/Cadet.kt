package ru.gd_alt.youwilldrive.models

import ru.gd_alt.youwilldrive.data.client.Connection
import android.util.Log
import ru.gd_alt.youwilldrive.data.models.RecordID


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

        suspend fun allWithPhotos(instructorUserId: String): MutableList<Cadet> {
            val (tableName, recordId) = instructorUserId.split(":")
            val rawObj = (
                Connection.cl.query(
                    "SELECT * FROM (SELECT\n" +
                    "(SELECT * FROM (SELECT <-is_cadet<-users AS user FROM \$parent)[0].user[0])[0].avatar as avatar,\n" +
                    "id,\n" +
                    "hours_already,\n" +
                    "(SELECT * FROM <-of_cadet<-plan_history ORDER BY date_time DESC LIMIT 1)[0]->assigned_instructor->instructor[0][0]<-is_instructor<-users[0][0].id as instructor_id\n" +
                    "FROM cadet) WHERE instructor_id = \$instructor_id;",
                    mapOf(
                        "instructor_id" to RecordID(tableName, recordId)
                    )
                ) as List<Map<*, *>>
            )[0]["result"] as List<Map<*, *>>

            Log.d("allWithPhotos", "${rawObj}")

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