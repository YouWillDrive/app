package ru.gd_alt.youwilldrive.models

import kotlinx.datetime.LocalDateTime
import java.time.Instant

class PlanHistoryPoint(override val id: String, var bonusHours: Int, var date: LocalDateTime): Identifiable {
    companion object: ModelCompanion<PlanHistoryPoint> {
        override val tableName: String = "plan_history_point"

        override fun fromDictionary(dictionary: Map<*, *>): PlanHistoryPoint {
            return PlanHistoryPoint(
                dictionary["id"]!!.toString(),
                dictionary["bonus_hours"]!!.toString().toInt(),
                dictionary["date"]!! as LocalDateTime
            )
        }
    }

    suspend fun ofCadet(): Cadet? {
        return fetchRelatedSingle<Cadet>("of_cadet", Cadet::fromId)
    }

    suspend fun assignedInstructor(): Instructor? {
        return fetchRelatedSingle<Instructor>("assigned_instructor", Instructor::fromId)
    }

    suspend fun relatedPlan(): Plan? {
        return fetchRelatedSingle<Plan>("related_plan", Plan::fromId)
    }

    suspend fun relatedTransmission(): Transmission? {
        return fetchRelatedSingle<Transmission>("related_transmission", Transmission::fromId)
    }
}