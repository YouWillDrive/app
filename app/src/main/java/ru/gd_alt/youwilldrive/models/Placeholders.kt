package ru.gd_alt.youwilldrive.models

import kotlinx.datetime.toKotlinLocalDateTime
import ru.gd_alt.youwilldrive.UtilsProvider
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.TimeZone

object Placeholders {
    val CurrentMonth = LocalDateTime.now().monthValue
    val CurrentYear = LocalDateTime.now().year
    val CurrentDay = LocalDateTime.now().dayOfMonth
    val DefaultDateTime1: LocalDateTime = LocalDateTime.of(CurrentYear, CurrentMonth, CurrentDay, 16, 0)
    val DefaultDateTime2: LocalDateTime = LocalDateTime.of(CurrentYear, CurrentMonth, CurrentDay, 17, 0)
    val DefaultDateTime3: LocalDateTime = LocalDateTime.of(CurrentYear, CurrentMonth, 29, 18, 0)
    val DefaultDateTime4: LocalDateTime = LocalDateTime.of(CurrentYear, CurrentMonth + 1, 27, 19, 0)
    val DefaultPlan = Plan("0", "Загркузка плана...", 20, 20, 0.0f)
    val CadetRole = Role("1", "Cadet")
    val InstructorRole = Role("2", "Instructor")
    val AdminRole = Role("3", "Admin")
    val DefaultUser1 = User("0", "User", "1234567890", "email@example.com", "a", "Загрузка имени...", "Загркузка фамилии...", "Загрузка отчества...")
    val DefaultUser2 = User("1", "User", "1234567890", "embil@example.com", "b", "Nbme", "Surnbme", "Pbtron")
    val DefaultUser3 = User("2", "User", "1234567890", "emcil@example.com", "c", "Ncme", "Surncme", "Pctron")
    val DefaultTransmission = Transmission("0", "Загрузка КПП...")
    val DefaultCar1 = Car("0", "Загрузка марки...", "Загрузка...", "Загрузка цвета...")
    val DefaultCar2 = Car("1", "Ford", "c321ba", "yellow")
    val DefaultCar3 = Car("2", "Lada", "m755ga", "blue")
    val DefaultCar4 = Car("2", "Niva", "u132pa", "red")
    val DefaultCadet = Cadet("0", 25)
    val DefaultUser = User("0", "", "+1234567890", "email@example.com", "a", "Загрузка имени...", "Загркузка фамилии...", "Загрузка отчества...")
    val DefaultInstructor = Instructor("0")
    val DefaultPlanHistoryPoint = PlanHistoryPoint("0", 0, DefaultDateTime1.toKotlinLocalDateTime())
    val DefaultEventType1 = EventType("1", "Lesson")
    val DefaultEventType2 = EventType("2", "Practice")
    val DefaultEventType3 = EventType("3", "Exam")
    val DefaultEventType4 = EventType("4", "Test")
    val DefaultEvent = Event("0", DefaultDateTime1.toKotlinLocalDateTime())
    val DefaultEvent1 = Event("1", DefaultDateTime2.toKotlinLocalDateTime())
    val DefaultEvent2 = Event("2", DefaultDateTime4.toKotlinLocalDateTime())
    val DefaultEvent3 = Event("3", DefaultDateTime3.toKotlinLocalDateTime())
    val DefaultEventList = listOf(DefaultEvent, DefaultEvent1, DefaultEvent2, DefaultEvent3)
}