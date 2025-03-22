package ru.gd_alt.youwilldrive.models

import kotlinx.serialization.Serializable

@Serializable
class Plan(val id: Int, var name: String, var theoryHours: Int, var practiceHours: Int, var price: Float)