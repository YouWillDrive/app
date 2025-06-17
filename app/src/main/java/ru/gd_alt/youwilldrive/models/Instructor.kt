package ru.gd_alt.youwilldrive.models

import android.util.Log
import ru.gd_alt.youwilldrive.data.client.Connection
import ru.gd_alt.youwilldrive.data.models.RecordID

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
        val actualPlanPoints = (
            Connection.cl.query(
                "(\n" +
                "    SELECT ->of_cadet->cadet.id AS cadetId\n" +
                "    FROM plan_history\n" +
                "    WHERE date_time IN (\n" +
                "        SELECT ->of_cadet->cadet.id AS cId, time::max(date_time) as dt\n" +
                "        FROM plan_history\n" +
                "        GROUP BY cId\n" +
                "    ).map(|\$i| \$i.dt)\n" +
                ").map(|\$j| \$j[\"cadetId\"][0])"
            ) as List<Map<String, List<RecordID>>>
        )[0]["result"] as List<RecordID>

        val cadets = mutableListOf<Cadet>()

        for (planPoint in actualPlanPoints) {
            Log.d("cadets", planPoint.recordId)
            val cadet = Cadet.fromId(
                planPoint.toString()
            )
            Log.d("cadets", "$cadet")
            if (cadet != null && cadet.actualPlanPoint()?.assignedInstructor()?.id == id) {
                cadets.add(cadet)
            }
        }

        return cadets
    }
}