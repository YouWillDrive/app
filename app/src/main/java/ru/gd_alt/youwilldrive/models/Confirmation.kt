package ru.gd_alt.youwilldrive.models

import kotlinx.serialization.Serializable

@Serializable
class Confirmation(val id: Int, var event: Event, var user: User, var value: Int, var date: Int, var isAutomatic: Boolean?, var confirmationType: ConfirmationType)