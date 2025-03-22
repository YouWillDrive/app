package ru.gd_alt.youwilldrive.models

import kotlinx.serialization.Serializable

@Serializable
class Instructor(val id: Int, var user: User, var car: Car)