package ru.gd_alt.youwilldrive.models

import kotlinx.serialization.Serializable

@Serializable
class Event(val id: Int, var type: EventType, var cadet: Cadet, var instructor: Instructor, var date: Int)