package ru.gd_alt.youwilldrive.models

import java.time.Instant

class Notification(val id: String, var body: Map<String, String>, var dateSent: Instant, var received: Boolean, var read: Boolean)