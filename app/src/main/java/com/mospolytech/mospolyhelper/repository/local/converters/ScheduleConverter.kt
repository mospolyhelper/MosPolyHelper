package com.mospolytech.mospolyhelper.repository.local.converters

import com.beust.klaxon.*
import com.mospolytech.mospolyhelper.repository.models.schedule.Group
import com.mospolytech.mospolyhelper.repository.models.schedule.Lesson
import com.mospolytech.mospolyhelper.repository.models.schedule.Schedule
import java.lang.StringBuilder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ScheduleConverter {
    companion object {
        val SCHEDULE_GROUP = Schedule::group.name
        val SCHEDULE_DATE_FROM = Schedule::dateFrom.name
        val SCHEDULE_DATE_TO = Schedule::dateTo.name
        val DAILY_SCHEDULES = Schedule::dailySchedules.name
        val LESSON_ORDER = Lesson::order.name
        val LESSON_TITLE = Lesson::title.name
        val LESSON_TEACHERS = Lesson::teachers.name
        val LESSON_DATE_FROM = Lesson::dateFrom.name
        val LESSON_DATE_TO = Lesson::dateTo.name
        val LESSON_AUDITORIUMS = Lesson::auditoriums.name
        val LESSON_TYPE = Lesson::type.name
    }

    val localDateConverter = object : Converter {
        override fun canConvert(cls: Class<*>) =
            cls == LocalDate::class.java

        override fun fromJson(jv: JsonValue): Any? {
            return LocalDate.parse(jv.string!!, dateFormatter)
        }

        override fun toJson(value: Any) =
            (value as LocalDate).format(dateFormatter)

    }

    val localDateTimeConverter = object : Converter {
        override fun canConvert(cls: Class<*>) =
            cls == LocalDateTime::class.java

        override fun fromJson(jv: JsonValue): Any? {
            return LocalDateTime.parse(jv.string!!, dateTimeFormatter)
        }

        override fun toJson(value: Any) =
            (value as LocalDateTime).format(dateTimeFormatter)

    }

    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun serializeSchedule(schedule: Schedule): String {
        val converter = Klaxon().converter(localDateConverter)
        return converter.toJsonString(schedule)
    }

    fun deserializeSchedule(scheduleString: String, isSession: Boolean, lastUpdate: LocalDateTime): Schedule {
        val converter = Klaxon().converter(localDateConverter)
        val parser = Parser.default()
        val json = parser.parse(StringBuilder(scheduleString)) as JsonObject
        val group = converter.parseFromJsonObject<Group>(json.obj(SCHEDULE_GROUP)!!)!!
        val dateFrom = converter.parseFromJsonObject<LocalDate>(json.obj(SCHEDULE_DATE_FROM)!!)!!
        val dateTo = converter.parseFromJsonObject<LocalDate>(json.obj(SCHEDULE_DATE_TO)!!)!!

        val dailySchedules = json
            .array<JsonArray<JsonObject>>(DAILY_SCHEDULES)!!
            .map { dailySchedule ->
                dailySchedule.map { lesson ->
                    Lesson(
                        lesson.int(LESSON_ORDER)!!,
                        lesson.string(LESSON_TITLE)!!,
                        lesson.array(LESSON_TEACHERS)!!,
                        converter.parseFromJsonObject(lesson.obj(LESSON_DATE_FROM)!!)!!,
                        converter.parseFromJsonObject(lesson.obj(LESSON_DATE_TO)!!)!!,
                        lesson.array(LESSON_AUDITORIUMS)!!,
                        lesson.string(LESSON_TYPE)!!,
                        group
                    )
                }
            }
        return  Schedule(
            dailySchedules,
            lastUpdate,
            group,
            isSession,
            dateFrom,
            dateTo
        )
    }

    fun serializeGroupList(groupList: List<String>) =
        Klaxon().toJsonString(groupList)

    fun deserializeGroupList(groupListString: String) =
        Klaxon().parseArray<String>(groupListString)!!
}