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
        val instructorUserId = (me()?.id ?: "a:a").split(':')
        val instructorUserRecord = RecordID(instructorUserId.first(), instructorUserId.last())
        val actualPlanPoints = (
            Connection.cl.query(
                "(\n" +
                "    SELECT * FROM (\n" +
                "        SELECT id\n" +
                "        FROM cadet\n" +
                "    )\n" +
                "    WHERE (\n" +
                "        (\n" +
                "            SELECT *\n" +
                "            FROM <-of_cadet<-plan_history\n" +
                "            ORDER BY date_time DESC\n" +
                "            LIMIT 1\n" +
                "        )[0]->assigned_instructor->instructor[0][0]<-is_instructor<-users[0][0].id\n" +
                "    ) = \$instructor_id\n" +
                ").map(|\$v| \$v.id)",
                mapOf(
                    "instructor_id" to instructorUserRecord
                )
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