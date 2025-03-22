package ru.gd_alt.youwilldrive.models

import kotlinx.serialization.Serializable

@Serializable
class Cadet(val id: Int, var user: User, var plan: Plan, var practiceHours: Int)