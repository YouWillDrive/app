package ru.gd_alt.youwilldrive.models

import kotlinx.serialization.Serializable

@Serializable
class Notification(val id: Int, var user: User, var body: Map<String, String>, var dateSent: Int, var received: Boolean, var read: Boolean)