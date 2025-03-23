package ru.gd_alt.youwilldrive.models

import java.time.LocalDateTime
import ru.gd_alt.youwilldrive.UtilsProvider

object Placeholders {
    val CurrentMonth = LocalDateTime.now().monthValue
    val CurrentYear = LocalDateTime.now().year
    val CurrentDay = LocalDateTime.now().dayOfMonth
    val DefaultDateTime1: LocalDateTime = LocalDateTime.of(CurrentYear, CurrentMonth, CurrentDay, 16, 0)
    val DefaultDateTime2: LocalDateTime = LocalDateTime.of(CurrentYear, CurrentMonth, CurrentDay, 17, 0)
    val DefaultDateTime3: LocalDateTime = LocalDateTime.of(CurrentYear, CurrentMonth, CurrentDay + 1, 18, 0)
    val DefaultDateTime4: LocalDateTime = LocalDateTime.of(CurrentYear, CurrentMonth + 1, CurrentDay + 1, 19, 0)
    val DefaultPlan = Plan(0, "Plan", 20, 20, 0.0f)
    val CadetRole = Role(1, "Cadet")
    val InstructorRole = Role(2, "Instructor")
    val AdminRole = Role(3, "Admin")
    val DefaultUser1 = User(0, CadetRole, "User", "1234567890", "a", "a", "a", "a", "a")
    val DefaultUser2 = User(1, InstructorRole, "User", "1234567890", "a", "a", "a", "a", "a")
    val DefaultUser3 = User(2, AdminRole, "User", "1234567890", "a", "a", "a", "a", "a")
    val DefaultTransmission = Transmission(0, "Manual")
    val DefaultCar = Car(0, "green", "Toyota", DefaultTransmission)
    val DefaultCadet = Cadet(0, DefaultUser1, DefaultPlan, 0)
    val DefaultInstructor = Instructor(0, DefaultUser2, DefaultCar)
    val DefaultPlanHistoryPoint = PlanHistoryPoint(0, DefaultPlan, DefaultCadet, 0, 0, DefaultInstructor)
    val DefaultEventType1 = EventType(0, "Lesson")
    val DefaultEventType2 = EventType(1, "Practice")
    val DefaultEventType3 = EventType(2, "Exam")
    val DefaultEventType4 = EventType(3, "Test")
    val DefaultEvent = Event(0, DefaultEventType1, DefaultCadet, DefaultInstructor, UtilsProvider.dateToTimestamp(DefaultDateTime1))
    val DefaultEvent1 = Event(1, DefaultEventType2, DefaultCadet, DefaultInstructor, UtilsProvider.dateToTimestamp(DefaultDateTime2))
    val DefaultEvent2 = Event(2, DefaultEventType3, DefaultCadet, DefaultInstructor, UtilsProvider.dateToTimestamp(DefaultDateTime3))
    val DefaultEvent3 = Event(3, DefaultEventType4, DefaultCadet, DefaultInstructor, UtilsProvider.dateToTimestamp(DefaultDateTime4))
    val DefaultEventList = listOf<Event>(DefaultEvent, DefaultEvent1, DefaultEvent2, DefaultEvent3)
}