package ru.gd_alt.youwilldrive.models

@Serializable
class Car(val id: Int, var plateNumber: String, var color: String, var transmission: Transmission)