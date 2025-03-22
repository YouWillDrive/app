package ru.gd_alt.youwilldrive.models

class PlanHistoryPoint(val id: Int, var plan: Plan, val cadet: Cadet, var bonusHours: Int, var date: Int) {
}